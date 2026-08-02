"""
Disease-Conditioned Temporal Mixture Network (DCTMN) -- v3
============================================================

v2 limitation (fixed): the gate consumed the pooled outputs of the very
branches it was about to weight:

    Branches -> Gate -> re-weights the same Branches

Mathematically valid, but it invites a fair reviewer objection: is the
gate learning disease-specific temporal reasoning, or just picking
whichever branch happened to produce the strongest activation for this
input? The causal story is muddy.

Fix in v3: the gate now conditions on disease identity + input
statistics computed DIRECTLY FROM THE RAW SEQUENCE, before any branch
runs. The branches and the gate become two independent, parallel
consumers of the same raw input -- the gate is not looking at anyone's
output when it decides how to weight them.

    Sequence input
         |
    -----+------------------------------+
    |                                    |
    v                                    v
  TCN / BiLSTM / Transformer      Input Statistics
  (representation branches)       (mean, std, volatility,
    |                               trend, recent growth rate)
    |                                    |
    |                          Disease ID -> Embedding
    |                                    |
    |                          [ disease_embedding, input_statistics ]
    |                                    |
    |                                  Gate (MLP + softmax)
    |                                    |
    |                          [w_tcn, w_bilstm, w_transformer]
    |                                    |
    +--------------- weighted sum -------+
                    |
              Fusion head -> forecast

Causal story is now clean:

    Disease + Current Context
              |
            Gate
              |
      Branch Preference
              |
           Fusion

rather than v2's "branches gate themselves."

Design rules preserved:
  - No hard-coded epidemiological rules.
  - Gate weights, disease embedding, AND input statistics all exposed
    for post-hoc analysis.
  - `gate_weight_variance_within_disease` kept from v2 -- still the
    direct test of whether the gate is genuinely context-sensitive
    or has collapsed back to a disease lookup table.
"""

import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, models

MODEL_NAME = "DCTMN"  # Disease-Conditioned Temporal Mixture Network


# ----------------------------------------------------------------------
# 1. Branch encoders (unchanged)
# ----------------------------------------------------------------------

def build_tcn_branch(seq_input, filters=64, kernel_size=3, dilations=(1, 2, 4, 8), name="tcn"):
    """Dilated causal Conv1D stack -> pooled representation vector."""
    x = seq_input
    for i, d in enumerate(dilations):
        x = layers.Conv1D(
            filters=filters,
            kernel_size=kernel_size,
            dilation_rate=d,
            padding="causal",
            activation="relu",
            name=f"{name}_conv{i}_d{d}",
        )(x)
        x = layers.LayerNormalization(name=f"{name}_ln{i}")(x)
    branch_repr = layers.GlobalAveragePooling1D(name=f"{name}_pool")(x)
    return branch_repr


def build_bilstm_branch(seq_input, units=64, name="bilstm"):
    """BiLSTM -> pooled representation vector."""
    x = layers.Bidirectional(
        layers.LSTM(units, return_sequences=True), name=f"{name}_bilstm"
    )(seq_input)
    branch_repr = layers.GlobalAveragePooling1D(name=f"{name}_pool")(x)
    return branch_repr


def build_transformer_branch(seq_input, num_heads=4, key_dim=32, ff_dim=128, name="transformer"):
    """Self-attention encoder block -> pooled representation vector."""
    attn_out = layers.MultiHeadAttention(
        num_heads=num_heads, key_dim=key_dim, name=f"{name}_mha"
    )(seq_input, seq_input)
    x = layers.LayerNormalization(name=f"{name}_ln1")(seq_input + attn_out)
    ff = layers.Dense(ff_dim, activation="relu", name=f"{name}_ff1")(x)
    ff = layers.Dense(x.shape[-1], name=f"{name}_ff2")(ff)
    x = layers.LayerNormalization(name=f"{name}_ln2")(x + ff)
    branch_repr = layers.GlobalAveragePooling1D(name=f"{name}_pool")(x)
    return branch_repr


# ----------------------------------------------------------------------
# 2. Input statistics (computed BEFORE any branch runs)
# ----------------------------------------------------------------------

