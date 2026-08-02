"""
Heterogeneous Multi-Scale Forecasting Architecture
REPRESENTATION COMPLEMENTARITY ANALYSIS
====================================================
Research question:
  Do heterogeneous temporal encoders (TCN / Bi-LSTM / Transformer) learn
  genuinely complementary representations, or largely redundant ones,
  despite architectural differences? And does removing a branch that
  shows high similarity (redundancy) to another actually cost accuracy?

Architecture:
  Short-term branch:  TCN   (dilated causal convolutions -> local spikes)
  Medium-term branch: Bi-LSTM (sequential trend dependencies)
  Long-term branch:   Transformer encoder (global/seasonal dependencies)
  All three run in PARALLEL on the same input window, pre-fusion
  representations are pooled to a common dimension for CKA comparison,
  then concatenated and fused for the forecast + severity heads.

What this script adds on top of the existing CV harness:
  1. The three-branch model itself (full model).
  2. Three leave-one-branch-out ablations (TCN+BiLSTM, TCN+Transformer,
     BiLSTM+Transformer) using an IDENTICAL fusion mechanism, so any
     accuracy difference is attributable to the missing branch, not to
     the fusion or data pipeline.
  3. Linear CKA computed pairwise between branch representations, on
     held-out test data, per fold -- to quantify complementarity.
  4. All of the above repeated across your existing expanding-window
     folds, so results are not resting on a single test year.

Author: Solomon Korede Olagunju, KolaDaisi University / University of Ibadan
"""

import os, warnings
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_DETERMINISTIC_OPS"] = "1"
os.environ["TF_CUDNN_DETERMINISTIC"] = "1"
warnings.filterwarnings("ignore")

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error
from sklearn.utils.class_weight import compute_class_weight

# ── CONFIGURATION ──────────────────────────────────────────────────────────
DATA_PATH  = r"C:\..PhD Thesis\ncdc_wer_full.csv"
OUT_DIR    = r"C:\..PhD Thesis\CholeraPredictionResearch\results_hetero"
os.makedirs(OUT_DIR, exist_ok=True)

DISEASES   = ["AFP","CSM","Cholera","Lassa Fever","Measles","Mpox","Yellow Fever"]
N_DIS      = len(DISEASES)
WINDOW     = 8
HORIZON    = 1
N_CLASSES  = 3

REP_DIM    = 32      # common pooled representation size per branch (for CKA comparability)
DROPOUT    = 0.30
EPOCHS     = 200
BATCH      = 16
PATIENCE   = 25
CLF_WEIGHT = 0.4
REG_WEIGHT = 0.6
MIN_TRAIN_WEEKS = 100

# FINAL DECISION, not to be revisited: fold 2022 (107 training sequences,
# only 7 test sequences) showed a confirmed catastrophic divergence in
# prior runs -- the SAME seed+fold combination produced MAE=81.2 in one
# run and MAE=248.8 in another, with nothing else different. That fold is
# excluded from the final reported statistics below. It remains in the
# raw per-fold logs and CSVs for full transparency, but is not used to
# compute the headline MAE/CKA/correlation numbers, because a fold this
# unstable would silently dominate and distort any summary statistic.
# This is a one-time, documented decision -- not an ongoing filter to be
# adjusted after seeing results.
EXCLUDE_YEARS_FROM_FINAL_STATS = [2022]

# Now that the pipeline is confirmed working end to end (first pass done),
# use 3 seeds for citable, statistically defensible numbers -- matching
# the rigor already established for the LSTM/attention comparison earlier
# in this project.
SEEDS = [42, 123, 2024]

print("="*70)
print("  Heterogeneous Multi-Scale Architecture | Representation Analysis")
print("="*70)
print("\n[LOAD] Reading data ...")

df = pd.read_csv(DATA_PATH)
wide = df.pivot_table(
    index=["year","epi_week"], columns="disease",
    values="weekly_suspected", aggfunc="first"
).reset_index()
wide.columns.name = None
wide = wide.sort_values(["year","epi_week"]).reset_index(drop=True)

