"""
Experiment 2 -- Do different diseases learn different branch preferences?
============================================================================

Follows directly from Experiment 1's null result (learned gate did not
beat uniform fusion, p=0.383). Before concluding the hypothesis failed,
this script distinguishes between two very different failure modes:

  Failure mode A: "The gate learned nothing"
      Every disease gets ~[0.33, 0.33, 0.33]. The gate collapsed to
      uniform on its own, so of course it doesn't beat a uniform
      baseline -- it IS one, just with wasted extra parameters.

  Failure mode B: "The gate learned something, but not the right thing"
      Different diseases DO get reliably different weights (e.g.
      Cholera -> TCN-heavy, Measles -> Transformer-heavy), but that
      differentiation doesn't translate into lower forecast error --
      e.g. because the "preferred" branch isn't actually the best one
      for that disease, or because seq_len=12/horizon=1 doesn't give
      the long-term branch enough runway to matter.

These have very different implications for the paper. A tells you the
architecture/training setup needs rethinking (e.g. gate capacity, loss
shaping, or that a gate is simply unnecessary for this task). B tells
you the CAUSAL STORY (context -> gate -> branch preference -> fusion)
is real and inspectable, even if it isn't (yet) improving accuracy --
which is itself a publishable, honestly-reported finding.

This script re-trains ONLY the learned-gate DCTMN (the uniform baseline
already has its answer from Experiment 1) across the same folds x seeds,
and this time keeps every (seed, fold, disease, sample) gate-weight
triple, using Dctmn.py's own diagnostic functions:
    - per_disease_gate_summary       -> mean weights + entropy per disease
    - gate_weight_variance_within_disease -> is the gate context-sensitive
                                              or a disease lookup table?

Outputs (written to OUT_DIR):
  - exp2_gate_weights_long.csv        -- every (seed, fold, disease, sample) gate weight row
  - exp2_per_disease_gate_summary.csv -- mean TCN/BiLSTM/Transformer weight + entropy per disease
  - exp2_within_disease_variance.csv  -- per-disease gate-weight std (context sensitivity check)
  - exp2_gate_weights_by_disease.png  -- grouped bar chart, mean branch weight per disease
  - exp2_entropy_by_disease.png       -- gate entropy per disease (low = confident/specialized)

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment2_gate_inspection.py
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

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import Dctmn
import ncdcData


# ----------------------------------------------------------------------
# 0. CONFIGURATION -- must match Experiment 1 exactly for comparability
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp2_gate_inspection"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

BRANCH_NAMES = ["TCN", "BiLSTM", "Transformer"]  # order matches build_dctmn's fusion order


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
    return preds["gate_weights"]  # (n_test, 3)


def main():
    print("=" * 70)
    print("  EXPERIMENT 2: Per-disease and context gate-weight inspection")
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

            for i in range(len(disease_ids)):
                d_name = id_to_name[int(disease_ids[i])]
                all_rows.append({
                    "seed_run": seed_run,
                    "fold_test_year": test_year,
                    "disease": d_name,
                    "disease_id": int(disease_ids[i]),
                    "w_tcn": float(gate_weights[i, 0]),
                    "w_bilstm": float(gate_weights[i, 1]),
                    "w_transformer": float(gate_weights[i, 2]),
                })
            print("done")

    gw_df = pd.DataFrame(all_rows)
    gw_df.to_csv(os.path.join(OUT_DIR, "exp2_gate_weights_long.csv"), index=False)

    # ------------------------------------------------------------------
    # Per-disease summary (mean weights + entropy), using Dctmn.py's own
    # diagnostic functions so results are directly comparable to any
    # future re-run of the sanity check in Dctmn.py's __main__ block.
    # ------------------------------------------------------------------
    gate_weights_arr = gw_df[["w_tcn", "w_bilstm", "w_transformer"]].values
    disease_ids_arr = gw_df["disease_id"].values

    per_disease = Dctmn.per_disease_gate_summary(gate_weights_arr, disease_ids_arr, disease_names=id_to_name)

    print(f"\n{'='*70}")
    print("  PER-DISEASE MEAN GATE WEIGHTS + ENTROPY")
    print(f"  (entropy in nats; max possible = ln(3) = {np.log(3):.3f} = fully uniform/uncommitted;")
    print(f"   entropy near 0 = gate is confidently picking one branch for this disease)")
    print(f"{'='*70}\n")

    summary_rows = []
    for d_id, info in sorted(per_disease.items(), key=lambda kv: kv[1]["name"]):
        w = info["mean_weights"]
        print(f"  {info['name']:<14} TCN={w[0]:.3f}  BiLSTM={w[1]:.3f}  Transformer={w[2]:.3f}  "
              f"entropy={info['entropy']:.3f}  (n={info['n']})")
        summary_rows.append({
            "disease": info["name"], "mean_w_tcn": w[0], "mean_w_bilstm": w[1],
            "mean_w_transformer": w[2], "entropy": info["entropy"], "n": info["n"],
        })
    per_disease_df = pd.DataFrame(summary_rows)
    per_disease_df.to_csv(os.path.join(OUT_DIR, "exp2_per_disease_gate_summary.csv"), index=False)

    # Is there ANY meaningful spread across diseases, or did everything
    # collapse to ~[0.33, 0.33, 0.33]? This is the direct test for
    # Failure Mode A vs. everything else.
    weight_cols = ["mean_w_tcn", "mean_w_bilstm", "mean_w_transformer"]
    cross_disease_std = per_disease_df[weight_cols].std(axis=0)
    max_cross_disease_range = (per_disease_df[weight_cols].max() - per_disease_df[weight_cols].min())
    print(f"\n  Cross-disease std of mean weights (near 0 => diseases are indistinguishable to the gate):")
    print(f"    {cross_disease_std.to_dict()}")
    print(f"  Cross-disease max-min range per branch:")
    print(f"    {max_cross_disease_range.to_dict()}")

    if (cross_disease_std < 0.02).all():
        verdict_a = ("FAILURE MODE A: gate weights are nearly identical across all diseases "
                     "(std < 0.02 on every branch) -- the gate has collapsed to a uniform "
                     "lookup, consistent with Experiment 1's null result.")
    else:
        verdict_a = ("NOT failure mode A: diseases DO receive measurably different mean gate "
                     "weights. If Experiment 1 was still null, this points to Failure Mode B -- "
                     "the gate differentiates diseases, but that differentiation isn't (yet) "
                     "translating into lower forecast error.")
    print(f"\n  VERDICT: {verdict_a}")

    # ------------------------------------------------------------------
    # Within-disease variance: is the gate context-sensitive (reacting to
    # THIS window's dynamics), or just a per-disease constant repeated
    # across every sample? Uses Dctmn.py's own function directly.
    # ------------------------------------------------------------------
    variance_result = Dctmn.gate_weight_variance_within_disease(gate_weights_arr, disease_ids_arr)
    print(f"\n{'='*70}")
    print("  WITHIN-DISEASE GATE WEIGHT VARIANCE (context sensitivity check)")
    print(f"  (near 0 std => gate ignores context, acts as a disease-only lookup table;")
    print(f"   meaningful std => gate responds to each window's own dynamics)")
    print(f"{'='*70}\n")

    variance_rows = []
    for d_id, info in variance_result.items():
        d_name = id_to_name[d_id]
        std = info["std_per_branch"]
        print(f"  {d_name:<14} std_TCN={std[0]:.3f}  std_BiLSTM={std[1]:.3f}  "
              f"std_Transformer={std[2]:.3f}  (n={info['n']})")
        variance_rows.append({
            "disease": d_name, "std_w_tcn": std[0], "std_w_bilstm": std[1],
            "std_w_transformer": std[2], "n": info["n"],
        })
    variance_df = pd.DataFrame(variance_rows)
    variance_df.to_csv(os.path.join(OUT_DIR, "exp2_within_disease_variance.csv"), index=False)

    # ------------------------------------------------------------------
    # Plot 1: grouped bar chart, mean branch weight per disease
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(11, 6))
    x = np.arange(len(per_disease_df))
    width = 0.25
    ax.bar(x - width, per_disease_df["mean_w_tcn"], width, label="TCN (short-term)", color="#e34948")
    ax.bar(x, per_disease_df["mean_w_bilstm"], width, label="BiLSTM (medium-term)", color="#2a78d6")
    ax.bar(x + width, per_disease_df["mean_w_transformer"], width, label="Transformer (long-term)", color="#4a8fc9")
    ax.axhline(1/3, color="gray", ls="--", lw=1, label="Uniform (1/3)")
    ax.set_xticks(x)
    ax.set_xticklabels(per_disease_df["disease"], rotation=30, ha="right")
    ax.set_ylabel("Mean gate weight (across all seeds, folds, test samples)")
    ax.set_title("Experiment 2: Does the Gate Learn Disease-Specific Branch Preferences?")
    ax.legend()
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp2_gate_weights_by_disease.png"), dpi=150, bbox_inches="tight")
    plt.close()

    # ------------------------------------------------------------------
    # Plot 2: entropy per disease
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(9, 5))
    sorted_df = per_disease_df.sort_values("entropy")
    ax.barh(sorted_df["disease"], sorted_df["entropy"], color="#2a78d6")
    ax.axvline(np.log(3), color="gray", ls="--", lw=1, label=f"Max entropy = ln(3) = {np.log(3):.3f} (fully uniform)")
    ax.set_xlabel("Gate weight entropy (nats)")
    ax.set_title("Per-Disease Gate Confidence\n(lower = gate consistently favors one branch for this disease)")
    ax.legend()
    ax.grid(axis="x", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp2_entropy_by_disease.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()