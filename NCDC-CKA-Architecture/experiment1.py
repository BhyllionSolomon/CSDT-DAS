"""
Experiment 1 -- Does DCTMN's learned gate beat uniform-weight fusion?
======================================================================

Research question (from the DCTMN research goal):
  Can a model learn when to trust short-term (TCN), medium-term
  (Bi-LSTM), or long-term (Transformer) temporal patterns for
  different diseases, and does that improve forecasting?

This script is the FIRST test of that question. It trains two variants
that are IDENTICAL in every respect except one:

  1. "learned_gate"  -- the real DCTMN. Gate weights are produced by a
                         trained MLP conditioned on disease id + input
                         statistics (see build_context_aware_gate in
                         Dctmn.py).
  2. "uniform_gate"  -- the SAME architecture (same branches, same
                         projections, same fusion head, same training
                         procedure), except the gate's softmax output
                         is replaced with a frozen constant [1/3,1/3,1/3]
                         for every sample. This isolates whether LEARNED,
                         context-sensitive weighting helps, as opposed to
                         "having three branches at all" or "having a
                         gate network's extra parameters" helping.

Design choices (locked in before running, not adjusted after seeing
results -- consistent with the discipline already applied in the
hetero/CKA script):
  - Fold scheme: expanding-window folds (train: years < Y, test: year Y),
    identical to Cka.py / the hetero script, via ncdcData.build_dctmn_folds.
  - seq_len=12, horizon=1 -- ncdcData.py's own defaults, since DCTMN's
    data glue was built around these.
  - 3 seeds (42, 123, 2024) per fold, matching the rigor level already
    established for the hetero/CKA comparison.
  - Paired statistic: Wilcoxon signed-rank test on (seed, fold, disease)
    MAE pairs, learned vs. uniform. Paired because both variants see
    the EXACT same train/test split and data for a given (seed, fold,
    disease) -- only the gate differs.

Outputs (written to OUT_DIR):
  - exp1_mae_long_RAW_ALL_FOLDS.csv   -- every (seed, fold, disease, variant) MAE row
  - exp1_mae_summary.csv              -- mean +/- std per variant, per disease
  - exp1_wilcoxon_test.csv            -- paired significance test result
  - exp1_learned_vs_uniform_mae.png   -- bar chart, aggregate MAE by variant
  - exp1_per_disease_delta.png        -- per-disease MAE delta (uniform - learned)

USAGE:
  1. Edit DATA_PATH / OUT_DIR below if your folders move again.
  2. Run from the NCDC-CKA-Architecture folder (so `import Dctmn` and
     `import ncdcData` resolve), e.g.:
         python experiment1_gate_vs_uniform.py
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
from sklearn.metrics import mean_absolute_error, mean_squared_error
from scipy.stats import wilcoxon

# Make sure Dctmn.py and ncdcData.py (same folder as this script) are importable
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import Dctmn
import ncdcData


# ----------------------------------------------------------------------
# 0. CONFIGURATION -- edit these two paths if folders move again
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp1_gate_vs_uniform"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]  # trimmed to real folds by min_test_examples
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15


# ----------------------------------------------------------------------
# 1. Frozen-gate DCTMN variant
# ----------------------------------------------------------------------
# Mirrors Dctmn.build_dctmn() exactly, except the learned softmax gate
# is replaced by a constant [1/3, 1/3, 1/3] broadcast to the batch.
# Branches, projections, fusion mechanism, and forecast head are
# IDENTICAL code paths to the learned-gate model -- only this one
# tensor differs.

class UniformGateLayer(layers.Layer):
    """Outputs a constant [1/num_branches, ...] gate, broadcast to batch size.
    Takes disease_id_input purely to read the batch size at call time --
    it does not use the disease id's value in any way."""
    def __init__(self, num_branches=3, name="uniform_gate", **kwargs):
        super().__init__(name=name, **kwargs)
        self.num_branches = num_branches

    def call(self, disease_id_input):
        batch_size = tf.shape(disease_id_input)[0]
        uniform = tf.fill([batch_size, self.num_branches], 1.0 / self.num_branches)
        return uniform