for d in DISEASES:
    if d not in wide.columns: wide[d] = 0.0
    wide[d] = pd.to_numeric(wide[d], errors="coerce").fillna(0.0).clip(0, 50000)

wide = wide[["year","epi_week"] + DISEASES].copy()
all_years = sorted(wide["year"].unique())
print(f"    {len(wide)} weeks total | Years available: {all_years}")

fold_test_years = []
for y in all_years:
    n_train_weeks = len(wide[wide["year"] < y])
    if n_train_weeks >= MIN_TRAIN_WEEKS:
        fold_test_years.append(y)
print(f"    Fold test years: {fold_test_years}")

# ── DATA HELPERS (identical to existing CV script) ─────────────────────────

def compute_thresholds(train_df):
    thresholds = {}
    for d in DISEASES:
        vals = train_df[d].values
        nonzero = vals[vals > 0]
        if len(nonzero) < 10:
            thresholds[d] = [0.0, 1.0]
        else:
            thresholds[d] = [np.percentile(nonzero, 33), np.percentile(nonzero, 66)]
    return thresholds

def to_severity(values, d, thresholds):
    t = thresholds[d]
    out = np.zeros(len(values), dtype=int)
    out[values > t[0]] = 1
    out[values > t[1]] = 2
    return out

def make_sequences(data_df, ws_df, window, horizon, thresholds):
    vals_s  = ws_df[DISEASES].values.astype("float32")
    vals_o  = data_df[DISEASES].values.astype("float32")
    years   = data_df["year"].values
    X_list, yr_list, yo_list = [], [], []
    yc_lists = {d: [] for d in DISEASES}
    for i in range(window, len(vals_s) - horizon + 1):
        yrs_w = years[i - window: i + horizon]
        if max(yrs_w) - min(yrs_w) > 1:
            continue
        X_list.append(vals_s[i - window: i])
        yr_list.append(vals_s[i])
        yo_list.append(vals_o[i])
        for j, d in enumerate(DISEASES):
            yc_lists[d].append(int(to_severity([vals_o[i, j]], d, thresholds)[0]))
    X   = np.array(X_list,  dtype="float32")
    yr  = np.array(yr_list, dtype="float32")
    yo  = np.array(yo_list, dtype="float32")
    yc  = {d: np.array(yc_lists[d], dtype="int32") for d in DISEASES}
    return X, yr, yc, yo

def augment(X, y_reg, y_clf, n=4, noise=0.02):
    Xa = [X]; ya = [y_reg]; yca = {d: [y_clf[d]] for d in DISEASES}
    for _ in range(n):
        Xa.append(np.clip(X + np.random.normal(0, noise, X.shape), 0, 1))
        ya.append(y_reg)
        for d in DISEASES: yca[d].append(y_clf[d])
    X2  = np.concatenate(Xa).astype("float32")
    yr2 = np.concatenate(ya).astype("float32")
    yc2 = {d: np.concatenate(yca[d]).astype("int32") for d in DISEASES}
    return X2, yr2, yc2

def one_hot(arr, n_classes=N_CLASSES):
    return keras.utils.to_categorical(arr, num_classes=n_classes).astype("float32")

def reg_metrics(yt, yp):
    mae  = mean_absolute_error(yt, yp)
    rmse = np.sqrt(mean_squared_error(yt, yp))
    return round(mae,2), round(rmse,2)

early_stop = keras.callbacks.EarlyStopping(
    monitor="val_loss", patience=PATIENCE, restore_best_weights=True, verbose=0)
reduce_lr  = keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss", factor=0.5, patience=10, verbose=0)
callbacks  = [early_stop, reduce_lr]
fit_kw = dict(validation_split=0.2, epochs=EPOCHS, batch_size=BATCH,
              callbacks=callbacks, verbose=0)

# ── BRANCH DEFINITIONS ──────────────────────────────────────────────────────
# Each branch takes the same (WINDOW, N_DIS) input and returns a pooled
# representation vector of size REP_DIM, so representations from different
# branch types are directly comparable via CKA.