class InputStatisticsLayer(layers.Layer):
    """
    Computes a compact numeric summary of the raw input window, per
    feature: mean, std, volatility (std of first differences), linear
    trend (OLS slope over time), and recent growth rate (last-window
    mean vs first-window mean, normalized).

    Output shape: (batch, num_features * 5)

    This is deliberately independent of the TCN/BiLSTM/Transformer
    branches -- it only ever looks at the raw sequence input, so the
    gate that consumes it cannot be accused of "looking at the
    branches' answers before grading them."
    """
    def __init__(self, recent_window=3, eps=1e-6, name="input_stats", **kwargs):
        super().__init__(name=name, **kwargs)
        self.recent_window = recent_window
        self.eps = eps

    def call(self, x):
        # x: (batch, seq_len, num_features)
        seq_len = x.shape[1]

        mean = tf.reduce_mean(x, axis=1)                 # (batch, features)
        std = tf.math.reduce_std(x, axis=1)               # (batch, features)

        diffs = x[:, 1:, :] - x[:, :-1, :]
        volatility = tf.math.reduce_std(diffs, axis=1)    # (batch, features)

        # OLS trend: slope of x over timestep index
        t = tf.range(seq_len, dtype=x.dtype)              # (seq_len,)
        t_centered = t - tf.reduce_mean(t)                # (seq_len,)
        denom = tf.reduce_sum(tf.square(t_centered)) + self.eps
        x_centered = x - tf.reduce_mean(x, axis=1, keepdims=True)
        numer = tf.reduce_sum(t_centered[None, :, None] * x_centered, axis=1)
        trend = numer / denom                             # (batch, features)

        # recent growth rate: last-window mean vs first-window mean
        w = min(self.recent_window, seq_len)
        recent = tf.reduce_mean(x[:, -w:, :], axis=1)
        early = tf.reduce_mean(x[:, :w, :], axis=1)
        growth_rate = (recent - early) / (tf.abs(early) + self.eps)

        stats = tf.concat([mean, std, volatility, trend, growth_rate], axis=-1)
        return stats

    def get_config(self):
        config = super().get_config()
        config.update({"recent_window": self.recent_window, "eps": self.eps})
        return config


# ----------------------------------------------------------------------
# 3. Disease + context-conditioned gate (context = input statistics)
# ----------------------------------------------------------------------