def build_dctmn_uniform_gate(
    seq_len,
    num_features,
    num_diseases,
    forecast_horizon=1,
    embed_dim=16,
    common_dim=64,
    recent_window=3,
):
    """Same architecture as Dctmn.build_dctmn, gate frozen to uniform weights."""
    seq_input = layers.Input(shape=(seq_len, num_features), name="sequence_input")
    disease_id_input = layers.Input(shape=(), dtype="int32", name="disease_id_input")

    tcn_repr = Dctmn.build_tcn_branch(seq_input)
    bilstm_repr = Dctmn.build_bilstm_branch(seq_input)
    transformer_repr = Dctmn.build_transformer_branch(seq_input)
    input_statistics_raw = Dctmn.InputStatisticsLayer(recent_window=recent_window)(seq_input)
    input_statistics = layers.LayerNormalization(name="input_stats_norm")(input_statistics_raw)

    tcn_proj = layers.Dense(common_dim, name="tcn_proj")(tcn_repr)
    bilstm_proj = layers.Dense(common_dim, name="bilstm_proj")(bilstm_repr)
    transformer_proj = layers.Dense(common_dim, name="transformer_proj")(transformer_repr)

    # Still compute a disease embedding, purely so parameter count / disease
    # awareness elsewhere in the network is comparable -- but it is NOT fed
    # into gating, since gating is frozen uniform here.
    embed = layers.Embedding(input_dim=num_diseases, output_dim=embed_dim,
                              name="disease_embed_unused_by_gate")(disease_id_input)
    disease_embedding = layers.Flatten(name="embed_flat_unused_by_gate")(embed)

    gate_weights = UniformGateLayer(num_branches=3)(disease_id_input)

    fused = Dctmn.WeightedBranchFusion(name="weighted_fusion")(
        [tcn_proj, bilstm_proj, transformer_proj], gate_weights
    )

    x = layers.Dense(64, activation="relu", name="head_dense1")(fused)
    x = layers.Dropout(0.2, name="head_dropout")(x)
    forecast_output = layers.Dense(forecast_horizon, name="forecast_output")(x)

    model = models.Model(
        inputs=[seq_input, disease_id_input],
        outputs={
            "forecast": forecast_output,
            "gate_weights": gate_weights,
            "disease_embedding": disease_embedding,
            "input_statistics": input_statistics,
        },
        name="DCTMN_uniform_gate",
    )
    return model


# ----------------------------------------------------------------------
# 2. Train + evaluate one variant on one fold
# ----------------------------------------------------------------------

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


def per_disease_metrics(y_true, y_pred, disease_id_test, disease_names, disease_id_map):
    id_to_name = {v: k for k, v in disease_id_map.items()}
    rows = []
    for d_id in np.unique(disease_id_test):
        mask = disease_id_test == d_id
        mae = mean_absolute_error(y_true[mask], y_pred[mask])
        rmse = np.sqrt(mean_squared_error(y_true[mask], y_pred[mask]))
        rows.append({"disease": id_to_name[int(d_id)], "MAE": mae, "RMSE": rmse, "n": int(mask.sum())})
    return rows


# ----------------------------------------------------------------------
# 3. Main experiment loop
# ----------------------------------------------------------------------

