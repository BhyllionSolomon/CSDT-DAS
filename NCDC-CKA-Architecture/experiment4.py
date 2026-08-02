"""
Experiment 4 -- Can a sharper (entropy-regularized) gate turn Experiment
3's real-but-timid context shift into an actual accuracy gain?
============================================================================

Diagnosis from Experiments 1-3:
  - Exp 1: learned gate does NOT beat uniform fusion (p=0.383, null).
  - Exp 2: gate does NOT differentiate between diseases (Failure Mode A).
  - Exp 3: gate DOES shift with outbreak-vs-quiet context, disease-
    agnostically (TCN down / BiLSTM up during outbreak, p<0.001 pooled),
    but the shift is small (entropy ~0.96-1.00 nats, vs. max possible
    ln(3)=1.099 -- i.e. barely below "completely uncommitted").

Hypothesis for this experiment:
  The context-sensitivity is real but too WEAK to move the fused
  representation enough to matter for forecast accuracy. If we push the
  gate toward more decisive (lower-entropy) weighting -- via an explicit
  entropy penalty added to the training loss -- the SAME context signal
  Experiment 3 found may translate into an actual MAE improvement over
  uniform fusion, where the unregularized gate could not.

  If sharpening the gate does NOT help either, that argues the
  context-signal itself is too weak/noisy to be useful for this task at
  this data scale, regardless of how decisively the gate commits to it --
  a different and more final conclusion than "the gate just wasn't
  decisive enough."

METHOD:
  Add an entropy penalty directly to the model via model.add_loss(),
  computed on the gate_weights output tensor:

      entropy_loss = mean( -sum(gate_weights * log(gate_weights)) )
      total_loss   = mse_loss + entropy_weight * entropy_loss

  Lower entropy = more decisive/confident gate. This is a STANDARD
  mixture-of-experts regularizer (used to prevent gate collapse to
  uniform mixing) -- it shapes the TRAINING objective, not the test
  metric, so it is not "cheating": the model must still generalize to
  unseen test weeks under whatever gate behavior the penalty encourages.

  Sweeps entropy_weight in {0.0, 0.02, 0.05, 0.1, 0.2}. 0.0 reproduces
  Experiment 1's original learned-gate result exactly (sanity check).

  Trained across the SAME expanding-window folds x 3 seeds as
  Experiments 1-3, and compared against the uniform-gate baseline MAE
  already established in Experiment 1 (re-used, not re-trained, since
  the uniform baseline has no entropy_weight to sweep).

Outputs (written to OUT_DIR):
  - exp4_mae_by_entropy_weight_long.csv   -- every (seed, fold, disease, entropy_weight) MAE row
  - exp4_mae_summary_by_weight.csv        -- mean +/- std MAE per entropy_weight setting
  - exp4_gate_entropy_by_weight.csv       -- mean realized gate entropy per entropy_weight (sanity check the penalty worked)
  - exp4_wilcoxon_vs_uniform.csv          -- paired test, each entropy_weight setting vs. uniform baseline
  - exp4_mae_vs_entropy_weight.png        -- does sharpening the gate help, hurt, or do nothing?
  - exp4_realized_entropy_vs_weight.png   -- confirms the penalty is actually reducing entropy as intended

USAGE:
  Run from the NCDC-CKA-Architecture folder. Needs Experiment 1's
  uniform_gate results -- re-uses the frozen-gate model definition from
  experiment1_gate_vs_uniform.py.
      python experiment4_entropy_regularized_gate.py
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
from experiment1 import build_dctmn_uniform_gate  # re-use exact baseline definition

# ----------------------------------------------------------------------
# 0. CONFIGURATION -- must match Experiments 1-3 exactly
# ----------------------------------------------------------------------

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\results_exp4_entropy_regularized_gate"
os.makedirs(OUT_DIR, exist_ok=True)

SEQ_LEN = 12
HORIZON = 1
TEST_YEARS = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
MIN_TEST_EXAMPLES = 1

SEEDS = [42, 123, 2024]
ENTROPY_WEIGHTS = [0.0, 0.02, 0.05, 0.1, 0.2]

EPOCHS = 100
BATCH_SIZE = 32
PATIENCE = 15


# ----------------------------------------------------------------------
# 1. DCTMN variant with an entropy penalty on the gate
# ----------------------------------------------------------------------

def build_dctmn_entropy_regularized(
    seq_len, num_features, num_diseases, entropy_weight,
    forecast_horizon=1, embed_dim=16, common_dim=64, recent_window=3,
):
    """Identical to Dctmn.build_dctmn, except an entropy penalty on
    gate_weights is added directly to the model's training loss via
    model.add_loss(). entropy_weight=0.0 must reproduce the original
    Dctmn.build_dctmn behavior exactly (sanity check, see main())."""
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

    gate_weights, disease_embedding = Dctmn.build_context_aware_gate(
        disease_id_input, input_statistics=input_statistics,
        num_diseases=num_diseases, embed_dim=embed_dim,
    )

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
        name=f"DCTMN_entropy_{entropy_weight}",
    )

    if entropy_weight > 0.0:
        entropy_per_sample = -tf.reduce_sum(
            gate_weights * tf.math.log(gate_weights + 1e-9), axis=-1
        )
        entropy_loss = tf.reduce_mean(entropy_per_sample)
        model.add_loss(entropy_weight * entropy_loss)
        model.add_metric(entropy_loss, name="gate_entropy")

    return model


# ----------------------------------------------------------------------
# 2. Train + evaluate one (fold, seed, entropy_weight) combination
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

    gate_entropy = -np.sum(preds["gate_weights"] * np.log(preds["gate_weights"] + 1e-9), axis=-1)

    return y_true, y_pred, gate_entropy.mean()


def per_disease_mae(y_true, y_pred, disease_id_test, disease_id_map):
    id_to_name = {v: k for k, v in disease_id_map.items()}
    rows = []
    for d_id in np.unique(disease_id_test):
        mask = disease_id_test == d_id
        mae = mean_absolute_error(y_true[mask], y_pred[mask])
        rows.append({"disease": id_to_name[int(d_id)], "MAE": mae, "n": int(mask.sum())})
    return rows


def train_uniform_baseline(fold, seed_run, num_features, num_diseases):
    tf.random.set_seed(seed_run)
    np.random.seed(seed_run)
    model = build_dctmn_uniform_gate(
        seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases, forecast_horizon=1,
    )
    return train_and_evaluate(model, fold, seed_run)


# ----------------------------------------------------------------------
# 3. Main sweep
# ----------------------------------------------------------------------

def main():
    print("=" * 70)
    print("  EXPERIMENT 4: Does an entropy-regularized (sharper) gate")
    print("  translate Experiment 3's context shift into accuracy gains?")
    print("=" * 70)

    folds, disease_id_map, disease_names = ncdcData.build_dctmn_folds(
        DATA_PATH, test_years=TEST_YEARS, seq_len=SEQ_LEN, horizon=HORIZON,
        min_test_examples=MIN_TEST_EXAMPLES,
    )
    num_diseases = len(disease_names)
    num_features = num_diseases

    all_rows = []
    entropy_rows = []

    for seed_run in SEEDS:
        print(f"\n{'#'*70}\n  SEED: {seed_run}\n{'#'*70}")

        for fold in folds:
            test_year = fold["test_year"]
            print(f"\n{'='*70}\n  Fold test_year={test_year}\n{'='*70}")

            # ---- uniform baseline (needed fresh per seed/fold for the paired test) ----
            print("  Training uniform_gate baseline ... ", end="", flush=True)
            y_true_u, y_pred_u, _ = train_uniform_baseline(fold, seed_run, num_features, num_diseases)
            print("done")
            for r in per_disease_mae(y_true_u, y_pred_u, fold["disease_id_test"], disease_id_map):
                all_rows.append({**r, "variant": "uniform_gate", "seed_run": seed_run, "fold_test_year": test_year})

            # ---- entropy-regularized sweep ----
            for ew in ENTROPY_WEIGHTS:
                label = f"entropy_w_{ew}"
                print(f"  Training entropy_weight={ew} ... ", end="", flush=True)
                model = build_dctmn_entropy_regularized(
                    seq_len=SEQ_LEN, num_features=num_features, num_diseases=num_diseases,
                    entropy_weight=ew, forecast_horizon=1,
                )
                y_true, y_pred, mean_entropy = train_and_evaluate(model, fold, seed_run)
                print(f"done (realized mean gate entropy = {mean_entropy:.4f})")

                for r in per_disease_mae(y_true, y_pred, fold["disease_id_test"], disease_id_map):
                    all_rows.append({**r, "variant": label, "seed_run": seed_run, "fold_test_year": test_year})
                entropy_rows.append({"seed_run": seed_run, "fold_test_year": test_year,
                                      "entropy_weight": ew, "mean_gate_entropy": mean_entropy})

    # ------------------------------------------------------------------
    # 4. Aggregate, test, save, plot
    # ------------------------------------------------------------------
    mae_df = pd.DataFrame(all_rows)
    mae_df.to_csv(os.path.join(OUT_DIR, "exp4_mae_by_entropy_weight_long.csv"), index=False)

    entropy_df = pd.DataFrame(entropy_rows)
    entropy_summary = entropy_df.groupby("entropy_weight")["mean_gate_entropy"].agg(["mean", "std"])
    entropy_summary.to_csv(os.path.join(OUT_DIR, "exp4_gate_entropy_by_weight.csv"))

    print(f"\n{'='*70}\n  SANITY CHECK: did the entropy penalty actually reduce entropy?\n{'='*70}")
    print(entropy_summary.to_string())
    print(f"  (max possible entropy = ln(3) = {np.log(3):.4f}; lower = more decisive gate)")

    print(f"\n{'='*70}\n  SUMMARY: MAE by variant\n{'='*70}")
    summary = mae_df.groupby("variant")["MAE"].agg(["mean", "std", "count"])
    summary = summary.reindex(["uniform_gate"] + [f"entropy_w_{ew}" for ew in ENTROPY_WEIGHTS])
    print(summary.to_string())
    summary.to_csv(os.path.join(OUT_DIR, "exp4_mae_summary_by_weight.csv"))

    # ---- Paired Wilcoxon: each entropy_weight setting vs. uniform_gate ----
    print(f"\n{'='*70}\n  PAIRED WILCOXON: each entropy_weight vs. uniform_gate\n{'='*70}\n")
    wilcoxon_rows = []
    for ew in ENTROPY_WEIGHTS:
        label = f"entropy_w_{ew}"
        pivot = mae_df[mae_df["variant"].isin(["uniform_gate", label])].pivot_table(
            index=["seed_run", "fold_test_year", "disease"], columns="variant", values="MAE"
        ).dropna()
        if len(pivot) >= 6:
            stat, p = wilcoxon(pivot["uniform_gate"], pivot[label], alternative="greater")
            median_delta = (pivot["uniform_gate"] - pivot[label]).median()
            sig = "SIGNIFICANT -- this gate beats uniform fusion" if (p < 0.05 and median_delta > 0) else "not significant"
            print(f"  entropy_weight={ew:<5} n={len(pivot):<4} stat={stat:>8.1f}  p={p:.4f}  "
                  f"median_delta={median_delta:+.4f}  -> {sig}")
            wilcoxon_rows.append({"entropy_weight": ew, "n_pairs": len(pivot), "wilcoxon_stat": stat,
                                   "p_value": p, "median_delta_uniform_minus_entropy_gate": median_delta,
                                   "verdict": sig})
    pd.DataFrame(wilcoxon_rows).to_csv(os.path.join(OUT_DIR, "exp4_wilcoxon_vs_uniform.csv"), index=False)

    # ---- Plot 1: MAE vs entropy_weight ----
    fig, ax = plt.subplots(figsize=(8, 5))
    xs = ENTROPY_WEIGHTS
    ys = [summary.loc[f"entropy_w_{ew}", "mean"] for ew in xs]
    errs = [summary.loc[f"entropy_w_{ew}", "std"] for ew in xs]
    ax.errorbar(xs, ys, yerr=errs, marker="o", color="#2a78d6", capsize=5, label="Entropy-regularized gate")
    ax.axhline(summary.loc["uniform_gate", "mean"], color="gray", ls="--",
               label=f"Uniform gate baseline ({summary.loc['uniform_gate', 'mean']:.3f})")
    ax.set_xlabel("Entropy penalty weight (higher = gate pushed to be more decisive)")
    ax.set_ylabel("Aggregate MAE")
    ax.set_title("Experiment 4: Does Sharpening the Gate Help Forecast Accuracy?")
    ax.legend()
    ax.grid(alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp4_mae_vs_entropy_weight.png"), dpi=150, bbox_inches="tight")
    plt.close()

    # ---- Plot 2: realized entropy vs weight (confirms penalty worked mechanically) ----
    fig, ax = plt.subplots(figsize=(7, 5))
    ax.errorbar(entropy_summary.index, entropy_summary["mean"], yerr=entropy_summary["std"],
                marker="o", color="#e34948", capsize=5)
    ax.axhline(np.log(3), color="gray", ls="--", label=f"Max entropy = ln(3) = {np.log(3):.3f}")
    ax.set_xlabel("Entropy penalty weight")
    ax.set_ylabel("Realized mean gate entropy (nats)")
    ax.set_title("Sanity Check: Does the Penalty Actually Sharpen the Gate?")
    ax.legend()
    ax.grid(alpha=0.3)
    plt.tight_layout()
    fig.savefig(os.path.join(OUT_DIR, "exp4_realized_entropy_vs_weight.png"), dpi=150, bbox_inches="tight")
    plt.close()

    print(f"\n  All outputs saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()