def tcn_branch(inp):
    """Short-term branch: dilated causal convolutions -> local spike patterns."""
    x = inp
    for dilation in (1, 2, 4):
        x = layers.Conv1D(32, 3, padding="causal", dilation_rate=dilation,
                           activation="relu")(x)
        x = layers.BatchNormalization()(x)
    x = layers.GlobalAveragePooling1D()(x)
    rep = layers.Dense(REP_DIM, activation="relu", name="tcn_rep")(x)
    return rep

def bilstm_branch(inp):
    """Medium-term branch: sequential trend dependencies."""
    x = layers.Bidirectional(layers.LSTM(32, return_sequences=False))(inp)
    x = layers.Dropout(DROPOUT)(x)
    rep = layers.Dense(REP_DIM, activation="relu", name="bilstm_rep")(x)
    return rep

def transformer_branch(inp):
    """Long-term branch: self-attention over the full window -> global/seasonal deps."""
    # simple learned positional encoding, added to the raw input
    pos = layers.Embedding(input_dim=WINDOW, output_dim=N_DIS)(tf.range(WINDOW))
    x = inp + pos
    attn_out = layers.MultiHeadAttention(num_heads=2, key_dim=16)(x, x)
    x = layers.LayerNormalization()(x + attn_out)
    ff = layers.Dense(32, activation="relu")(x)
    ff = layers.Dense(N_DIS)(ff)
    x = layers.LayerNormalization()(x + ff)
    x = layers.GlobalAveragePooling1D()(x)
    rep = layers.Dense(REP_DIM, activation="relu", name="transformer_rep")(x)
    return rep

BRANCH_BUILDERS = {"tcn": tcn_branch, "bilstm": bilstm_branch, "transformer": transformer_branch}

def build_heterogeneous_model(branch_names):
    """
    Builds a model using only the specified branches (e.g. ["tcn","bilstm","transformer"]
    for the full model, or a 2-element subset for leave-one-out ablations).
    Returns (training_model, representation_model) — the second lets us pull
    out each branch's pooled representation for CKA analysis, without
    needing a separate forward pass or duplicated weights.
    """
    inp = layers.Input(shape=(WINDOW, N_DIS), name="input")
    branch_reps = {}
    for name in branch_names:
        branch_reps[name] = BRANCH_BUILDERS[name](inp)

    concat = layers.concatenate(list(branch_reps.values()), name="branch_concat")
    shared = layers.Dense(64, activation="relu")(concat)
    shared = layers.Dropout(DROPOUT)(shared)

    reg_outs = []
    for i, d in enumerate(DISEASES):
        rh = layers.Dense(16, activation="relu", name=f"rh_{i}")(shared)
        ro = layers.Dense(1, name=f"reg_{i}")(rh)
        reg_outs.append(ro)
    reg_concat = layers.concatenate(reg_outs, name="regression_out")

    clf_outs = []
    for i, d in enumerate(DISEASES):
        ch = layers.Dense(16, activation="relu", name=f"ch_{i}")(shared)
        co = layers.Dense(N_CLASSES, activation="softmax", name=f"clf_{i}")(ch)
        clf_outs.append(co)

    all_outs = [reg_concat] + clf_outs
    train_model = keras.Model(inp, all_outs, name="Hetero_" + "_".join(branch_names))

    losses = {"regression_out": "mse"}
    lw     = {"regression_out": REG_WEIGHT}
    for i in range(N_DIS):
        losses[f"clf_{i}"] = "categorical_crossentropy"
        lw[f"clf_{i}"]     = CLF_WEIGHT / N_DIS
    # clipnorm=1.0 added after observing catastrophic divergence on the
    # smallest fold (2022, 107 training sequences): the same seed/fold
    # combination gave MAE=81.2 in one run and MAE=248.8 in another,
    # identical in every other respect. Gradient clipping is a standard,
    # architecture-neutral stability measure -- applied here to ALL
    # variants (full + ablations) equally, so it doesn't bias the
    # comparison toward any one of them.
    train_model.compile(optimizer=keras.optimizers.Adam(1e-3, clipnorm=1.0),
                        loss=losses, loss_weights=lw)

    # Representation-extraction model — same weights, different outputs.
    rep_model = keras.Model(inp, list(branch_reps.values()), name="Hetero_reps_" + "_".join(branch_names))

    return train_model, rep_model, list(branch_reps.keys())

