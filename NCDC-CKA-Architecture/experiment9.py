"""
Experiment 9 -- Cross-Disease Attention Network (CDAN)
============================================================================

Correction to the framing used in Experiments 1-8: those experiments
treated each disease mostly on its own. The gate looked at one
disease's identity and one window of numbers, and decided how to
blend three generic time-pattern branches. It never explicitly asked
"what does what is happening in OTHER diseases tell us about this
one".

This is a real gap, not a minor one. Prior work in this project
(the Nigeria Cholera Intelligence Platform) already found 72
statistically significant Granger-causality relationships between
diseases in this same surveillance system. That means cross-disease
relationships are known to exist in this data. Nothing in DCTMN's
architecture was built to use them.

CDAN is built specifically to test whether using these relationships
directly improves both forecasting accuracy and the ability to catch
real outbreaks early (the actual, practical goal established earlier
in this project).

ARCHITECTURE:
  1. Each disease's own 12-week window is passed through a SHARED
     BiLSTM (the same weights used for every disease), producing one
     summary vector per disease. This keeps the proven, simple
     temporal encoder from Experiment 6 rather than reintroducing
     unnecessary complexity.
  2. All seven disease summary vectors are then passed together
     through a multi-head self-attention layer, treating each
     disease's vector as one token in a sequence of seven. This lets
     the model learn, directly from data, which diseases' patterns
     are informative for forecasting which other diseases, the
     learned analogue of the Granger-causality relationships already
     found in prior work, rather than a fixed, hand-picked graph.
  3. Each disease's attention-updated vector is passed through its
     own small output head to produce that disease's next-value
     forecast. All seven diseases are forecast simultaneously from
     one shared window, rather than one training example per disease.

EVALUATION:
  Same expanding-window folds, three seeds, and the same 90th-
  percentile outbreak-timing test (EARLY / ON_TIME / LATE / MISSED,
  plus false alarm rate) used in Experiments 7 and 8, so results are
  directly comparable to the single-disease BiLSTM baseline.

Outputs (written to OUT_DIR):
  - exp9_mae_by_disease.csv
  - exp9_warning_summary.csv
  - exp9_false_alarm_rate.csv
  - exp9_attention_weights_mean.csv   -- which diseases attend to which, on average
  - exp9_cross_disease_attention.png -- heatmap of learned disease-to-disease attention

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment9_cross_disease_attention.py
"""

import os
import sys
import warnings

os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_DETERMINISTIC_OPS"] = "1"
os.environ["TF_CUDNN_DETERMINISTIC"] = "1"
warnings.filterwarnings("ignore")

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras import layers, models
from sklearn.metrics import mean_absolute_error
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import ncdcData  # reuses load_and_pivot only; windowing here is multi-output, unlike DCTMN's per-disease format


DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp9_cross_disease_attention"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TRAIN_WEEKS_FOR_FOLD = 40
SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

OUTBREAK_PERCENTILE = 90
MIN_LEAD_WEEKS = 1
BILSTM_UNITS = 64
ATTN_HEADS = 2
ATTN_KEY_DIM = 32


# ----------------------------------------------------------------------
# 1. Multi-output windowing: one window in, ALL diseases' next value out
# ----------------------------------------------------------------------

def build_multi_output_examples(wide_df, disease_names, seq_len, horizon):
    feature_matrix = wide_df[disease_names].values.astype("float32")  # (n_weeks, n_diseases)
    years = wide_df["year"].values.astype("int32")
    n_weeks = feature_matrix.shape[0]

    X_list, y_list, target_year_list = [], [], []
    last_valid_t = n_weeks - horizon - 1
    for t in range(seq_len - 1, last_valid_t + 1):
        window = feature_matrix[t - seq_len + 1: t + 1]        # (seq_len, n_diseases)
        target = feature_matrix[t + horizon]                    # (n_diseases,)
        X_list.append(window)
        y_list.append(target)
        target_year_list.append(years[t + horizon])

    X_all = np.stack(X_list).astype("float32")
    y_all = np.stack(y_list).astype("float32")
    target_year_all = np.array(target_year_list, dtype="int32")
    return X_all, y_all, target_year_all


def fit_scaler(X_train, y_train):
    flat = X_train.reshape(-1, X_train.shape[-1])
    mean = flat.mean(axis=0)
    std = flat.std(axis=0) + 1e-6
    return mean, std


