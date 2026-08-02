"""
Experiment 8 -- Does training the model to care more about rare outbreak
weeks fix the near-total miss rate found in Experiment 7?
============================================================================

Experiment 7 (corrected, 90th-percentile outbreak threshold) found that
the model MISSED 88 out of 93 real outbreak weeks across seven diseases.
The likely cause: plain mean-squared-error training rewards a model for
staying close to the ordinary, typical case count every week, since
that is right most of the time. A rare, unusually high outbreak week
is treated as just one more data point to average over, so the model
has little incentive to ever predict a number that high.

This experiment tests a direct, standard fix: give training weeks that
are ABOVE the outbreak threshold much more weight in the loss function,
so getting those weeks wrong costs the model far more than getting an
ordinary week slightly wrong. This is the same principle already used
elsewhere in this project's own prior work (class-weighted training for
imbalanced severity categories).

METHOD:
  For each disease, in each fold, the OUTBREAK_PERCENTILE (90th) of
  TRAINING targets is used to mark which training weeks are "outbreak"
  weeks (identical definition to Experiment 7's fix). Each outbreak
  training week is given a sample weight of OUTBREAK_WEIGHT; every
  other training week keeps a weight of 1. This is standard weighted
  regression, not a change to the architecture; the same BiLSTM-only
  model from Experiment 6 is used throughout.

  OUTBREAK_WEIGHT is swept across {1, 5, 10, 20} (1 = no fix, reproduces
  Experiment 7's original behavior, included as a direct sanity check).

  For each weight, two things are measured on the SAME test data:
    1. Overall MAE (does fixing outbreak detection cost general accuracy?)
    2. The same EARLY / ON_TIME / LATE / MISSED outbreak-timing test and
       false alarm rate from Experiment 7, so the tradeoff is visible
       side by side rather than assumed.

Outputs (written to OUT_DIR):
  - exp8_mae_by_weight.csv               -- overall MAE per outbreak weight setting
  - exp8_warning_summary_by_weight.csv   -- EARLY/ON_TIME/LATE/MISSED counts per weight setting
  - exp8_false_alarm_by_weight.csv       -- false alarm rate per weight setting
  - exp8_tradeoff.png                    -- MAE vs. outbreak catch rate, across weight settings

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment8_outbreak_weighted_training.py
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

import Dctmn
import ncdcData


# ----------------------------------------------------------------------
# 0. CONFIGURATION
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp8_outbreak_weighted"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1
SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

OUTBREAK_PERCENTILE = 90   # identical definition to Experiment 7's fix
MIN_LEAD_WEEKS = 1
OUTBREAK_WEIGHTS = [1, 5, 10, 20]   # 1 = no fix, included as a direct sanity check against Experiment 7


def build_bilstm_only_model(seq_len, num_features, common_dim=64, forecast_horizon=1):
    seq_input = layers.Input(shape=(seq_len, num_features), name="sequence_input")
    repr_ = Dctmn.build_bilstm_branch(seq_input, name="bilstm")
    proj = layers.Dense(common_dim, name="bilstm_proj")(repr_)
    x = layers.Dense(64, activation="relu", name="head_dense1")(proj)
    x = layers.Dropout(0.2, name="head_dropout")(x)
    forecast_output = layers.Dense(forecast_horizon, name="forecast_output")(x)
    return models.Model(inputs=seq_input, outputs=forecast_output, name="BiLSTM_only")


def train_and_evaluate(model, fold, seed_run, sample_weight_train):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0), loss="mse")
    early_stop = tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=7, verbose=0)
    model.fit(fold["X_train"], fold["y_train"], sample_weight=sample_weight_train,
              validation_split=0.15, epochs=EPOCHS, batch_size=BATCH_SIZE,
              callbacks=[early_stop, reduce_lr], verbose=0)
    y_pred_scaled = model.predict(fold["X_test"], verbose=0).reshape(-1)
    y_true_scaled = fold["y_test"]
    y_pred = ncdcData.inverse_scale_y(y_pred_scaled, fold["disease_id_test"], fold["scaler_mean"], fold["scaler_std"])
    y_true = ncdcData.inverse_scale_y(y_true_scaled, fold["disease_id_test"], fold["scaler_mean"], fold["scaler_std"])
    return y_true, y_pred


def score_warning_timing(y_true, y_pred, disease_ids, thresholds, id_to_name):
    event_rows, fa_rows = [], []
    for d_id in np.unique(disease_ids):
        d_name = id_to_name[int(d_id)]
        mask = disease_ids == d_id
        real_seq, pred_seq = y_true[mask], y_pred[mask]
        thresh = thresholds.get(d_id, np.percentile(real_seq, OUTBREAK_PERCENTILE))

        real_flag = real_seq > thresh
        pred_flag = pred_seq > thresh

        quiet_mask = ~real_flag
        false_alarms = int((pred_flag[quiet_mask]).sum()) if quiet_mask.sum() > 0 else 0
        fa_rows.append({"disease": d_name, "false_alarms": false_alarms, "n_quiet": int(quiet_mask.sum())})

        onset_idx = None
        for i in range(len(real_flag)):
            if real_flag[i] and (i == 0 or not real_flag[i-1]):
                onset_idx = i
                break
        if onset_idx is None:
            continue

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
        event_rows.append({"disease": d_name, "verdict": verdict})
    return event_rows, fa_rows


def main():
    print("=" * 70)
    print("  EXPERIMENT 8: Does outbreak-weighted training fix the miss rate?")
    print("=" * 70)

    folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
        DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=HORIZON,
        min_test_examples=MIN_TEST_EXAMPLES,
    )
    id_to_name = {v: k for k, v in disease_id_map.items()}
    num_diseases = len(disease_names)
    num_features = num_diseases

    mae_rows = []
    all_event_rows = []
    all_fa_rows = []

    for weight in OUTBREAK_WEIGHTS:
        print(f"\n{'#'*70}\n  OUTBREAK_WEIGHT = {weight}\n{'#'*70}")

        for seed_run in SEEDS:
            for fold in folds:
                test_year = fold["test_year"]
                print(f"  seed {seed_run}, fold {test_year} ... ", end="", flush=True)

                # per-disease outbreak threshold, from TRAINING data only
                thresholds = {}
                for d_id in np.unique(fold["disease_id_train"]):
                    mask = fold["disease_id_train"] == d_id
                    train_vals = ncdcData.inverse_scale_y(fold["y_train"][mask], np.full(mask.sum(), d_id),
                                                           fold["scaler_mean"], fold["scaler_std"])
                    thresholds[d_id] = np.percentile(train_vals, OUTBREAK_PERCENTILE)

                # per-sample training weight: OUTBREAK_WEIGHT if that training week
                # was itself an outbreak week for its disease, else 1
                train_vals_full = ncdcData.inverse_scale_y(
                    fold["y_train"], fold["disease_id_train"], fold["scaler_mean"], fold["scaler_std"])
                sample_weight_train = np.ones(len(fold["y_train"]), dtype="float32")
                for i in range(len(sample_weight_train)):
                    d_id = fold["disease_id_train"][i]
                    if train_vals_full[i] > thresholds.get(d_id, np.inf):
                        sample_weight_train[i] = weight

                model = build_bilstm_only_model(SEQ_LEN, num_features, forecast_horizon=1)
                y_true, y_pred = train_and_evaluate(model, fold, seed_run, sample_weight_train)

                mae = mean_absolute_error(y_true, y_pred)
                mae_rows.append({"weight": weight, "seed_run": seed_run, "fold_test_year": test_year, "MAE": mae})

                event_rows, fa_rows = score_warning_timing(y_true, y_pred, fold["disease_id_test"], thresholds, id_to_name)
                for r in event_rows:
                    all_event_rows.append({**r, "weight": weight, "seed_run": seed_run, "fold_test_year": test_year})
                for r in fa_rows:
                    all_fa_rows.append({**r, "weight": weight, "seed_run": seed_run, "fold_test_year": test_year})

                print(f"MAE={mae:.3f}")

    # ------------------------------------------------------------------
    # Aggregate
    # ------------------------------------------------------------------
    mae_df = pd.DataFrame(mae_rows)
    mae_summary = mae_df.groupby("weight")["MAE"].agg(["mean", "std"])
    mae_summary.to_csv(os.path.join(OUT_DIR, "exp8_mae_by_weight.csv"))

    event_df = pd.DataFrame(all_event_rows)
    warning_summary = event_df.groupby(["weight", "verdict"]).size().unstack(fill_value=0)
    for col in ["EARLY", "ON_TIME", "LATE", "MISSED"]:
        if col not in warning_summary.columns:
            warning_summary[col] = 0
    warning_summary = warning_summary[["EARLY", "ON_TIME", "LATE", "MISSED"]]
    warning_summary.to_csv(os.path.join(OUT_DIR, "exp8_warning_summary_by_weight.csv"))

    fa_df = pd.DataFrame(all_fa_rows)
    fa_by_weight = fa_df.groupby("weight").apply(lambda g: g["false_alarms"].sum() / g["n_quiet"].sum())
    fa_by_weight.to_csv(os.path.join(OUT_DIR, "exp8_false_alarm_by_weight.csv"))

    print(f"\n{'='*70}\n  SUMMARY\n{'='*70}\n")
    print("Overall MAE by outbreak weight:")
    print(mae_summary.to_string())
    print("\nOutbreak timing outcomes by weight:")
    print(warning_summary.to_string())
    print("\nOverall false alarm rate by weight:")
    print(fa_by_weight.to_string())

    total_events = warning_summary.sum(axis=1)
    caught = warning_summary["EARLY"] + warning_summary["ON_TIME"]
    catch_rate = (caught / total_events * 100).round(1)
    print("\nOutbreak catch rate (EARLY + ON_TIME, percent of real outbreaks not missed):")
    print(catch_rate.to_string())

    # ------------------------------------------------------------------
    # Plot: tradeoff between MAE and catch rate across weight settings
    # ------------------------------------------------------------------
    fig, ax1 = plt.subplots(figsize=(8, 5))
    ax2 = ax1.twinx()
    ax1.plot(mae_summary.index, mae_summary["mean"], marker="o", color="#e34948", label="Overall MAE")
    ax2.plot(catch_rate.index, catch_rate.values, marker="s", color="#2a78d6", label="Outbreak catch rate (%)")
    ax1.set_xlabel("Outbreak sample weight during training")
    ax1.set_ylabel("Overall MAE", color="#e34948")
    ax2.set_ylabel("Outbreak catch rate (%)", color="#2a78d6")
    ax1.set_title("Experiment 8: Accuracy vs. Outbreak-Catching Tradeoff")
    fig.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp8_tradeoff.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()