# ── LINEAR CKA ───────────────────────────────────────────────────────────
def linear_cka(X, Y):
    """
    Linear Centered Kernel Alignment between two representation matrices
    of shape (N_samples, dim). Returns a value in [0,1] — 1.0 means the
    two representations are (up to rotation/scaling) identical, i.e.
    fully redundant; values near 0 mean the representations capture
    largely different information, i.e. complementary.
    Reference: Kornblith et al., 2019, "Similarity of Neural Network
    Representations Revisited".
    """
    X = X - X.mean(axis=0, keepdims=True)
    Y = Y - Y.mean(axis=0, keepdims=True)
    XtX_F = np.linalg.norm(X.T @ X, ord="fro")
    YtY_F = np.linalg.norm(Y.T @ Y, ord="fro")
    XtY_F = np.linalg.norm(X.T @ Y, ord="fro")
    denom = XtX_F * YtY_F
    if denom == 0:
        return np.nan
    return float((XtY_F ** 2) / denom)

# ── MAIN LOOP ────────────────────────────────────────────────────────────
FULL_BRANCHES = ["tcn", "bilstm", "transformer"]
ABLATIONS = {
    "no_transformer": ["tcn", "bilstm"],       # tests: does Transformer add anything?
    "no_bilstm":      ["tcn", "transformer"],  # tests: does Bi-LSTM add anything?
    "no_tcn":         ["bilstm", "transformer"],# tests: does TCN add anything?
}

all_mae_rows = []     # seed, fold, variant, disease, MAE
all_cka_rows = []      # seed, fold, branch_pair, CKA
fold_meta_rows = []    # seed, fold, n_train_seq, n_test_seq

