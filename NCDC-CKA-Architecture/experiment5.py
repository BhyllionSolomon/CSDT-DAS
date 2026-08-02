"""
Experiment 5 -- Does branch specialization matter more at longer horizons?
=============================================================================

Motivation, following the null results of Experiments 1, 2, and 4:
  - Exp 1: learned gate does not beat uniform fusion at horizon=1 (p=0.34).
  - Exp 2: gate does not differentiate between diseases (Failure Mode A).
  - Exp 4: forcing the gate to be maximally decisive (entropy pushed from
    ~0.97 down to ~0.25 nats) STILL does not produce an accuracy gain.

One structural explanation not yet tested: at horizon=1 (next week),
next week's case count is so strongly autocorrelated with the immediate
past that TCN (local), BiLSTM (sequential), and Transformer (global)
branches may all converge to near-identical, trivially-good predictions
regardless of how they're weighted -- making the entire gating question
moot BY CONSTRUCTION at this horizon, independent of anything about the
architecture or training procedure.

Longer horizons (4, 8 weeks ahead) remove the easy local-persistence
signal that all three branches can exploit equally well, and are where
genuine differences in what each branch is architecturally built to
capture -- short-term shocks (TCN) vs. sustained trend (BiLSTM) vs.
seasonal/long-range dependency (Transformer) -- would plausibly start
to diverge and matter for accuracy, IF branch specialization is a real
phenomenon at all for this data.

This is a DIFFERENT, falsifiable question from Experiments 1/2/4: not
"can we make the existing gate work better" but "does the premise that
branches specialize even apply outside horizon=1".

METHOD:
  For horizon in {1, 4, 8}:
    - Rebuild folds via ncdcData.build_dctmn_folds(..., horizon=horizon).
      (seq_len stays 12; only the forecast target's distance ahead changes,
      identical mechanism to how Experiments 1-4 used horizon=1.)
    - Train learned_gate (Dctmn.build_dctmn) and uniform_gate
      (experiment1.build_dctmn_uniform_gate) across the same folds x
      3 seeds used throughout this project.
    - Compute per-disease MAE for both variants; paired Wilcoxon test.
    - ALSO re-run Experiment 2's cross-disease gate-weight diagnostic at
      each horizon, to check whether disease differentiation itself
      (not just accuracy) becomes more pronounced at longer horizons.

  If the learned-gate-vs-uniform gap widens (and/or becomes significant)
  as horizon increases, that supports the "specialization matters more
  long-range" hypothesis. If the gap stays flat/null across all three
  horizons, that argues branch specialization is not a rescuable premise
  for this dataset regardless of horizon -- a stronger, more final
  conclusion than anything Experiments 1-4 alone could support.

Outputs (written to OUT_DIR):
  - exp5_mae_by_horizon_long.csv          -- every (seed, fold, disease, horizon, variant) MAE row
  - exp5_mae_summary_by_horizon.csv       -- mean +/- std MAE per (horizon, variant)
  - exp5_wilcoxon_by_horizon.csv          -- paired test result per horizon
  - exp5_cross_disease_gate_std_by_horizon.csv -- does disease differentiation grow with horizon?
  - exp5_mae_delta_vs_horizon.png         -- does the learned-gate advantage grow with horizon?
  - exp5_cross_disease_std_vs_horizon.png -- does disease differentiation grow with horizon?

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment5_horizon_sensitivity.py
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
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from sklearn.metrics import mean_absolute_error
from scipy.stats import wilcoxon

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import Dctmn
import ncdcData
from experiment1 import build_dctmn_uniform_gate  # re-use exact frozen-gate baseline


# ----------------------------------------------------------------------
# 0. CONFIGURATION
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp5_horizon_sensitivity"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZONS = [1, 4, 8]
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15


def train_and_evaluate(model, fold, seed_run):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)

    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0), loss={"forecast": "mse"})

    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
        monitor="val_loss", factor=0.5, patience=7, verbose=0)

    model.fit(
        {"sequence_input": fold["X_train"], "disease_id_input": fold["disease_id_train"]},
        {"forecast": fold["y_train"]},
        validation_split=0.15,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        callbacks=[early_stop, reduce_lr],
        verbose=0,
    )

    preds = model.predict(
        {"sequence_input": fold["X_test"], "disease_id_input": fold["disease_id_test"]},
        verbose=0,
    )
    y_pred_scaled = preds["forecast"].reshape(-1)
    y_true_scaled = fold["y_test"]

    y_pred = ncdcData.inverse_scale_y(y_pred_scaled, fold["disease_id_test"],
                                       fold["scaler_mean"], fold["scaler_std"])
    y_true = ncdcData.inverse_scale_y(y_true_scaled, fold["disease_id_test"],
                                       fold["scaler_mean"], fold["scaler_std"])

    return y_true, y_pred, preds["gate_weights"]


def per_disease_mae(y_true, y_pred, disease_id_test, id_to_name):
    rows = []
    for d_id in np.unique(disease_id_test):
        mask = disease_id_test == d_id
        mae = mean_absolute_error(y_true[mask], y_pred[mask])
        rows.append({"disease": id_to_name[int(d_id)], "MAE": mae, "n": int(mask.sum())})
    return rows


def main():
    print("=" * 70)
    print("  EXPERIMENT 5: Does branch specialization matter more at")
    print("  longer forecast horizons?")
    print("=" * 70)

    all_mae_rows = []
    all_gate_rows = []

    for horizon in HORIZONS:
        print(f"\n{'#'*70}\n  HORIZON = {horizon} week(s) ahead\n{'#'*70}")

        folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
            DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=horizon,
            min_test_examples=MIN_TEST_EXAMPLES,
        )
        id_to_name = {v: k for k, v in disease_id_map.items()}
        num_diseases = len(disease_names)
        num_features = num_diseases

        for seed_run in SEEDS:
            print(f"\n  --- seed {seed_run} ---")

            for fold in folds:
                test_year = fold["test_year"]
                print(f"    fold {test_year}: ", end="", flush=True)

                # ---- learned gate ----
                tf.random.set_seed(seed_run); np.random.seed(seed_run)
                learned_model = Dctmn.build_dctmn(
                    seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
                    forecast_horizon=1,
                )
                y_true_l, y_pred_l, gate_w_l = train_and_evaluate(learned_model, fold, seed_run)
                learned_rows = per_disease_mae(y_true_l, y_pred_l, fold["disease_id_test"], id_to_name)
                for r in learned_rows:
                    all_mae_rows.append({**r, "variant": "learned_gate", "seed_run": seed_run,
                                          "fold_test_year": test_year, "horizon": horizon})

                # cross-disease gate weight spread at this horizon (Experiment 2 diagnostic, reused)
                per_disease_gw = Dctmn.per_disease_gate_summary(gate_w_l, fold["disease_id_test"], id_to_name)
                mean_weights_matrix = np.array([info["mean_weights"] for info in per_disease_gw.values()])
                cross_disease_std = mean_weights_matrix.std(axis=0)  # [tcn, bilstm, transformer]
                all_gate_rows.append({
                    "horizon": horizon, "seed_run": seed_run, "fold_test_year": test_year,
                    "cross_disease_std_tcn": cross_disease_std[0],
                    "cross_disease_std_bilstm": cross_disease_std[1],
                    "cross_disease_std_transformer": cross_disease_std[2],
                })

                # ---- uniform baseline ----
                tf.random.set_seed(seed_run); np.random.seed(seed_run)
                uniform_model = build_dctmn_uniform_gate(
                    seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
                    forecast_horizon=1,
                )
                y_true_u, y_pred_u, _ = train_and_evaluate(uniform_model, fold, seed_run)
                uniform_rows = per_disease_mae(y_true_u, y_pred_u, fold["disease_id_test"], id_to_name)
                for r in uniform_rows:
                    all_mae_rows.append({**r, "variant": "uniform_gate", "seed_run": seed_run,
                                          "fold_test_year": test_year, "horizon": horizon})

                learned_mae_agg = np.mean([r["MAE"] for r in learned_rows])
                uniform_mae_agg = np.mean([r["MAE"] for r in uniform_rows])
                print(f"learned={learned_mae_agg:.3f} uniform={uniform_mae_agg:.3f} "
                      f"delta={uniform_mae_agg - learned_mae_agg:+.3f}")

    # ------------------------------------------------------------------
    # Aggregate
    # ------------------------------------------------------------------
    mae_df = pd.DataFrame(all_mae_rows)
    mae_df.to_csv(os.path.join(OUT_DIR, "exp5_mae_by_horizon_long.csv"), index=False)

    gate_df = pd.DataFrame(all_gate_rows)
    gate_summary = gate_df.groupby("horizon")[
        ["cross_disease_std_tcn", "cross_disease_std_bilstm", "cross_disease_std_transformer"]
    ].mean()
    gate_summary.to_csv(os.path.join(OUT_DIR, "exp5_cross_disease_gate_std_by_horizon.csv"))

    print(f"\n{'='*70}\n  SUMMARY: MAE by horizon x variant\n{'='*70}")
    summary = mae_df.groupby(["horizon", "variant"])["MAE"].agg(["mean", "std", "count"])
    print(summary.to_string())
    summary.to_csv(os.path.join(OUT_DIR, "exp5_mae_summary_by_horizon.csv"))

    print(f"\n{'='*70}\n  Cross-disease gate weight std by horizon "
          f"(does disease differentiation itself grow with horizon?)\n{'='*70}")
    print(gate_summary.to_string())
    print("  (Experiment 2 found ~0.006-0.017 at horizon=1 -- Failure Mode A. "
          "Compare these values to that baseline.)")

    # ------------------------------------------------------------------
    # Paired Wilcoxon per horizon
    # ------------------------------------------------------------------
    print(f"\n{'='*70}\n  PAIRED WILCOXON per horizon (learned_gate vs uniform_gate)\n{'='*70}\n")
    wilcoxon_rows = []
    for horizon in HORIZONS:
        pivot = mae_df[mae_df["horizon"] == horizon].pivot_table(
            index=["seed_run", "fold_test_year", "disease"], columns="variant", values="MAE"
        ).dropna()
        if len(pivot) >= 6:
            stat, p = wilcoxon(pivot["uniform_gate"], pivot["learned_gate"], alternative="greater")
            median_delta = (pivot["uniform_gate"] - pivot["learned_gate"]).median()
            sig = "SIGNIFICANT -- learned gate wins" if (p < 0.05 and median_delta > 0) else "not significant"
            print(f"  horizon={horizon:<3} n={len(pivot):<4} stat={stat:>8.1f}  p={p:.4f}  "
                  f"median_delta={median_delta:+.4f}  -> {sig}")
            wilcoxon_rows.append({"horizon": horizon, "n_pairs": len(pivot), "wilcoxon_stat": stat,
                                   "p_value": p, "median_delta_uniform_minus_learned": median_delta,
                                   "verdict": sig})
    wilcoxon_df = pd.DataFrame(wilcoxon_rows)
    wilcoxon_df.to_csv(os.path.join(OUT_DIR, "exp5_wilcoxon_by_horizon.csv"), index=False)

    trend = ""
    if len(wilcoxon_df) == len(HORIZONS):
        deltas = wilcoxon_df["median_delta_uniform_minus_learned"].values
        if np.all(np.diff(deltas) > 0):
            trend = "Learned-gate advantage INCREASES monotonically with horizon -- supports the hypothesis."
        elif np.all(np.diff(deltas) < 0):
            trend = "Learned-gate advantage DECREASES with horizon -- contradicts the hypothesis."
        else:
            trend = "No consistent monotonic trend across horizons -- inconclusive/flat."
    print(f"\n  TREND ACROSS HORIZONS: {trend}")

    # ------------------------------------------------------------------
    # Plot 1: MAE delta vs horizon
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.axhline(0, color="black", lw=0.8)
    ax.plot(wilcoxon_df["horizon"], wilcoxon_df["median_delta_uniform_minus_learned"],
            marker="o", color="#2a78d6", markersize=8)
    for _, row in wilcoxon_df.iterrows():
        marker_color = "#2a78d6" if "SIGNIFICANT" in row["verdict"] else "#a0a0a0"
        ax.scatter([row["horizon"]], [row["median_delta_uniform_minus_learned"]], color=marker_color, s=120, zorder=5)
    ax.set_xlabel("Forecast horizon (weeks ahead)")
    ax.set_ylabel("Median (uniform_gate MAE - learned_gate MAE)\n(positive = learned gate wins)")
    ax.set_title("Does the Learned-Gate Advantage Grow With Forecast Horizon?")
    ax.set_xticks(HORIZONS)
    ax.grid(alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp5_mae_delta_vs_horizon.png"), dpi=150, bbox_inches="tight")
    plt.close()

    # ------------------------------------------------------------------
    # Plot 2: cross-disease gate std vs horizon
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(8, 5))
    for col, label, color in [
        ("cross_disease_std_tcn", "TCN", "#e34948"),
        ("cross_disease_std_bilstm", "BiLSTM", "#2a78d6"),
        ("cross_disease_std_transformer", "Transformer", "#4a8fc9"),
    ]:
        ax.plot(gate_summary.index, gate_summary[col], marker="o", label=label, color=color)
    ax.set_xlabel("Forecast horizon (weeks ahead)")
    ax.set_ylabel("Cross-disease std of mean gate weight\n(higher = gate differentiates diseases more)")
    ax.set_title("Does Disease Differentiation in the Gate Grow With Horizon?")
    ax.set_xticks(HORIZONS)
    ax.legend()
    ax.grid(alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp5_cross_disease_std_vs_horizon.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()