def build_context_aware_gate(
    disease_id_input,
    input_statistics,        # (batch, stat_dim) -- from InputStatisticsLayer, NOT branch outputs
    num_diseases,
    embed_dim=16,
    extra_covariates_input=None,
    gate_hidden=32,
    num_branches=3,
    name="gate",
):
    """
    disease_id_input       : integer tensor, shape (batch,)
    input_statistics        : (batch, stat_dim) raw-sequence summary,
                               independent of the branches being weighted.
    extra_covariates_input  : optional numeric tensor (batch, k), e.g.
                               externally supplied epi covariates.

    Returns:
        gate_weights      : (batch, num_branches) softmax, sample-specific
        disease_embedding : (batch, embed_dim), kept for post-hoc analysis
    """
    embed = layers.Embedding(
        input_dim=num_diseases, output_dim=embed_dim, name=f"{name}_disease_embed"
    )(disease_id_input)
    disease_embedding = layers.Flatten(name=f"{name}_embed_flat")(embed)

    context_parts = [disease_embedding, input_statistics]
    if extra_covariates_input is not None:
        context_parts.append(extra_covariates_input)

    gate_input = layers.Concatenate(name=f"{name}_concat_context")(context_parts)

    x = layers.Dense(gate_hidden, activation="relu", name=f"{name}_hidden1")(gate_input)
    x = layers.Dense(gate_hidden // 2, activation="relu", name=f"{name}_hidden2")(x)
    gate_weights = layers.Dense(
        num_branches, activation="softmax", name=f"{name}_softmax"
    )(x)
    return gate_weights, disease_embedding


# ----------------------------------------------------------------------
# 4. Weighted fusion of branch representations (unchanged)
# ----------------------------------------------------------------------

class WeightedBranchFusion(layers.Layer):
    """Combines branch representation vectors using per-sample gate weights."""
    def call(self, branch_reprs, gate_weights):
        stacked = tf.stack(branch_reprs, axis=1)          # (batch, num_branches, feat_dim)
        weights = tf.expand_dims(gate_weights, axis=-1)   # (batch, num_branches, 1)
        fused = tf.reduce_sum(stacked * weights, axis=1)  # (batch, feat_dim)
        return fused


# ----------------------------------------------------------------------
# 5. Full model
# ----------------------------------------------------------------------

def build_dctmn(
    seq_len,
    num_features,
    num_diseases,
    forecast_horizon=1,
    embed_dim=16,
    common_dim=64,
    recent_window=3,
    use_extra_covariates=False,
    num_extra_covariates=0,
):
    seq_input = layers.Input(shape=(seq_len, num_features), name="sequence_input")
    disease_id_input = layers.Input(shape=(), dtype="int32", name="disease_id_input")

    extra_covariates_input = None
    if use_extra_covariates:
        extra_covariates_input = layers.Input(
            shape=(num_extra_covariates,), name="extra_covariates_input"
        )

    # Two independent, parallel consumers of the raw sequence:
    # (a) the three representation branches, (b) the input-statistics summary.
    tcn_repr = build_tcn_branch(seq_input)
    bilstm_repr = build_bilstm_branch(seq_input)
    transformer_repr = build_transformer_branch(seq_input)
    input_statistics_raw = InputStatisticsLayer(recent_window=recent_window)(seq_input)
    # Normalize before the gate consumes it -- raw mean/std/volatility/trend/
    # growth-rate scales differ wildly across features and across quiet vs.
    # outbreak windows. Without this, the gate's Dense layers saturate on
    # magnitude alone rather than learning disease-specific reasoning.
    input_statistics = layers.LayerNormalization(name="input_stats_norm")(input_statistics_raw)

    tcn_proj = layers.Dense(common_dim, name="tcn_proj")(tcn_repr)
    bilstm_proj = layers.Dense(common_dim, name="bilstm_proj")(bilstm_repr)
    transformer_proj = layers.Dense(common_dim, name="transformer_proj")(transformer_repr)

    # Gate depends only on disease identity + input statistics --
    # it never sees the branches' outputs.
    gate_weights, disease_embedding = build_context_aware_gate(
        disease_id_input,
        input_statistics=input_statistics,
        num_diseases=num_diseases,
        embed_dim=embed_dim,
        extra_covariates_input=extra_covariates_input,
    )

    fused = WeightedBranchFusion(name="weighted_fusion")(
        [tcn_proj, bilstm_proj, transformer_proj], gate_weights
    )

    x = layers.Dense(64, activation="relu", name="head_dense1")(fused)
    x = layers.Dropout(0.2, name="head_dropout")(x)
    forecast_output = layers.Dense(forecast_horizon, name="forecast_output")(x)

    inputs = [seq_input, disease_id_input]
    if use_extra_covariates:
        inputs.append(extra_covariates_input)

    model = models.Model(
        inputs=inputs,
        outputs={
            "forecast": forecast_output,
            "gate_weights": gate_weights,
            "disease_embedding": disease_embedding,
            "input_statistics": input_statistics,   # exposed for diagnostics too
        },
        name=MODEL_NAME,
    )
    return model


# ----------------------------------------------------------------------
# 6. Diagnostic utilities
# ----------------------------------------------------------------------

def gate_weight_entropy(gate_weights_batch, eps=1e-9):
    """Shannon entropy of gate weights per sample, in nats."""
    gw = np.clip(gate_weights_batch, eps, 1.0)
    return -np.sum(gw * np.log(gw), axis=-1)


def per_disease_gate_summary(gate_weights_batch, disease_ids, disease_names=None):
    """Mean gate weights + entropy per disease -- the headline table."""
    entropies = gate_weight_entropy(gate_weights_batch)
    summary = {}
    for d_id in np.unique(disease_ids):
        mask = disease_ids == d_id
        mean_weights = gate_weights_batch[mask].mean(axis=0)
        summary[int(d_id)] = {
            "name": disease_names.get(int(d_id), str(d_id)) if disease_names else str(d_id),
            "mean_weights": mean_weights.tolist(),
            "entropy": float(entropies[mask].mean()),
            "n": int(mask.sum()),
        }
    return summary


def gate_weight_variance_within_disease(gate_weights_batch, disease_ids):
    """
    For each disease, how much do gate weights vary across samples of
    that SAME disease? Near-zero std -> gate collapsed to a disease
    lookup table despite having context available. Meaningful std ->
    gate is genuinely responding to within-disease temporal context.
    """
    result = {}
    for d_id in np.unique(disease_ids):
        mask = disease_ids == d_id
        std_per_branch = gate_weights_batch[mask].std(axis=0)
        result[int(d_id)] = {
            "std_per_branch": std_per_branch.tolist(),
            "n": int(mask.sum()),
        }
    return result


if __name__ == "__main__":
    model = build_dctmn(seq_len=12, num_features=5, num_diseases=9, forecast_horizon=1)
    model.summary()

    # Same sanity check as v2, but now the gate genuinely never touches
    # branch outputs -- it only sees disease id + raw input statistics.
    rng = np.random.default_rng(0)
    seq_a = rng.normal(loc=0.0, scale=1.0, size=(1, 12, 5)).astype("float32")     # quiet period
    seq_b = rng.normal(loc=0.0, scale=1.0, size=(1, 12, 5)).astype("float32")
    seq_b[:, -3:, :] += 8.0                                                       # recent spike
    same_disease_id = np.array([3], dtype="int32")  # e.g. Cholera = index 3

    out_a = model.predict([seq_a, same_disease_id], verbose=0)
    out_b = model.predict([seq_b, same_disease_id], verbose=0)

    print("\nSame disease, different context (gate never sees branch outputs):")
    print("  window A gate weights:", out_a["gate_weights"][0])
    print("  window B gate weights:", out_b["gate_weights"][0])
    print("  identical?", np.allclose(out_a["gate_weights"], out_b["gate_weights"]))
    print("  window A input stats (first 5):", out_a["input_statistics"][0][:5])
    print("  window B input stats (first 5):", out_b["input_statistics"][0][:5])