for seed_run in SEEDS:
    np.random.seed(seed_run)
    tf.random.set_seed(seed_run)
    print(f"\n{'#'*70}\n  SEED RUN: {seed_run}\n{'#'*70}")

    for fold_i, test_year in enumerate(fold_test_years, 1):
        print(f"\n{'='*70}")
        print(f"  FOLD {fold_i}/{len(fold_test_years)}  |  Train: years < {test_year}  |  Test: {test_year}")
        print(f"{'='*70}")

        train_mask = wide["year"] < test_year
        test_mask  = wide["year"] == test_year
        if test_mask.sum() < WINDOW + 5:
            print("    Skipping — too few test weeks.")
            continue

        thresholds = compute_thresholds(wide[train_mask])
        wide_log = wide.copy()
        wide_log[DISEASES] = np.log1p(wide[DISEASES])
        scaler = MinMaxScaler((0, 1))
        scaler.fit(wide_log[train_mask][DISEASES].values)
        ws = wide.copy()
        ws[DISEASES] = scaler.transform(wide_log[DISEASES].values)
        def inv(y_sc): return np.expm1(scaler.inverse_transform(y_sc))

        X_tr, y_reg_tr, y_clf_tr, _ = make_sequences(
            wide[train_mask], ws[train_mask], WINDOW, HORIZON, thresholds)
        X_te, y_reg_te, y_clf_te, y_orig_te = make_sequences(
            wide[test_mask], ws[test_mask], WINDOW, HORIZON, thresholds)

        if len(X_tr) < 30 or len(X_te) < 5:
            print("    Skipping — insufficient sequences.")
            continue
        print(f"    Train sequences: {len(X_tr)}  |  Test sequences: {len(X_te)}")

        X_tr_a, y_reg_a, y_clf_a = augment(X_tr, y_reg_tr, y_clf_tr)
        y_clf_a_oh = {d: one_hot(y_clf_a[d]) for d in DISEASES}

        class_weights_dict = {}
        for d in DISEASES:
            try:
                cw = compute_class_weight("balanced", classes=np.array([0,1,2]), y=y_clf_a[d])
                class_weights_dict[d] = dict(enumerate(cw))
            except ValueError:
                class_weights_dict[d] = {0: 1.0, 1: 1.0, 2: 1.0}

        y_tr_list = [y_reg_a] + [y_clf_a_oh[d] for d in DISEASES]
        sample_weight_list = [np.ones(len(y_reg_a), dtype="float32")]
        for d in DISEASES:
            cw = class_weights_dict[d]
            sw = np.array([cw[c] for c in y_clf_a[d]], dtype="float32")
            sample_weight_list.append(sw)

        fold_meta_rows.append(dict(seed_run=seed_run, fold_test_year=test_year,
                                    n_train_seq=len(X_tr), n_test_seq=len(X_te)))

        # ── Train FULL heterogeneous model ──────────────────────────────
        print("    Training FULL (TCN+BiLSTM+Transformer) model ...", end=" ", flush=True)
        full_model, full_rep_model, full_branch_order = build_heterogeneous_model(FULL_BRANCHES)
        full_model.fit(X_tr_a, y_tr_list, sample_weight=sample_weight_list, **fit_kw)
        full_out = full_model.predict(X_te, verbose=0)
        full_reg = inv(full_out[0])
        print("done")

        for i, d in enumerate(DISEASES):
            mae,_ = reg_metrics(y_orig_te[:,i], full_reg[:,i])
            all_mae_rows.append(dict(seed_run=seed_run, fold_test_year=test_year,
                                      variant="full", disease=d, MAE=mae))
        full_mae = np.mean([r["MAE"] for r in all_mae_rows
                             if r["seed_run"]==seed_run and r["fold_test_year"]==test_year
                             and r["variant"]=="full"])
        print(f"    FULL model aggregate MAE: {full_mae:.1f}")

        # ── CKA between branch representations ──────────────────────────
        # IMPORTANT: computed on TRAINING sequences (X_tr, real non-augmented,
        # 107-186 samples depending on fold), NOT on the test set. Early
        # folds have as few as 7 test sequences -- a sanity check confirmed
        # CKA between two INDEPENDENT random matrices at n=50 still gives
        # ~0.40 (not ~0), a known finite-sample bias that gets much worse
        # at n=7. Using the larger training set gives a statistically
        # meaningful complementarity estimate; test-set CKA at n=7 would
        # just be noise dressed up as a finding.
        reps = full_rep_model.predict(X_tr, verbose=0)  # X_tr, not X_te
        rep_dict = dict(zip(full_branch_order, reps))
        pairs = [("tcn","bilstm"), ("tcn","transformer"), ("bilstm","transformer")]
        print(f"    Pairwise CKA (branch representation similarity, n={len(X_tr)} training sequences):")
        for a, b in pairs:
            cka_val = linear_cka(rep_dict[a], rep_dict[b])
            print(f"      {a:<12} <-> {b:<12}: CKA = {cka_val:.3f}")
            all_cka_rows.append(dict(seed_run=seed_run, fold_test_year=test_year,
                                      branch_a=a, branch_b=b, CKA=round(cka_val,4),
                                      n_samples=len(X_tr)))

        # ── Leave-one-branch-out ablations ──────────────────────────────
        for ablation_name, branches in ABLATIONS.items():
            print(f"    Training ablation '{ablation_name}' ({'+'.join(branches)}) ...", end=" ", flush=True)
            ab_model, _, _ = build_heterogeneous_model(branches)
            ab_model.fit(X_tr_a, y_tr_list, sample_weight=sample_weight_list, **fit_kw)
            ab_out = ab_model.predict(X_te, verbose=0)
            ab_reg = inv(ab_out[0])
            print("done")
            for i, d in enumerate(DISEASES):
                mae,_ = reg_metrics(y_orig_te[:,i], ab_reg[:,i])
                all_mae_rows.append(dict(seed_run=seed_run, fold_test_year=test_year,
                                          variant=ablation_name, disease=d, MAE=mae))
            ab_mae = np.mean([r["MAE"] for r in all_mae_rows
                               if r["seed_run"]==seed_run and r["fold_test_year"]==test_year
                               and r["variant"]==ablation_name])
            delta = ab_mae - full_mae
            direction = "WORSE without this branch" if delta > 0 else "NO WORSE / BETTER without it"
            print(f"      {ablation_name} aggregate MAE: {ab_mae:.1f}  (delta vs full: {delta:+.1f} -> {direction})")