def build_folds(csv_path, test_years, seq_len, horizon):
    wide_df, disease_names = ncdcData.load_and_pivot(csv_path)
    X_all, y_all, target_year_all = build_multi_output_examples(wide_df, disease_names, seq_len, horizon)

    folds = []
    for test_year in test_years:
        train_mask = target_year_all < test_year
        test_mask = target_year_all == test_year
        if test_mask.sum() < 5 or train_mask.sum() < MIN_TRAIN_WEEKS_FOR_FOLD:
            print(f"  [SKIP] test_year={test_year}: insufficient data.")
            continue

        X_train_fold, y_train_fold = X_all[train_mask], y_all[train_mask]
        X_test_fold, y_test_fold = X_all[test_mask], y_all[test_mask]

        mean, std = fit_scaler(X_train_fold, y_train_fold)
        folds.append({
            "test_year": test_year,
            "X_train": (X_train_fold - mean) / std,
            "y_train": (y_train_fold - mean) / std,
            "X_test": (X_test_fold - mean) / std,
            "y_test": (y_test_fold - mean) / std,
            "y_test_raw": y_test_fold,
            "y_train_raw": y_train_fold,
            "scaler_mean": mean, "scaler_std": std,
        })
        print(f"  Fold test_year={test_year}: train={train_mask.sum()}, test={test_mask.sum()}")

    return folds, disease_names


# ----------------------------------------------------------------------
# 2. CDAN architecture
# ----------------------------------------------------------------------

def build_cdan(seq_len, num_diseases):
    seq_input = layers.Input(shape=(seq_len, num_diseases), name="window_input")

    # Step 1: shared per-disease temporal encoder. Each disease's own
    # column is treated as an independent 1-feature time series and
    # passed through the SAME BiLSTM weights, producing one summary
    # vector per disease.
    per_disease_series = layers.Permute((2, 1), name="to_disease_major")(seq_input)   # (batch, n_diseases, seq_len)
    per_disease_series = layers.Reshape((num_diseases, seq_len, 1), name="add_feature_dim")(per_disease_series)

    shared_bilstm = layers.Bidirectional(layers.LSTM(BILSTM_UNITS, return_sequences=False), name="shared_bilstm")
    disease_summaries = layers.TimeDistributed(shared_bilstm, name="per_disease_encode")(per_disease_series)
    # disease_summaries: (batch, n_diseases, 2*BILSTM_UNITS) -- one token per disease

    # Step 2: cross-disease self-attention. Each disease's token attends
    # to every other disease's token, learning data-driven relationships
    # analogous to the Granger-causality links found in prior work.
    attn_out, attn_scores = layers.MultiHeadAttention(
        num_heads=ATTN_HEADS, key_dim=ATTN_KEY_DIM, name="cross_disease_attention"
    )(disease_summaries, disease_summaries, return_attention_scores=True)
    fused = layers.LayerNormalization(name="post_attn_norm")(disease_summaries + attn_out)

    # Step 3: per-disease output head, applied identically to every
    # disease's fused token, producing one forecast per disease.
    head = layers.Dense(32, activation="relu", name="head_dense")
    out_head = layers.Dense(1, name="head_out")
    per_disease_hidden = layers.TimeDistributed(head, name="td_head_dense")(fused)
    per_disease_hidden = layers.Dropout(0.2, name="head_dropout")(per_disease_hidden)
    per_disease_forecast = layers.TimeDistributed(out_head, name="td_head_out")(per_disease_hidden)
    forecast_output = layers.Reshape((num_diseases,), name="forecast_output")(per_disease_forecast)

    model = models.Model(inputs=seq_input, outputs={"forecast": forecast_output, "attention": attn_scores}, name="CDAN")
    return model


def train_and_evaluate(model, fold, seed_run):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0),
                  loss={"forecast": "mse", "attention": None})
    early_stop = tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=7, verbose=0)
    model.fit(fold["X_train"], {"forecast": fold["y_train"]}, validation_split=0.15,
              epochs=EPOCHS, batch_size=BATCH_SIZE, callbacks=[early_stop, reduce_lr], verbose=0)
    preds = model.predict(fold["X_test"], verbose=0)
    y_pred_scaled = preds["forecast"]
    y_pred = y_pred_scaled * fold["scaler_std"] + fold["scaler_mean"]
    y_true = fold["y_test_raw"]
    return y_true, y_pred, preds["attention"]


def score_warning_timing(y_true_col, y_pred_col, thresh):
    real_flag = y_true_col > thresh
    pred_flag = y_pred_col > thresh
    quiet_mask = ~real_flag
    false_alarms = int(pred_flag[quiet_mask].sum()) if quiet_mask.sum() > 0 else 0
    n_quiet = int(quiet_mask.sum())

    onset_idx = None
    for i in range(len(real_flag)):
        if real_flag[i] and (i == 0 or not real_flag[i-1]):
            onset_idx = i
            break
    if onset_idx is None:
        return None, false_alarms, n_quiet

    pred_onset_idx = None
    for i in range(len(pred_flag)):
        if pred_flag[i] and (i == 0 or not pred_flag[i-1]):
            if pred_onset_idx is None or i <= onset_idx:
                pred_onset_idx = i
                if i <= onset_idx:
                    break
    if pred_onset_idx is None:
        verdict = "MISSED"
    else:
        lead = onset_idx - pred_onset_idx
        verdict = "EARLY" if lead >= MIN_LEAD_WEEKS else ("ON_TIME" if lead == 0 else "LATE")
    return verdict, false_alarms, n_quiet


