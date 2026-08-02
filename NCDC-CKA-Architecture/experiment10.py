"""
Experiment 10 -- Cross-Disease Attention Network with outbreak-weighted
training: the direct test of the actual project goal.
============================================================================

Actual goal of this project, stated plainly: forecast Nigeria's
notifiable diseases early enough that health workers could act on the
warning -- sending vaccines to an area before measles cases climb,
preparing oral rehydration supplies before a cholera outbreak grows,
or moving resources ahead of a rise in Lassa fever cases, rather than
responding only after the outbreak is already visible in the data.

Experiment 9 (cross-disease attention alone) fixed general accuracy
and false alarms, but caught ZERO real outbreaks early or on time --
every single one was caught late or missed.

Experiment 8 (outbreak-weighted training alone, on the older
single-disease model) caught SOME outbreaks early, but only by
sacrificing overall accuracy and reintroducing a high false alarm rate.

This experiment combines both fixes in one model: the cross-disease
attention architecture from Experiment 9, trained with the
outbreak-weighted loss from Experiment 8, so that:
  - the model still sees all seven diseases together (Experiment 9's
    proven accuracy and false-alarm improvement), AND
  - the model is still explicitly penalized more for missing a rare,
    real outbreak value than for being slightly off on an ordinary
    week (Experiment 8's proven ability to force earlier detection).

If this combination still cannot catch outbreaks early, that is a
much stronger and more final answer than either experiment alone
could give: it would mean the weekly case-count data itself does not
carry an advance signal usable for real early warning, not that the
architecture failed to look for one.

IMPLEMENTATION NOTE:
  Standard Keras sample_weight, when passed as a single value per
  training example, cannot weight different diseases differently
  WITHIN the same window, because the built-in "mse" loss already
  averages across the seven diseases before sample_weight is applied.
  To weight specific diseases within a window correctly, the true
  target and the per-disease weight are passed in as additional model
  inputs, and the weighted loss is added directly via model.add_loss(),
  computed BEFORE any averaging across diseases collapses the weight
  information. A separate, lightweight inference-only model, sharing
  the exact same trained layers, is used for generating test
  predictions without needing target/weight inputs at test time.

Outputs (written to OUT_DIR):
  - exp10_mae_by_weight.csv
  - exp10_warning_summary_by_weight.csv
  - exp10_false_alarm_by_weight.csv
  - exp10_tradeoff.png

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment10_combined_fix.py
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

import ncdcData

# Re-use the exact multi-output data pipeline from Experiment 9
from experiment9 import build_folds, BILSTM_UNITS, ATTN_HEADS, ATTN_KEY_DIM

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp10_combined_fix"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

OUTBREAK_PERCENTILE = 90
MIN_LEAD_WEEKS = 1
OUTBREAK_WEIGHTS = [1, 5, 10, 20]   # 1 = cross-disease attention alone (reproduces Experiment 9)


# ----------------------------------------------------------------------
# Combined architecture: CDAN core + outbreak-weighted training via
# model.add_loss (see implementation note above)
# ----------------------------------------------------------------------

def build_combined_models(seq_len, num_diseases):
    seq_input = layers.Input(shape=(seq_len, num_diseases), name="window_input")

    per_disease_series = layers.Permute((2, 1), name="to_disease_major")(seq_input)
    per_disease_series = layers.Reshape((num_diseases, seq_len, 1), name="add_feature_dim")(per_disease_series)

    shared_bilstm = layers.Bidirectional(layers.LSTM(BILSTM_UNITS, return_sequences=False), name="shared_bilstm")
    disease_summaries = layers.TimeDistributed(shared_bilstm, name="per_disease_encode")(per_disease_series)

    attn_out, attn_scores = layers.MultiHeadAttention(
        num_heads=ATTN_HEADS, key_dim=ATTN_KEY_DIM, name="cross_disease_attention"
    )(disease_summaries, disease_summaries, return_attention_scores=True)
    fused = layers.LayerNormalization(name="post_attn_norm")(disease_summaries + attn_out)

    head = layers.Dense(32, activation="relu", name="head_dense")
    out_head = layers.Dense(1, name="head_out")
    per_disease_hidden = layers.TimeDistributed(head, name="td_head_dense")(fused)
    per_disease_hidden = layers.Dropout(0.2, name="head_dropout")(per_disease_hidden)
    per_disease_forecast = layers.TimeDistributed(out_head, name="td_head_out")(per_disease_hidden)
    forecast_output = layers.Reshape((num_diseases,), name="forecast_output")(per_disease_forecast)

    # Inference-only model: window in, forecast + attention out. No
    # target or weight inputs needed, used for test-time prediction.
    inference_model = models.Model(
        inputs=seq_input, outputs={"forecast": forecast_output, "attention": attn_scores}, name="CDAN_inference"
    )

    # Training model: adds target and per-disease weight as explicit
    # inputs, and computes the weighted MSE loss internally via
    # add_loss, BEFORE any averaging collapses the per-disease weights.
    # Keras 3 requires any raw tensor math (tf.square, tf.reduce_mean)
    # to be wrapped inside an actual Layer subclass rather than called
    # directly on symbolic Input tensors -- this is what WeightedMSELoss
    # below does.
    y_true_input = layers.Input(shape=(num_diseases,), name="y_true_input")
    weight_input = layers.Input(shape=(num_diseases,), name="weight_input")

    class WeightedMSELoss(layers.Layer):
        def call(self, inputs):
            y_true, y_pred, weight = inputs
            squared_error = tf.square(y_true - y_pred)          # (batch, n_diseases)
            weighted_squared_error = weight * squared_error       # per-disease weighting preserved
            loss = tf.reduce_mean(weighted_squared_error)
            self.add_loss(loss)
            return y_pred  # pass-through, only used to wire the graph together

    loss_output = WeightedMSELoss(name="weighted_mse_loss")([y_true_input, forecast_output, weight_input])

    training_model = models.Model(
        inputs=[seq_input, y_true_input, weight_input], outputs=loss_output, name="CDAN_training"
    )

    return training_model, inference_model


def train_and_evaluate(fold, seed_run, outbreak_weight, num_diseases):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)

    training_model, inference_model = build_combined_models(SEQ_LEN, num_diseases)
    training_model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0), loss=None)

    # per-disease, per-training-sample weight: outbreak_weight if that
    # disease's value in that training window was itself an outbreak
    # value (top 10% for that disease), else 1
    thresholds = np.percentile(fold["y_train_raw"], OUTBREAK_PERCENTILE, axis=0)  # (n_diseases,)
    weight_train = np.where(fold["y_train_raw"] > thresholds, float(outbreak_weight), 1.0).astype("float32")

    n = fold["X_train"].shape[0]
    split = int(n * 0.85)
    idx = np.arange(n)  # no shuffle here -- keep simple, deterministic split for validation

    early_stop = tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=7, verbose=0)

    training_model.fit(
        [fold["X_train"][:split], fold["y_train"][:split], weight_train[:split]],
        validation_data=([fold["X_train"][split:], fold["y_train"][split:], weight_train[split:]], None),
        epochs=EPOCHS, batch_size=BATCH_SIZE, callbacks=[early_stop, reduce_lr], verbose=0,
    )

    preds = inference_model.predict(fold["X_test"], verbose=0)
    y_pred = preds["forecast"] * fold["scaler_std"] + fold["scaler_mean"]
    y_true = fold["y_test_raw"]
    return y_true, y_pred, thresholds


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
    print("  EXPERIMENT 10: Cross-disease attention + outbreak-weighted training")
    print("  Direct test of: does this warn early enough to act on?")
    print("=" * 70)

    folds, disease_names = build_folds(DATA_PATH, TEST_YEARS, SEQ_LEN, HORIZON)
    num_diseases = len(disease_names)

    mae_rows, event_rows, fa_rows = [], [], []

    for weight in OUTBREAK_WEIGHTS:
        print(f"\n{'#'*70}\n  OUTBREAK_WEIGHT = {weight}\n{'#'*70}")

        for seed_run in SEEDS:
            for fold in folds:
                test_year = fold["test_year"]
                print(f"  seed {seed_run}, fold {test_year} ... ", end="", flush=True)

                y_true, y_pred, thresholds_scaled_space = train_and_evaluate(fold, seed_run, weight, num_diseases)
                # thresholds computed on raw y_train_raw already, reuse directly per disease below
                thresholds_raw = np.percentile(fold["y_train_raw"], OUTBREAK_PERCENTILE, axis=0)

                for j, d_name in enumerate(disease_names):
                    mae = mean_absolute_error(y_true[:, j], y_pred[:, j])
                    mae_rows.append({"weight": weight, "disease": d_name, "seed_run": seed_run,
                                      "fold_test_year": test_year, "MAE": mae})

                    verdict, false_alarms, n_quiet = score_warning_timing(y_true[:, j], y_pred[:, j], thresholds_raw[j])
                    if verdict is not None:
                        event_rows.append({"weight": weight, "disease": d_name, "seed_run": seed_run,
                                            "fold_test_year": test_year, "verdict": verdict})
                    fa_rows.append({"weight": weight, "disease": d_name, "false_alarms": false_alarms, "n_quiet": n_quiet})

                print("done")

    mae_df = pd.DataFrame(mae_rows)
    mae_summary = mae_df.groupby("weight")["MAE"].agg(["mean", "std"])
    mae_summary.to_csv(os.path.join(OUT_DIR, "exp10_mae_by_weight.csv"))

    event_df = pd.DataFrame(event_rows)
    warning_summary = event_df.groupby(["weight", "verdict"]).size().unstack(fill_value=0)
    for col in ["EARLY", "ON_TIME", "LATE", "MISSED"]:
        if col not in warning_summary.columns:
            warning_summary[col] = 0
    warning_summary = warning_summary[["EARLY", "ON_TIME", "LATE", "MISSED"]]
    warning_summary.to_csv(os.path.join(OUT_DIR, "exp10_warning_summary_by_weight.csv"))

    fa_df = pd.DataFrame(fa_rows)
    fa_by_weight = fa_df.groupby("weight").apply(lambda g: g["false_alarms"].sum() / g["n_quiet"].sum())
    fa_by_weight.to_csv(os.path.join(OUT_DIR, "exp10_false_alarm_by_weight.csv"))

    print(f"\n{'='*70}\n  SUMMARY\n{'='*70}\n")
    print("Overall MAE by outbreak weight:")
    print(mae_summary.to_string())
    print("\nOutbreak timing outcomes by weight:")
    print(warning_summary.to_string())
    print("\nFalse alarm rate by weight:")
    print(fa_by_weight.to_string())

    total_events = warning_summary.sum(axis=1)
    caught = warning_summary["EARLY"] + warning_summary["ON_TIME"]
    catch_rate = (caught / total_events * 100).round(1)
    print("\nOutbreak catch rate (EARLY + ON_TIME, percent of real outbreaks not missed/late):")
    print(catch_rate.to_string())

    best_weight = catch_rate.idxmax()
    print(f"\n  Best setting for early warning: outbreak_weight={best_weight}, "
          f"catching {catch_rate[best_weight]}% of real outbreaks early or on time, "
          f"at MAE={mae_summary.loc[best_weight, 'mean']:.3f} and false alarm rate="
          f"{fa_by_weight[best_weight]:.3f}.")

    fig, ax1 = plt.subplots(figsize=(8, 5))
    ax2 = ax1.twinx()
    ax1.plot(mae_summary.index, mae_summary["mean"], marker="o", color="#e34948", label="Overall MAE")
    ax2.plot(catch_rate.index, catch_rate.values, marker="s", color="#2a78d6", label="Outbreak catch rate (%)")
    ax1.set_xlabel("Outbreak sample weight during training")
    ax1.set_ylabel("Overall MAE", color="#e34948")
    ax2.set_ylabel("Outbreak catch rate (%)", color="#2a78d6")
    ax1.set_title("Experiment 10: Combined Fix -- Accuracy vs. Early-Warning Tradeoff")
    fig.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp10_tradeoff.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()