# ── AGGREGATE AND SAVE ──────────────────────────────────────────────────
mae_df_raw = pd.DataFrame(all_mae_rows)
cka_df_raw = pd.DataFrame(all_cka_rows)
meta_df = pd.DataFrame(fold_meta_rows)

# Save the FULL, unfiltered raw data first, for transparency.
mae_df_raw.to_csv(os.path.join(OUT_DIR, "hetero_mae_long_RAW_ALL_FOLDS.csv"), index=False)
cka_df_raw.to_csv(os.path.join(OUT_DIR, "hetero_cka_long_RAW_ALL_FOLDS.csv"), index=False)

# Apply the documented exclusion for all FINAL statistics below.
mae_df = mae_df_raw[~mae_df_raw["fold_test_year"].isin(EXCLUDE_YEARS_FROM_FINAL_STATS)].copy()
cka_df = cka_df_raw[~cka_df_raw["fold_test_year"].isin(EXCLUDE_YEARS_FROM_FINAL_STATS)].copy()

print(f"\n{'='*70}")
print("  SUMMARY: MAE by variant (mean across all seed x fold combinations)")
print(f"  [Excluding fold(s) {EXCLUDE_YEARS_FROM_FINAL_STATS} from final stats")
print(f"   due to confirmed training instability -- see raw CSVs for full data]")
print(f"{'='*70}")

variant_summary = mae_df.groupby(["seed_run","fold_test_year","variant"])["MAE"].mean().reset_index()
pivot = variant_summary.pivot_table(index=["seed_run","fold_test_year"], columns="variant", values="MAE")
print("\n" + pivot.to_string())

print("\n  MEAN +/- STD per variant, across all seed x fold combos:\n")
for v in ["full","no_tcn","no_bilstm","no_transformer"]:
    vals = variant_summary[variant_summary["variant"]==v]["MAE"].values
    if len(vals) > 0:
        print(f"    {v:<16}: {vals.mean():7.2f}  +/-  {vals.std():6.2f}  (n={len(vals)})")

print(f"\n{'='*70}")
print("  SUMMARY: Pairwise CKA (branch representation complementarity)")
print(f"{'='*70}")
print("\n  (Higher CKA = more redundant/similar; lower CKA = more complementary)\n")
cka_summary = cka_df.groupby(["branch_a","branch_b"])["CKA"].agg(["mean","std","count"])
print(cka_summary.to_string())

print("\n  Per-fold CKA detail (to check if complementarity depends on data volume):\n")
print(cka_df[["seed_run","fold_test_year","n_samples","branch_a","branch_b","CKA"]].to_string(index=False))

# ── FORMAL TEST: does a branch's overlap with the OTHER TWO predict how ──
# costly it is to remove it? This turns the qualitative pattern you can
# eyeball in the table into an actual statistic. For each branch, in each
# fold x seed, we compute (a) its MEAN CKA with the other two branches
# (higher = more redundant with what remains) and (b) the ablation cost
# of removing it (MAE_without - MAE_full). If complementarity genuinely
# predicts necessity, low mean-CKA branches should show HIGH ablation
# cost -- i.e. a NEGATIVE correlation between mean CKA and cost.
print(f"\n{'='*70}")
print("  DOES BRANCH REDUNDANCY (CKA) PREDICT ABLATION COST?")
print(f"{'='*70}")