def main():
    print("=" * 70)
    print("  EXPERIMENT 9: Cross-Disease Attention Network (CDAN)")
    print("=" * 70)

    folds, disease_names = build_folds(DATA_PATH, TEST_YEARS, SEQ_LEN, HORIZON)
    num_diseases = len(disease_names)

    mae_rows, event_rows, fa_rows = [], [], []
    attn_accum = np.zeros((num_diseases, num_diseases))
    attn_count = 0

    for seed_run in SEEDS:
        print(f"\n{'#'*70}\n  SEED: {seed_run}\n{'#'*70}")
        for fold in folds:
            test_year = fold["test_year"]
            print(f"  fold {test_year} ... ", end="", flush=True)

            model = build_cdan(SEQ_LEN, num_diseases)
            y_true, y_pred, attn = train_and_evaluate(model, fold, seed_run)

            mean_attn = attn.mean(axis=(0, 1))  # average over batch and heads -> (n_diseases, n_diseases)
            attn_accum += mean_attn
            attn_count += 1

            train_vals_by_disease = fold["y_train_raw"]
            for j, d_name in enumerate(disease_names):
                mae = mean_absolute_error(y_true[:, j], y_pred[:, j])
                mae_rows.append({"disease": d_name, "seed_run": seed_run, "fold_test_year": test_year, "MAE": mae})

                thresh = np.percentile(train_vals_by_disease[:, j], OUTBREAK_PERCENTILE)
                verdict, false_alarms, n_quiet = score_warning_timing(y_true[:, j], y_pred[:, j], thresh)
                if verdict is not None:
                    event_rows.append({"disease": d_name, "seed_run": seed_run, "fold_test_year": test_year, "verdict": verdict})
                fa_rows.append({"disease": d_name, "false_alarms": false_alarms, "n_quiet": n_quiet})

            print("done")

    mae_df = pd.DataFrame(mae_rows)
    mae_summary = mae_df.groupby("disease")["MAE"].agg(["mean", "std"])
    mae_summary.to_csv(os.path.join(OUT_DIR, "exp9_mae_by_disease.csv"))

    event_df = pd.DataFrame(event_rows)
    warning_summary = event_df.groupby(["disease", "verdict"]).size().unstack(fill_value=0)
    for col in ["EARLY", "ON_TIME", "LATE", "MISSED"]:
        if col not in warning_summary.columns:
            warning_summary[col] = 0
    warning_summary = warning_summary[["EARLY", "ON_TIME", "LATE", "MISSED"]]
    warning_summary.to_csv(os.path.join(OUT_DIR, "exp9_warning_summary.csv"))

    fa_df = pd.DataFrame(fa_rows)
    fa_by_disease = fa_df.groupby("disease").apply(lambda g: g["false_alarms"].sum() / g["n_quiet"].sum())
    fa_by_disease.to_csv(os.path.join(OUT_DIR, "exp9_false_alarm_rate.csv"))

    mean_attn_matrix = attn_accum / attn_count
    attn_matrix_df = pd.DataFrame(mean_attn_matrix, index=disease_names, columns=disease_names)
    attn_matrix_df.to_csv(os.path.join(OUT_DIR, "exp9_attention_weights_mean.csv"))

    print(f"\n{'='*70}\n  SUMMARY\n{'='*70}\n")
    print("MAE per disease (CDAN, cross-disease attention):")
    print(mae_summary.to_string())
    overall_mae = mae_df["MAE"].mean()
    print(f"\nOverall mean MAE across all diseases: {overall_mae:.3f}")
    print("\nOutbreak timing outcomes:")
    print(warning_summary.to_string())
    total_events = warning_summary.sum(axis=1).sum()
    caught = (warning_summary["EARLY"] + warning_summary["ON_TIME"]).sum()
    print(f"\nOutbreak catch rate: {caught} / {total_events} ({100*caught/total_events:.1f}%)")
    print("\nFalse alarm rate per disease:")
    print(fa_by_disease.to_string())

    # ------------------------------------------------------------------
    # Plot: cross-disease attention heatmap
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 7))
    im = ax.imshow(mean_attn_matrix, cmap="Blues", vmin=0, vmax=mean_attn_matrix.max())
    ax.set_xticks(range(num_diseases)); ax.set_xticklabels(disease_names, rotation=45, ha="right")
    ax.set_yticks(range(num_diseases)); ax.set_yticklabels(disease_names)
    ax.set_xlabel("Attended-to disease (source of information)")
    ax.set_ylabel("Disease being forecast (target)")
    ax.set_title("Learned Cross-Disease Attention (mean across folds, seeds, heads)")
    for i in range(num_diseases):
        for j in range(num_diseases):
            ax.text(j, i, f"{mean_attn_matrix[i,j]:.2f}", ha="center", va="center",
                     color="white" if mean_attn_matrix[i,j] > mean_attn_matrix.max()/2 else "black", fontsize=8)
    fig.colorbar(im, ax=ax, label="Mean attention weight")
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp9_cross_disease_attention.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()