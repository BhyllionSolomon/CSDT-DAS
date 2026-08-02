"""
Experiment 7 -- Does the forecast actually warn early, or does it only
catch up after cases have already risen?
============================================================================

Everything tested so far (Experiments 1-6) measured point-forecast
accuracy (mean absolute error). None of it tested whether the model's
predictions would actually be USEFUL for a real decision, such as
sending vaccines or oral rehydration supplies to an area before a rise
in cases, rather than after.

This experiment tests that directly, using the simplest sufficient
architecture identified in Experiment 6 (a single BiLSTM branch, which
matched full three-branch accuracy). The question here is not "how
accurate is the number" but "does the number cross into outbreak
territory before the real data does, in time to act".

METHOD:
  For each disease, in each fold's test period:
    1. Label each REAL week as "outbreak" or "quiet" using the same
       per-disease training-set median split already used in
       Experiment 3 (no test-set leakage).
    2. Label each PREDICTED week the same way, using the model's
       forecast value against that same threshold.
    3. Find the first real outbreak onset in the test period.
    4. Check whether the model's prediction ALSO crossed the threshold
       at that same week, earlier, or later:
         - EARLY: predicted crossing arrives at least MIN_LEAD_WEEKS
           before the real crossing (useful -- there is lead time to act)
         - ON_TIME: predicted crossing happens the same week (some use,
           no lead time)
         - LATE: predicted crossing happens after the real crossing
         - MISSED: no predicted crossing found near the real onset
    5. Also compute the FALSE ALARM RATE: how often the model predicts
       "outbreak" during a week that was actually quiet.

  Assumption made explicit: an early warning only counts as useful if
  it arrives at least one full week ahead of the real crossing
  (MIN_LEAD_WEEKS = 1). A same-week or late signal is not treated as
  actionable warning, even if numerically close.

Outputs (written to OUT_DIR):
  - exp7_warning_timing_long.csv       -- every outbreak onset event, with timing verdict
  - exp7_warning_summary.csv           -- count of early/on-time/late/missed per disease
  - exp7_false_alarm_rate.csv          -- false alarm rate per disease
  - exp7_warning_timing_by_disease.png -- stacked bar chart of warning outcomes per disease

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment7_early_warning.py
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
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp7_early_warning"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1
SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

MIN_LEAD_WEEKS = 1  # an early warning must arrive at least this many weeks ahead to count as useful
OUTBREAK_PERCENTILE = 90  # a week only counts as "outbreak" if it is in the top 10% of that
                          # disease's case counts -- a rare, unusually high week, not merely
                          # above average. This replaces an earlier, incorrect median-based
                          # threshold that flagged roughly half of all weeks as "outbreak."


# ----------------------------------------------------------------------
# 1. Simplest sufficient model, per Experiment 6: BiLSTM branch alone
# ----------------------------------------------------------------------

def build_bilstm_only_model(seq_len, num_features, common_dim=64, forecast_horizon=1):
    seq_input = layers.Input(shape=(seq_len, num_features), name="sequence_input")
    repr_ = Dctmn.build_bilstm_branch(seq_input, name="bilstm")
    proj = layers.Dense(common_dim, name="bilstm_proj")(repr_)
    x = layers.Dense(64, activation="relu", name="head_dense1")(proj)
    x = layers.Dropout(0.2, name="head_dropout")(x)
    forecast_output = layers.Dense(forecast_horizon, name="forecast_output")(x)
    return models.Model(inputs=seq_input, outputs=forecast_output, name="BiLSTM_only")


def train_and_evaluate(model, fold, seed_run):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0), loss="mse")
    early_stop = tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=7, verbose=0)
    model.fit(fold["X_train"], fold["y_train"], validation_split=0.15, epochs=EPOCHS,
              batch_size=BATCH_SIZE, callbacks=[early_stop, reduce_lr], verbose=0)
    y_pred_scaled = model.predict(fold["X_test"], verbose=0).reshape(-1)
    y_true_scaled = fold["y_test"]
    y_pred = ncdcData.inverse_scale_y(y_pred_scaled, fold["disease_id_test"], fold["scaler_mean"], fold["scaler_std"])
    y_true = ncdcData.inverse_scale_y(y_true_scaled, fold["disease_id_test"], fold["scaler_mean"], fold["scaler_std"])
    return y_true, y_pred


def main():
    print("=" * 70)
    print("  EXPERIMENT 7: Does the forecast warn early, or only catch up?")
    print("=" * 70)

    folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
        DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=HORIZON,
        min_test_examples=MIN_TEST_EXAMPLES,
    )
    id_to_name = {v: k for k, v in disease_id_map.items()}
    num_diseases = len(disease_names)
    num_features = num_diseases

    event_rows = []
    false_alarm_rows = []

    for seed_run in SEEDS:
        print(f"\n{'#'*70}\n  SEED: {seed_run}\n{'#'*70}")

        for fold in folds:
            test_year = fold["test_year"]
            print(f"  Fold {test_year} ... ", end="", flush=True)

            model = build_bilstm_only_model(SEQ_LEN, num_features, forecast_horizon=1)
            y_true, y_pred = train_and_evaluate(model, fold, seed_run)
            disease_ids = fold["disease_id_test"]

            # threshold per disease: median of TRAINING targets for that disease (no test leakage)
            thresholds = {}
            for d_id in np.unique(fold["disease_id_train"]):
                mask = fold["disease_id_train"] == d_id
                train_vals = ncdcData.inverse_scale_y(fold["y_train"][mask], np.full(mask.sum(), d_id),
                                                       fold["scaler_mean"], fold["scaler_std"])
                # FIX: an outbreak must be a RARE, unusually high week, not merely
                # above the middle (median) of all weeks -- the median flags roughly
                # half of all weeks as "outbreak" by construction, which is not what
                # health workers mean by the term and inflates false alarms artificially.
                # The 90th percentile of training case counts is used instead: only
                # the top 10% of weeks for that disease count as an outbreak level.
                thresholds[d_id] = np.percentile(train_vals, OUTBREAK_PERCENTILE)

            for d_id in np.unique(disease_ids):
                d_name = id_to_name[int(d_id)]
                mask = disease_ids == d_id
                real_seq = y_true[mask]
                pred_seq = y_pred[mask]
                thresh = thresholds.get(d_id, np.percentile(real_seq, OUTBREAK_PERCENTILE))

                real_flag = real_seq > thresh
                pred_flag = pred_seq > thresh

                # false alarm rate: predicted outbreak while real was quiet
                quiet_mask = ~real_flag
                false_alarms = int((pred_flag[quiet_mask]).sum()) if quiet_mask.sum() > 0 else 0
                false_alarm_rows.append({
                    "disease": d_name, "seed_run": seed_run, "fold_test_year": test_year,
                    "false_alarms": false_alarms, "n_quiet_weeks": int(quiet_mask.sum()),
                    "false_alarm_rate": false_alarms / quiet_mask.sum() if quiet_mask.sum() > 0 else np.nan,
                })

                # find first real outbreak onset: first week real_flag is True after a quiet week (or index 0)
                onset_idx = None
                for i in range(len(real_flag)):
                    if real_flag[i] and (i == 0 or not real_flag[i-1]):
                        onset_idx = i
                        break

                if onset_idx is None:
                    continue  # no outbreak onset in this fold's test period for this disease

                # find earliest predicted onset near the real onset
                pred_onset_idx = None
                for i in range(len(pred_flag)):
                    if pred_flag[i] and (i == 0 or not pred_flag[i-1]):
                        if pred_onset_idx is None or i <= onset_idx:
                            pred_onset_idx = i
                            if i <= onset_idx:
                                break

                if pred_onset_idx is None:
                    verdict = "MISSED"
                    lead = None
                else:
                    lead = onset_idx - pred_onset_idx  # positive = early, 0 = on time, negative = late
                    if lead >= MIN_LEAD_WEEKS:
                        verdict = "EARLY"
                    elif lead == 0:
                        verdict = "ON_TIME"
                    else:
                        verdict = "LATE"

                event_rows.append({
                    "disease": d_name, "seed_run": seed_run, "fold_test_year": test_year,
                    "real_onset_week_idx": onset_idx, "pred_onset_week_idx": pred_onset_idx,
                    "lead_weeks": lead, "verdict": verdict,
                })
            print("done")

    # ------------------------------------------------------------------
    # Aggregate
    # ------------------------------------------------------------------
    event_df = pd.DataFrame(event_rows)
    event_df.to_csv(os.path.join(OUT_DIR, "exp7_warning_timing_long.csv"), index=False)

    fa_df = pd.DataFrame(false_alarm_rows)
    fa_summary = fa_df.groupby("disease")["false_alarm_rate"].mean().sort_values()
    fa_summary.to_csv(os.path.join(OUT_DIR, "exp7_false_alarm_rate.csv"))

    print(f"\n{'='*70}\n  WARNING TIMING SUMMARY (per disease, across all seeds/folds)\n{'='*70}\n")
    summary = event_df.groupby(["disease", "verdict"]).size().unstack(fill_value=0)
    for col in ["EARLY", "ON_TIME", "LATE", "MISSED"]:
        if col not in summary.columns:
            summary[col] = 0
    summary = summary[["EARLY", "ON_TIME", "LATE", "MISSED"]]
    print(summary.to_string())
    summary.to_csv(os.path.join(OUT_DIR, "exp7_warning_summary.csv"))

    total_events = summary.sum().sum()
    total_early = summary["EARLY"].sum()
    print(f"\n  Overall: {total_early} / {total_events} outbreak onsets ({100*total_early/total_events:.1f}%) "
          f"were flagged with at least {MIN_LEAD_WEEKS} week(s) of real lead time.")

    print(f"\n  False alarm rate per disease (predicted outbreak during an actually-quiet week):")
    print(fa_summary.to_string())

    if total_early / total_events < 0.5:
        verdict_line = ("VERDICT: The model's forecast does NOT reliably provide early warning. "
                         "Most outbreak onsets were caught on time or late, meaning the prediction "
                         "would not have given health workers meaningful lead time to act ahead of "
                         "vaccine, water treatment, or supply decisions.")
    else:
        verdict_line = ("VERDICT: The model's forecast DOES provide early warning for the majority "
                         "of outbreak onsets tested, suggesting genuine potential for supporting "
                         "advance resource allocation decisions, though false alarm rates should be "
                         "weighed alongside this before any operational use.")
    print(f"\n  {verdict_line}")

    # ------------------------------------------------------------------
    # Plot
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(10, 6))
    summary.plot(kind="bar", stacked=True, ax=ax,
                 color={"EARLY": "#2a78d6", "ON_TIME": "#f0ad4e", "LATE": "#e34948", "MISSED": "#888888"})
    ax.set_ylabel("Number of outbreak onset events")
    ax.set_title("Experiment 7: Did the Forecast Warn Early, On Time, Late, or Miss the Outbreak?")
    ax.legend(title="Timing")
    plt.xticks(rotation=30, ha="right")
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp7_warning_timing_by_disease.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()