from scipy import stats as _stats

branch_to_ablation = {"tcn": "no_tcn", "bilstm": "no_bilstm", "transformer": "no_transformer"}
full_mae_by_key = (mae_df[mae_df["variant"]=="full"]
                   .groupby(["seed_run","fold_test_year"])["MAE"].mean())

records = []
for (seed_run, fold_test_year), grp in cka_df.groupby(["seed_run","fold_test_year"]):
    cka_lookup = {}
    for _, row in grp.iterrows():
        cka_lookup[(row["branch_a"], row["branch_b"])] = row["CKA"]
        cka_lookup[(row["branch_b"], row["branch_a"])] = row["CKA"]

    full_mae = full_mae_by_key.get((seed_run, fold_test_year), np.nan)

    for branch in ["tcn", "bilstm", "transformer"]:
        others = [b for b in ["tcn","bilstm","transformer"] if b != branch]
        mean_cka = np.mean([cka_lookup[(branch, o)] for o in others])

        ablation_name = branch_to_ablation[branch]
        ab_mae = (mae_df[(mae_df["seed_run"]==seed_run) &
                          (mae_df["fold_test_year"]==fold_test_year) &
                          (mae_df["variant"]==ablation_name)]["MAE"].mean())
        cost = ab_mae - full_mae
        records.append(dict(seed_run=seed_run, fold_test_year=fold_test_year,
                             branch=branch, mean_cka_with_others=round(mean_cka,4),
                             ablation_cost=round(cost,2)))

corr_df = pd.DataFrame(records)
corr_df.to_csv(os.path.join(OUT_DIR, "hetero_cka_vs_cost.csv"), index=False)

print("\n  Per-branch mean-CKA vs. ablation-cost (all seed x fold x branch rows):\n")
print(corr_df.to_string(index=False))

valid = corr_df.dropna(subset=["mean_cka_with_others","ablation_cost"])
if len(valid) >= 4:
    pear_r, pear_p = _stats.pearsonr(valid["mean_cka_with_others"], valid["ablation_cost"])
    spear_r, spear_p = _stats.spearmanr(valid["mean_cka_with_others"], valid["ablation_cost"])
    print(f"\n  Pearson  correlation (mean CKA vs. ablation cost): r={pear_r:.3f}, p={pear_p:.4f}")
    print(f"  Spearman correlation (mean CKA vs. ablation cost): r={spear_r:.3f}, p={spear_p:.4f}")
    print(f"\n  Spearman is treated as the PRIMARY statistic here, not Pearson --")
    print(f"  Pearson is sensitive to single extreme outliers, and this exact")
    print(f"  dataset already demonstrated one (fold 2022 catastrophic")
    print(f"  divergence, now excluded from this calculation, but the general")
    print(f"  risk of Pearson being misleadingly 'significant' due to one wild")
    print(f"  point remains a live concern with only n={len(valid)} points).")
    actual_sign = "NEGATIVE" if spear_r < 0 else "POSITIVE"
    hypothesis_supported = "SUPPORTS" if spear_r < 0 else "CONTRADICTS"
    print(f"\n  ACTUAL RESULT: Spearman r is {actual_sign} ({spear_r:.3f}), which")
    print(f"  {hypothesis_supported} the original hypothesis that higher branch")
    print(f"  redundancy (CKA) predicts LOWER necessity (lower ablation cost).")
    print(f"  p={spear_p:.4f} -- {'this reaches' if spear_p < 0.05 else 'this does NOT reach'} conventional significance (0.05).")
else:
    print("\n  Not enough data points yet for a meaningful correlation test.")

mae_df.to_csv(os.path.join(OUT_DIR, "hetero_mae_long.csv"), index=False)
cka_df.to_csv(os.path.join(OUT_DIR, "hetero_cka_long.csv"), index=False)
meta_df.to_csv(os.path.join(OUT_DIR, "hetero_fold_meta.csv"), index=False)
print(f"\n  CSVs saved to: {OUT_DIR}")