def main():
    print("=" * 70)
    print("  EXPERIMENT 1: Learned gate vs. frozen uniform-weight fusion")
    print("=" * 70)

    folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
        DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=HORIZON,
        min_test_examples=MIN_TEST_EXAMPLES,
    )
    num_diseases = len(disease_names)
    num_features = num_diseases  # wide_df feature columns == diseases (shared window)

    all_rows = []

    for seed_run in SEEDS:
        print(f"\n{'#'*70}\n  SEED: {seed_run}\n{'#'*70}")

        for fold in folds:
            test_year = fold["test_year"]
            print(f"\n{'='*70}\n  Fold test_year={test_year}\n{'='*70}")

            # ---- learned gate (real DCTMN) ----
            print("  Training learned-gate DCTMN ...", end=" ", flush=True)
            learned_model = Dctmn.build_dctmn(
                seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
                forecast_horizon=1,
            )
            y_true, y_pred_learned, gate_w_learned = train_and_evaluate(learned_model, fold, seed_run)
            print("done")

            learned_rows = per_disease_metrics(y_true, y_pred_learned, fold["disease_id_test"],
                                                disease_names, disease_id_map)
            for r in learned_rows:
                all_rows.append({**r, "variant": "learned_gate", "seed_run": seed_run, "fold_test_year": test_year})

            learned_mae_agg = np.mean([r["MAE"] for r in learned_rows])
            print(f"    learned_gate aggregate MAE: {learned_mae_agg:.3f}")

            # ---- uniform gate (frozen baseline) ----
            print("  Training uniform-gate baseline ...", end=" ", flush=True)
            uniform_model = build_dctmn_uniform_gate(
                seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
                forecast_horizon=1,
            )
            y_true_u, y_pred_uniform, gate_w_uniform = train_and_evaluate(uniform_model, fold, seed_run)
            print("done")

            uniform_rows = per_disease_metrics(y_true_u, y_pred_uniform, fold["disease_id_test"],
                                                disease_names, disease_id_map)
            for r in uniform_rows:
                all_rows.append({**r, "variant": "uniform_gate", "seed_run": seed_run, "fold_test_year": test_year})

            uniform_mae_agg = np.mean([r["MAE"] for r in uniform_rows])
            delta = uniform_mae_agg - learned_mae_agg
            direction = "learned gate WINS" if delta > 0 else "uniform gate wins / ties"
            print(f"    uniform_gate aggregate MAE: {uniform_mae_agg:.3f}  "
                  f"(delta vs learned: {delta:+.3f} -> {direction})")

            # sanity check: did the learned gate actually vary across diseases,
            # or collapse to ~uniform on its own?
            gw_std_across_diseases = gate_w_learned.std(axis=0)
            print(f"    learned gate weight std across ALL test samples "
                  f"(near 0 => gate barely differentiates anything): {gw_std_across_diseases}")

    # ------------------------------------------------------------------
    # 4. Aggregate, test, save, plot
    # ------------------------------------------------------------------
    mae_df = pd.DataFrame(all_rows)
    mae_df.to_csv(os.path.join(OUT_DIR, "exp1_mae_long_RAW_ALL_FOLDS.csv"), index=False)

    print(f"\n{'='*70}\n  SUMMARY: MAE by variant\n{'='*70}")
    summary = mae_df.groupby(["variant"])["MAE"].agg(["mean", "std", "count"])
    print(summary.to_string())
    summary.to_csv(os.path.join(OUT_DIR, "exp1_mae_summary.csv"))

    print(f"\n  Per-disease MAE by variant:\n")
    per_disease_summary = mae_df.groupby(["disease", "variant"])["MAE"].mean().unstack()
    print(per_disease_summary.to_string())
    per_disease_summary.to_csv(os.path.join(OUT_DIR, "exp1_per_disease_mae_summary.csv"))

    # ---- Paired Wilcoxon signed-rank test ----
    # Pair on (seed_run, fold_test_year, disease) -- both variants see the
    # identical split/data for that key, so this is a genuine paired test.
    pivot = mae_df.pivot_table(
        index=["seed_run", "fold_test_year", "disease"], columns="variant", values="MAE"
    ).dropna()
    print(f"\n{'='*70}\n  PAIRED WILCOXON SIGNED-RANK TEST (learned_gate vs uniform_gate)\n"
          f"  n paired (seed, fold, disease) observations: {len(pivot)}\n{'='*70}")

    if len(pivot) >= 6:
        stat, p_value = wilcoxon(pivot["uniform_gate"], pivot["learned_gate"], alternative="greater")
        median_delta = (pivot["uniform_gate"] - pivot["learned_gate"]).median()
        print(f"\n  H1 (one-sided): uniform_gate MAE > learned_gate MAE (learned gate helps)")
        print(f"  Wilcoxon statistic = {stat:.3f}, p = {p_value:.4f}")
        print(f"  Median (uniform_gate MAE - learned_gate MAE) = {median_delta:+.3f}")
        conclusion = ("SUPPORTS the hypothesis that learned, context-sensitive gating "
                      "improves forecasting" if p_value < 0.05 and median_delta > 0
                      else "does NOT reach significance -- learned gate has not been shown "
                           "to beat uniform fusion on this data")
        print(f"  CONCLUSION: {conclusion}")

        pd.DataFrame([{
            "n_pairs": len(pivot), "wilcoxon_stat": stat, "p_value": p_value,
            "median_delta_uniform_minus_learned": median_delta, "conclusion": conclusion,
        }]).to_csv(os.path.join(OUT_DIR, "exp1_wilcoxon_test.csv"), index=False)
    else:
        print("  Not enough paired observations for a meaningful test yet.")

    # ---- Plot 1: aggregate MAE bar chart ----
    fig, ax = plt.subplots(figsize=(6, 5))
    means = [summary.loc["learned_gate", "mean"], summary.loc["uniform_gate", "mean"]]
    stds = [summary.loc["learned_gate", "std"], summary.loc["uniform_gate", "std"]]
    bars = ax.bar(["Learned gate\n(real DCTMN)", "Uniform gate\n(frozen 1/3,1/3,1/3)"],
                   means, yerr=stds, color=["#2a78d6", "#a0a0a0"], capsize=6, width=0.5)
    for b, v in zip(bars, means):
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.5, f"{v:.2f}",
                ha="center", va="bottom", fontsize=10)
    ax.set_ylabel("Aggregate MAE (mean across diseases, folds, seeds)")
    ax.set_title("Experiment 1: Does Learned Gating Beat Uniform Fusion?")
    ax.grid(axis="y", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp1_learned_vs_uniform_mae.png"), dpi=150, bbox_inches="tight")
    plt.close()

    # ---- Plot 2: per-disease delta ----
    delta_by_disease = (per_disease_summary["uniform_gate"] - per_disease_summary["learned_gate"]).sort_values()
    fig, ax = plt.subplots(figsize=(8, 5))
    colors = ["#2a78d6" if v > 0 else "#e34948" for v in delta_by_disease.values]
    ax.barh(delta_by_disease.index, delta_by_disease.values, color=colors)
    ax.axvline(0, color="black", lw=0.8)
    ax.set_xlabel("MAE(uniform_gate) - MAE(learned_gate)\n(positive = learned gate wins for this disease)")
    ax.set_title("Per-Disease Benefit of Learned Gating")
    ax.grid(axis="x", alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp1_per_disease_delta.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()