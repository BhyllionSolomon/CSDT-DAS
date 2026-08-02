"""
Experiment 3 -- Does the gate respond to outbreak vs. quiet context?
=======================================================================

Follows from Experiment 2's finding (Failure Mode A: gate weights do
NOT differ meaningfully by disease identity -- cross-disease std was
0.006-0.017, near-max entropy ~0.96-1.00 nats). However, Experiment 2
also found non-trivial WITHIN-disease variance (std 0.11-0.19 per
branch) -- the gate IS reacting to something sample-to-sample, just not
to disease identity. This script tests the most likely remaining
candidate: is that reactivity tracking outbreak vs. quiet conditions?

This would be a more precise, more honestly reportable finding than a
flat "the gate does nothing" -- i.e. "the gate is context-sensitive in
a disease-AGNOSTIC way" is a specific, falsifiable claim in its own
right, and worth checking before writing off DCTMN's gating mechanism
entirely.

METHOD:
  For each (seed, fold, disease) test window, classify it as "outbreak"
  or "quiet" using the SAME per-disease case-count percentile split
  already used elsewhere in this project (see compute_thresholds in the
  hetero script: 33rd/66th percentile of nonzero training values).
  Here we use a simpler binary split (median of TRAINING values for
  that disease, computed within that fold -- no test-set leakage) since
  we only need two buckets, not three severity classes.

  Then compare gate weight DISTRIBUTIONS between the two buckets:
    - overall (pooled across all diseases): does context matter at all?
    - per-disease: does any specific disease show FOLD-CONSISTENT
      outbreak-vs-quiet gate shifts, even if the AVERAGE across all
      diseases doesn't move much?

  This re-trains DCTMN (same as Experiment 2) since Experiment 2 did not
  save the actual case-count values needed to classify quiet/outbreak --
  only the gate weights and disease ids were kept. Re-training is
  necessary to attach quiet/outbreak labels to each already-computed
  gate-weight row using the corresponding fold's real y_test values.

Outputs (written to OUT_DIR):
  - exp3_gate_weights_with_context_long.csv  -- gate weights + quiet/outbreak label per sample
  - exp3_outbreak_vs_quiet_summary.csv        -- mean weights, pooled and per-disease, by context
  - exp3_mannwhitney_tests.csv                -- per-branch, per-disease Mann-Whitney U test
  - exp3_context_sensitivity_overview.png     -- grouped bars: quiet vs outbreak, per branch, pooled
  - exp3_per_disease_context_shift.png        -- per-disease delta (outbreak weight - quiet weight), per branch

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment3_context_sensitivity.py
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
from scipy.stats import mannwhitneyu

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import Dctmn
import ncdcData


# ----------------------------------------------------------------------
# 0. CONFIGURATION -- must match Experiments 1 & 2 exactly
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp3_context_sensitivity"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

BRANCH_NAMES = ["w_tcn", "w_bilstm", "w_transformer"]


def train_learned_gate(fold, seed_run, num_features, num_diseases):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)

    model = Dctmn.build_dctmn(
        seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
        forecast_horizon=1,
    )
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
    return preds["gate_weights"]


def classify_quiet_outbreak(fold):
    """
    Per-disease median split, computed on TRAIN data only (no test-set
    leakage), applied to label each TEST sample's target value as
    'quiet' (<= train median for that disease) or 'outbreak' (> median).
    Uses the already-scaled y (fine for a binary split -- scaling is
    monotonic per disease, so the median split is unaffected).
    """
    train_disease_id = fold["disease_id_train"]
    train_y = fold["y_train"]
    test_disease_id = fold["disease_id_test"]
    test_y = fold["y_test"]

    medians = {}
    for d_id in np.unique(train_disease_id):
        medians[d_id] = np.median(train_y[train_disease_id == d_id])

    labels = np.array([
        "outbreak" if test_y[i] > medians.get(test_disease_id[i], np.median(train_y)) else "quiet"
        for i in range(len(test_y))
    ])
    return labels


def main():
    print("=" * 70)
    print("  EXPERIMENT 3: Context sensitivity -- quiet vs. outbreak")
    print("=" * 70)

    folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
        DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=HORIZON,
        min_test_examples=MIN_TEST_EXAMPLES,
    )
    id_to_name = {v: k for k, v in disease_id_map.items()}
    num_diseases = len(disease_names)
    num_features = num_diseases

    all_rows = []

    for seed_run in SEEDS:
        print(f"\n{'#'*70}\n  SEED: {seed_run}\n{'#'*70}")

        for fold in folds:
            test_year = fold["test_year"]
            print(f"  Fold test_year={test_year} ... ", end="", flush=True)

            gate_weights = train_learned_gate(fold, seed_run, num_features, num_diseases)
            disease_ids = fold["disease_id_test"]
            context_labels = classify_quiet_outbreak(fold)

            for i in range(len(disease_ids)):
                d_name = id_to_name[int(disease_ids[i])]
                all_rows.append({
                    "seed_run": seed_run,
                    "fold_test_year": test_year,
                    "disease": d_name,
                    "disease_id": int(disease_ids[i]),
                    "context": context_labels[i],
                    "w_tcn": float(gate_weights[i, 0]),
                    "w_bilstm": float(gate_weights[i, 1]),
                    "w_transformer": float(gate_weights[i, 2]),
                })
            print("done")

    gw_df = pd.DataFrame(all_rows)
    gw_df.to_csv(os.path.join(OUT_DIR, "exp3_gate_weights_with_context_long.csv"), index=False)

    # ------------------------------------------------------------------
    # Pooled: does context matter AT ALL, averaged across every disease?
    # ------------------------------------------------------------------
    print(f"\n{'='*70}")
    print("  POOLED (ALL DISEASES): mean gate weight, quiet vs. outbreak")
    print(f"{'='*70}\n")

    pooled_summary = gw_df.groupby("context")[BRANCH_NAMES].mean()
    print(pooled_summary.to_string())
    pooled_summary.to_csv(os.path.join(OUT_DIR, "exp3_pooled_context_summary.csv"))

    pooled_tests = []
    for branch in BRANCH_NAMES:
        quiet_vals = gw_df[gw_df["context"] == "quiet"][branch].values
        outbreak_vals = gw_df[gw_df["context"] == "outbreak"][branch].values
        if len(quiet_vals) >= 5 and len(outbreak_vals) >= 5:
            stat, p = mannwhitneyu(outbreak_vals, quiet_vals, alternative="two-sided")
            pooled_tests.append({"branch": branch, "u_stat": stat, "p_value": p,
                                  "mean_quiet": quiet_vals.mean(), "mean_outbreak": outbreak_vals.mean(),
                                  "delta_outbreak_minus_quiet": outbreak_vals.mean() - quiet_vals.mean()})
    pooled_tests_df = pd.DataFrame(pooled_tests)
    print(f"\n  Pooled Mann-Whitney U tests (outbreak vs. quiet), per branch:\n")
    print(pooled_tests_df.to_string(index=False))

    # ------------------------------------------------------------------
    # Per-disease: does ANY individual disease show a real shift, even if
    # the pooled average doesn't move much?
    # ------------------------------------------------------------------
    print(f"\n{'='*70}")
    print("  PER-DISEASE: mean gate weight, quiet vs. outbreak")
    print(f"{'='*70}\n")

    per_disease_rows = []
    mw_rows = []
    for d_name, d_group in gw_df.groupby("disease"):
        quiet = d_group[d_group["context"] == "quiet"]
        outbreak = d_group[d_group["context"] == "outbreak"]
        row = {"disease": d_name, "n_quiet": len(quiet), "n_outbreak": len(outbreak)}
        for branch in BRANCH_NAMES:
            row[f"{branch}_quiet"] = quiet[branch].mean() if len(quiet) else np.nan
            row[f"{branch}_outbreak"] = outbreak[branch].mean() if len(outbreak) else np.nan
            row[f"{branch}_delta"] = row[f"{branch}_outbreak"] - row[f"{branch}_quiet"]

            if len(quiet) >= 5 and len(outbreak) >= 5:
                stat, p = mannwhitneyu(outbreak[branch].values, quiet[branch].values, alternative="two-sided")
                mw_rows.append({"disease": d_name, "branch": branch, "u_stat": stat, "p_value": p,
                                 "delta_outbreak_minus_quiet": row[f"{branch}_delta"]})
        per_disease_rows.append(row)
        print(f"  {d_name:<14} " + "  ".join(
            f"{b.split('_')[1].upper()}: quiet={row[f'{b}_quiet']:.3f} outbreak={row[f'{b}_outbreak']:.3f} "
            f"(delta={row[f'{b}_delta']:+.3f})" for b in BRANCH_NAMES
        ))

    per_disease_df = pd.DataFrame(per_disease_rows)
    per_disease_df.to_csv(os.path.join(OUT_DIR, "exp3_per_disease_context_summary.csv"), index=False)

    mw_df = pd.DataFrame(mw_rows)
    mw_df.to_csv(os.path.join(OUT_DIR, "exp3_mannwhitney_tests.csv"), index=False)

    n_significant = (mw_df["p_value"] < 0.05).sum() if len(mw_df) else 0
    print(f"\n  Per-disease x per-branch Mann-Whitney tests: {n_significant} / {len(mw_df)} "
          f"reached p < 0.05 (uncorrected).")

    if len(pooled_tests_df) and (pooled_tests_df["p_value"] < 0.05).any():
        verdict = ("Context DOES shift gate weights in a disease-agnostic way -- "
                   "consistent with Experiment 2's within-disease variance finding, "
                   "now explained: the gate reacts to outbreak/quiet conditions "
                   "generically, not to WHICH disease is being forecast.")
    else:
        verdict = ("Pooled test does not show a significant outbreak-vs-quiet shift either. "
                   "Combined with Experiment 2, this suggests the gate's within-disease "
                   "variance may be closer to noise than genuine context-sensitivity -- "
                   "worth checking gate_weight entropy/variance against a random-input "
                   "control before concluding context-sensitivity is real.")
    print(f"\n  VERDICT: {verdict}")

    # ------------------------------------------------------------------
    # Plot 1: pooled quiet vs outbreak, grouped by branch
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(7, 5))
    x = np.arange(len(BRANCH_NAMES))
    width = 0.35
    quiet_means = [pooled_summary.loc["quiet", b] for b in BRANCH_NAMES]
    outbreak_means = [pooled_summary.loc["outbreak", b] for b in BRANCH_NAMES]
    ax.bar(x - width/2, quiet_means, width, label="Quiet", color="#a0a0a0")
    ax.bar(x + width/2, outbreak_means, width, label="Outbreak", color="#e34948")
    ax.axhline(1/3, color="gray", ls="--", lw=1, label="Uniform (1/3)")
    ax.set_xticks(x)
    ax.set_xticklabels(["TCN", "BiLSTM", "Transformer"])
    ax.set_ylabel("Mean gate weight (pooled across all diseases)")
    ax.set_title("Experiment 3: Does the Gate React to Outbreak vs. Quiet Conditions?")
    ax.legend()
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp3_context_sensitivity_overview.png"), dpi=150, bbox_inches="tight")
    plt.close()

    # ------------------------------------------------------------------
    # Plot 2: per-disease delta (outbreak - quiet), one subplot per branch
    # ------------------------------------------------------------------
    fig, axes = plt.subplots(1, 3, figsize=(16, 5), sharey=True)
    for ax, branch in zip(axes, BRANCH_NAMES):
        col = f"{branch}_delta"
        sorted_df = per_disease_df.sort_values(col)
        colors = ["#2a78d6" if v > 0 else "#e34948" for v in sorted_df[col]]
        ax.barh(sorted_df["disease"], sorted_df[col], color=colors)
        ax.axvline(0, color="black", lw=0.8)
        ax.set_title(branch.replace("w_", "").upper())
        ax.grid(axis="x", alpha=0.3)
    axes[0].set_xlabel("Outbreak weight - Quiet weight")
    fig.suptitle("Per-Disease Context Shift in Gate Weights (by branch)")
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp3_per_disease_context_shift.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()