# ── PLOTS ──────────────────────────────────────────────────────────────
print("\n[PLOTS] Saving analysis plots ...")

fig, ax = plt.subplots(figsize=(9,5))
variants = ["full","no_tcn","no_bilstm","no_transformer"]
labels = ["Full\n(TCN+BiLSTM+Transf)","No TCN\n(BiLSTM+Transf)",
          "No BiLSTM\n(TCN+Transf)","No Transformer\n(TCN+BiLSTM)"]
means = [variant_summary[variant_summary["variant"]==v]["MAE"].mean() for v in variants]
colors = ["#e34948","#7ab3e0","#4a8fc9","#2a78d6"]
bars = ax.bar(labels, means, color=colors, width=0.5)
for b, v in zip(bars, means):
    ax.text(b.get_x()+b.get_width()/2, b.get_height()+0.5, f"{v:.1f}",
            ha="center", va="bottom", fontsize=10)
ax.set_ylabel("Aggregate MAE (mean across diseases, folds, seeds)")
ax.set_title("Leave-One-Branch-Out Ablation:\nDoes removing a branch actually hurt accuracy?")
ax.grid(axis="y", alpha=0.3)
plt.tight_layout()
fig.savefig(os.path.join(OUT_DIR,"ablation_mae_comparison.png"), dpi=150, bbox_inches="tight")
plt.close()

fig, ax = plt.subplots(figsize=(7,5))
pair_labels = [f"{a}\n<->\n{b}" for a,b in cka_summary.index]
cka_means = cka_summary["mean"].values
cka_stds  = cka_summary["std"].values
ax.bar(pair_labels, cka_means, yerr=cka_stds, color="#2a78d6", capsize=5)
ax.axhline(0.5, color="gray", ls="--", lw=1, label="CKA = 0.5 (moderate similarity)")
ax.set_ylabel("Linear CKA (0 = complementary, 1 = redundant)")
ax.set_title("Branch Representation Similarity\n(pairwise, across all folds/seeds)")
ax.set_ylim(0, 1)
ax.legend()
ax.grid(axis="y", alpha=0.3)
plt.tight_layout()
fig.savefig(os.path.join(OUT_DIR,"branch_cka_comparison.png"), dpi=150, bbox_inches="tight")
plt.close()

# Plot 3: does redundancy (CKA) predict ablation cost? -- the key scatter
fig, ax = plt.subplots(figsize=(7,6))
branch_colors = {"tcn": "#e34948", "bilstm": "#2a78d6", "transformer": "#4a8fc9"}
for branch, sub in corr_df.groupby("branch"):
    ax.scatter(sub["mean_cka_with_others"], sub["ablation_cost"],
               label=branch, color=branch_colors.get(branch, "gray"), s=70, alpha=0.8)
if len(valid) >= 2:
    z = np.polyfit(valid["mean_cka_with_others"], valid["ablation_cost"], 1)
    xs = np.linspace(valid["mean_cka_with_others"].min(), valid["mean_cka_with_others"].max(), 50)
    ax.plot(xs, np.poly1d(z)(xs), "k--", alpha=0.5, label="linear trend")
ax.axhline(0, color="gray", lw=0.8)
ax.set_xlabel("Branch's mean CKA with the other two branches\n(higher = more redundant)")
ax.set_ylabel("Ablation cost (MAE without branch - MAE full)\n(higher = more necessary)")
ax.set_title("Does Redundancy Predict Necessity?\nEach point = one branch, one fold, one seed")
ax.legend()
ax.grid(alpha=0.3)
plt.tight_layout()
fig.savefig(os.path.join(OUT_DIR,"cka_vs_ablation_cost_scatter.png"), dpi=150, bbox_inches="tight")
plt.close()

print(f"    Plots saved to: {OUT_DIR}")
print("\n" + "="*70)
print("  DONE. Check CholeraPredictionResearch/results_hetero/")
print("="*70)