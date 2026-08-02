"""
Experiment 6 -- Branch ablation: are all three branches even necessary?
==========================================================================

Follows directly from the conclusion of Experiments 1, 2, 4, and 5: a
learned, disease-and-context-conditioned gate does not beat simple
uniform averaging of TCN + BiLSTM + Transformer, at any tested horizon.

That result licenses a follow-up claim often made informally but not
yet actually tested in this project: "future researchers could just
use uniform fusion instead of a learned gate." This experiment tests
whether uniform fusion of all three branches is itself justified, or
whether a SINGLE branch alone -- or a subset of two -- does just as
well. If a single branch matches the full three-branch uniform-fusion
MAE, the honest recommendation shrinks further: not just "skip the
gate," but "skip two of the three branches entirely."

METHOD:
  Seven architecture variants, all IDENTICAL in every other respect
  (same head, same training procedure, same folds/seeds):

    1. tcn_only            -- TCN branch alone
    2. bilstm_only          -- BiLSTM branch alone
    3. transformer_only      -- Transformer branch alone
    4. no_tcn (bilstm+transformer, uniform average of the two)
    5. no_bilstm (tcn+transformer, uniform average of the two)
    6. no_transformer (tcn+bilstm, uniform average of the two)
    7. full_uniform (all three, uniform average -- this is Experiment 1's
       uniform_gate baseline, re-trained fresh here for a fair paired
       comparison across all seven variants at once)

  Single-branch variants use that branch's projection directly (no
  averaging needed, nothing to gate). Two- and three-branch variants
  use simple uniform averaging of the projected representations --
  deliberately NOT a learned gate, since Experiments 1/2/4 already
  established the gate provides no benefit; this isolates a different
  question (how many branches, not how they're weighted).

  Trained across the same expanding-window folds x 3 seeds used
  throughout this project, at horizon=1 (Experiments 1/2/3/4's horizon;
  Experiment 5 already showed longer horizons don't change the picture).

  Statistical comparison: for each of the 6 ablated variants, a paired
  Wilcoxon signed-rank test against full_uniform, on (seed, fold,
  disease) MAE pairs -- same paired design as every prior experiment
  in this project.

Outputs (written to OUT_DIR):
  - exp6_mae_by_variant_long.csv       -- every (seed, fold, disease, variant) MAE row
  - exp6_mae_summary_by_variant.csv    -- mean +/- std MAE per variant
  - exp6_wilcoxon_vs_full_uniform.csv  -- paired test, each variant vs. full_uniform
  - exp6_mae_by_variant.png            -- bar chart, all 7 variants side by side

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python experiment6_branch_ablation.py
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
from sklearn.metrics import mean_absolute_error
from scipy.stats import wilcoxon

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import Dctmn
import ncdcData


# ----------------------------------------------------------------------
# 0. CONFIGURATION -- must match Experiments 1-5
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp6_branch_ablation"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15

BRANCH_BUILDERS = {
    "tcn": Dctmn.build_tcn_branch,
    "bilstm": Dctmn.build_bilstm_branch,
    "transformer": Dctmn.build_transformer_branch,
}

VARIANTS = {
    "tcn_only": ["tcn"],
    "bilstm_only": ["bilstm"],
    "transformer_only": ["transformer"],
    "no_tcn": ["bilstm", "transformer"],
    "no_bilstm": ["tcn", "transformer"],
    "no_transformer": ["tcn", "bilstm"],
    "full_uniform": ["tcn", "bilstm", "transformer"],
}


# ----------------------------------------------------------------------
# 1. Generic branch-subset model (uniform averaging when >1 branch)
# ----------------------------------------------------------------------

def build_branch_subset_model(branch_names, seq_len, num_features, common_dim=64, forecast_horizon=1):
    """Builds a model using only the specified branches. If more than
    one branch is given, their projections are combined by simple
    UNIFORM averaging (not a learned gate -- Experiments 1/2/4 already
    established the gate adds nothing, so this isolates a different
    question: how many branches are needed at all)."""
    seq_input = layers.Input(shape=(seq_len, num_features), name="sequence_input")

    projections = []
    for name in branch_names:
        repr_ = BRANCH_BUILDERS[name](seq_input, name=name)
        proj = layers.Dense(common_dim, name=f"{name}_proj")(repr_)
        projections.append(proj)

    if len(projections) == 1:
        fused = projections[0]
    else:
        fused = layers.Average(name="uniform_average")(projections)

    x = layers.Dense(64, activation="relu", name="head_dense1")(fused)
    x = layers.Dropout(0.2, name="head_dropout")(x)
    forecast_output = layers.Dense(forecast_horizon, name="forecast_output")(x)

    model = models.Model(inputs=seq_input, outputs=forecast_output,
                          name="Branch_" + "_".join(branch_names))
    return model


def train_and_evaluate(model, fold, seed_run):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)

    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3, clipnorm=1.0), loss="mse")

    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
        monitor="val_loss", factor=0.5, patience=7, verbose=0)

    # Note: this model takes ONLY sequence_input (no disease_id_input) --
    # branch/uniform-average variants don't condition on disease identity
    # at all, consistent with testing "do the branches/fusion need disease
    # info" being a separate question already answered (no) in Exp 2.
    model.fit(
        fold["X_train"], fold["y_train"],
        validation_split=0.15,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        callbacks=[early_stop, reduce_lr],
        verbose=0,
    )

    y_pred_scaled = model.predict(fold["X_test"], verbose=0).reshape(-1)
    y_true_scaled = fold["y_test"]

    y_pred = ncdcData.inverse_scale_y(y_pred_scaled, fold["disease_id_test"],
                                       fold["scaler_mean"], fold["scaler_std"])
    y_true = ncdcData.inverse_scale_y(y_true_scaled, fold["disease_id_test"],
                                       fold["scaler_mean"], fold["scaler_std"])
    return y_true, y_pred


def per_disease_mae(y_true, y_pred, disease_id_test, id_to_name):
    rows = []
    for d_id in np.unique(disease_id_test):
        mask = disease_id_test == d_id
        mae = mean_absolute_error(y_true[mask], y_pred[mask])
        rows.append({"disease": id_to_name[int(d_id)], "MAE": mae, "n": int(mask.sum())})
    return rows


def main():
    print("=" * 70)
    print("  EXPERIMENT 6: Branch ablation -- are all three branches needed?")
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
            print(f"\n  Fold {test_year}:")

            for variant_name, branch_list in VARIANTS.items():
                print(f"    {variant_name:<18} ({'+'.join(branch_list)}) ... ", end="", flush=True)
                model = build_branch_subset_model(
                    branch_list, seq_len=SEQ_LEN, num_features=num_features, forecast_horizon=1,
                )
                y_true, y_pred = train_and_evaluate(model, fold, seed_run)
                rows = per_disease_mae(y_true, y_pred, fold["disease_id_test"], id_to_name)
                mae_agg = np.mean([r["MAE"] for r in rows])
                print(f"MAE={mae_agg:.3f}")
                for r in rows:
                    all_rows.append({**r, "variant": variant_name, "seed_run": seed_run,
                                      "fold_test_year": test_year})

    # ------------------------------------------------------------------
    # Aggregate
    # ------------------------------------------------------------------
    mae_df = pd.DataFrame(all_rows)
    mae_df.to_csv(os.path.join(OUT_DIR, "exp6_mae_by_variant_long.csv"), index=False)

    print(f"\n{'='*70}\n  SUMMARY: MAE by variant\n{'='*70}")
    summary = mae_df.groupby("variant")["MAE"].agg(["mean", "std", "count"])
    summary = summary.reindex(list(VARIANTS.keys()))
    print(summary.to_string())
    summary.to_csv(os.path.join(OUT_DIR, "exp6_mae_summary_by_variant.csv"))

    # ------------------------------------------------------------------
    # Paired Wilcoxon: each variant vs. full_uniform
    # ------------------------------------------------------------------
    print(f"\n{'='*70}\n  PAIRED WILCOXON: each variant vs. full_uniform\n{'='*70}\n")
    wilcoxon_rows = []
    for variant_name in VARIANTS:
        if variant_name == "full_uniform":
            continue
        pivot = mae_df[mae_df["variant"].isin(["full_uniform", variant_name])].pivot_table(
            index=["seed_run", "fold_test_year", "disease"], columns="variant", values="MAE"
        ).dropna()
        if len(pivot) >= 6:
            stat, p = wilcoxon(pivot["full_uniform"], pivot[variant_name], alternative="two-sided")
            median_delta = (pivot["full_uniform"] - pivot[variant_name]).median()
            if p < 0.05 and median_delta > 0:
                verdict = f"{variant_name} SIGNIFICANTLY BETTER than full_uniform"
            elif p < 0.05 and median_delta < 0:
                verdict = f"{variant_name} SIGNIFICANTLY WORSE than full_uniform"
            else:
                verdict = "no significant difference -- equivalent to full_uniform"
            print(f"  {variant_name:<18} n={len(pivot):<4} stat={stat:>8.1f}  p={p:.4f}  "
                  f"median_delta(full-variant)={median_delta:+.4f}  -> {verdict}")
            wilcoxon_rows.append({"variant": variant_name, "n_pairs": len(pivot), "wilcoxon_stat": stat,
                                   "p_value": p, "median_delta_full_minus_variant": median_delta,
                                   "verdict": verdict})
    wilcoxon_df = pd.DataFrame(wilcoxon_rows)
    wilcoxon_df.to_csv(os.path.join(OUT_DIR, "exp6_wilcoxon_vs_full_uniform.csv"), index=False)

    equivalent_variants = wilcoxon_df[wilcoxon_df["verdict"].str.contains("no significant")]["variant"].tolist()
    if equivalent_variants:
        simplest = min(equivalent_variants, key=lambda v: len(VARIANTS[v]))
        print(f"\n  SIMPLEST VARIANT STATISTICALLY EQUIVALENT TO full_uniform: '{simplest}' "
              f"({'+'.join(VARIANTS[simplest])}, {len(VARIANTS[simplest])} branch(es))")
        print(f"  If reproducible, the honest recommendation for this dataset is: "
              f"use '{simplest}', not the full three-branch fusion and definitely not a learned gate.")
    else:
        print(f"\n  No ablated variant is statistically equivalent to full_uniform -- "
              f"all three branches appear necessary together.")

    # ------------------------------------------------------------------
    # Plot: MAE by variant
    # ------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(10, 6))
    order = list(VARIANTS.keys())
    means = [summary.loc[v, "mean"] for v in order]
    stds = [summary.loc[v, "std"] for v in order]
    colors = ["#4a8fc9" if v != "full_uniform" else "#e34948" for v in order]
    bars = ax.bar(order, means, yerr=stds, color=colors, capsize=5)
    for b, v in zip(bars, means):
        ax.text(b.get_x() + b.get_width()/2, b.get_height() + 0.5, f"{v:.2f}",
                ha="center", va="bottom", fontsize=9)
    ax.set_ylabel("Aggregate MAE (mean across diseases, folds, seeds)")
    ax.set_title("Experiment 6: Branch Ablation -- How Many Branches Are Actually Needed?")
    ax.set_xticklabels(order, rotation=30, ha="right")
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp6_mae_by_variant.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()