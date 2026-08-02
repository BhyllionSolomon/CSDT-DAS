"""
NCDC Data Glue for DCTMN
=========================

Turns a long-format weekly NCDC surveillance CSV into the
(sequence, disease_id, target, target_year) tuples DCTMN needs, using
an expanding-window-by-year fold scheme consistent with your Cka.py
setup (train: years < Y, test: year Y).

IMPORTANT DESIGN CHOICE -- read this before running:

Your original Cka.py architecture is a MULTI-OUTPUT model: one shared
multivariate window in, forecasts for all diseases out at once (that's
why mae_long.csv has one row per disease per fold -- those are output
dimensions of one model, not separate training examples).

DCTMN is a CONDITIONED model: it needs "which disease am I forecasting
right now" as an explicit input. To make that comparable, this glue
reformulates the task as:

    for every valid week window:
        for every disease d:
            X  = the SAME shared multivariate window (all diseases'
                 case counts as features, last `seq_len` weeks)
            disease_id = id(d)
            y  = disease d's case count `horizon` week(s) ahead

So one window produces `num_diseases` DCTMN training examples instead
of one multi-output example. This is a deliberate reformulation of the
task, not an attempt to reproduce Cka.py's exact sequence counts --
expect different n_train_seq / n_test_seq than your original log, and
that's correct, not a bug. What must stay identical to keep the
baseline comparison fair is the FOLD BOUNDARIES (train: years < Y,
test: year Y) and which raw weeks feed which fold.

Expected input CSV: long format, one row per (year, week, disease),
e.g.:

    year, week, disease, cases
    2019, 1,    Cholera, 12
    2019, 1,    Measles, 4
    ...

Adjust COLUMN NAMES below to match your actual file if they differ.
"""

import os
import tempfile

import numpy as np
import pandas as pd


# ----------------------------------------------------------------------
# 0. Column name configuration -- EDIT to match your actual CSV
# ----------------------------------------------------------------------

YEAR_COL = "year"
WEEK_COL = "epi_week"
DISEASE_COL = "disease"
VALUE_COL = "weekly_suspected"

# COVID-19 and Diphtheria have real reports for only ~57-59 of the ~372
# weeks other diseases cover (~15% coverage). Forward-filling that many
# gaps manufactures long fabricated-looking flat stretches. Decide
# explicitly rather than silently including them -- default here is to
# EXCLUDE them; set to [] to include everything instead.
EXCLUDE_DISEASES = ["COVID-19", "Diphtheria"]

# Case counts are extremely right-skewed (Mpox mean ~16, COVID-19 mean
# ~24,000, single-week values up to 507,006). log1p compresses this
# before any scaling/model input -- standard practice for count-based
# surveillance data. This is a fixed, monotonic transform (no fold
# leakage), unlike the z-score step below which IS fit per-fold.
LOG_TRANSFORM = True


# ----------------------------------------------------------------------
# 1. Load + pivot to wide (weeks x diseases)
# ----------------------------------------------------------------------

def load_and_pivot(csv_path):
    """
    Reads the long-format CSV and pivots to one row per (year, week),
    one column per disease, sorted chronologically.

    Returns:
        wide_df: DataFrame indexed by RangeIndex (chronological order),
                 with columns [YEAR_COL, WEEK_COL, <disease_1>, ..., <disease_n>]
        disease_names: sorted list of disease column names
    """
    df = pd.read_csv(csv_path)

    if EXCLUDE_DISEASES:
        n_before = len(df)
        df = df[~df[DISEASE_COL].isin(EXCLUDE_DISEASES)]
        print(f"    Excluded {EXCLUDE_DISEASES} ({n_before - len(df)} rows) -- "
              f"insufficient weekly coverage, see EXCLUDE_DISEASES note.")

    df = df.sort_values([YEAR_COL, WEEK_COL])

    wide = df.pivot_table(
        index=[YEAR_COL, WEEK_COL], columns=DISEASE_COL, values=VALUE_COL, aggfunc="first"
    )
    wide = wide.sort_index().reset_index()

    disease_names = sorted([c for c in wide.columns if c not in (YEAR_COL, WEEK_COL)])

    # Forward-fill then zero-fill any remaining gaps -- surveillance data
    # commonly has missing weeks per disease; adjust this policy if you
    # have a better-justified imputation already established elsewhere
    # (you mentioned MICE imputation for the cholera platform -- reuse
    # that here instead of this fallback if available).
    wide[disease_names] = wide[disease_names].ffill().fillna(0.0)

    if LOG_TRANSFORM:
        wide[disease_names] = np.log1p(wide[disease_names])

    return wide, disease_names


# ----------------------------------------------------------------------
# 2. Disease id mapping (fixed, reproducible across folds)
# ----------------------------------------------------------------------

def build_disease_id_map(disease_names):
    """Alphabetical, stable mapping -- same ids used in every fold."""
    return {name: idx for idx, name in enumerate(sorted(disease_names))}


# ----------------------------------------------------------------------
# 3. Build (window, disease_id, target, target_year) examples
# ----------------------------------------------------------------------

def build_examples(wide_df, disease_names, disease_id_map, seq_len=12, horizon=1):
    """
    Slides a window of length `seq_len` over the full chronological
    series. For each valid window end index t, creates one example per
    disease:
        X          = wide_df[disease_names].iloc[t-seq_len+1 : t+1].values
                     shape (seq_len, num_diseases)
        disease_id = disease_id_map[d]
        y          = wide_df[d].iloc[t + horizon]
        target_year= wide_df[YEAR_COL].iloc[t + horizon]

    Returns:
        X_all          : (n_examples, seq_len, num_diseases) float32
        disease_id_all : (n_examples,) int32
        y_all          : (n_examples,) float32
        target_year_all: (n_examples,) int32
    """
    feature_matrix = wide_df[disease_names].values.astype("float32")
    years = wide_df[YEAR_COL].values.astype("int32")
    n_weeks = feature_matrix.shape[0]

    X_list, disease_id_list, y_list, target_year_list = [], [], [], []

    last_valid_t = n_weeks - horizon - 1
    for t in range(seq_len - 1, last_valid_t + 1):
        window = feature_matrix[t - seq_len + 1: t + 1]     # (seq_len, num_diseases)
        target_idx = t + horizon
        target_year = years[target_idx]

        for d in disease_names:
            d_id = disease_id_map[d]
            y_val = wide_df[d].iloc[target_idx]
            X_list.append(window)
            disease_id_list.append(d_id)
            y_list.append(y_val)
            target_year_list.append(target_year)

    X_all = np.stack(X_list).astype("float32")
    disease_id_all = np.array(disease_id_list, dtype="int32")
    y_all = np.array(y_list, dtype="float32")
    target_year_all = np.array(target_year_list, dtype="int32")

    return X_all, disease_id_all, y_all, target_year_all


def fit_scaler(X_train):
    """
    Per-feature (per-disease-column) mean/std, computed ONLY from
    training data -- fit fresh for every fold, never on test.
    X_train: (n, seq_len, num_features)
    Returns mean, std: (num_features,)
    """
    flat = X_train.reshape(-1, X_train.shape[-1])
    mean = flat.mean(axis=0)
    std = flat.std(axis=0) + 1e-6
    return mean, std


def apply_scaler_to_X(X, mean, std):
    return (X - mean) / std


def apply_scaler_to_y(y, disease_id, mean, std):
    """y and disease_id share axis 0; each y is scaled by ITS OWN
    disease's mean/std (disease_id indexes into the same feature axis
    as X, since wide_df columns and disease_id_map are both alphabetical)."""
    return (y - mean[disease_id]) / std[disease_id]


def inverse_scale_y(y_scaled, disease_id, mean, std):
    return y_scaled * std[disease_id] + mean[disease_id]


# ----------------------------------------------------------------------
# 4. Expanding-window fold split (matches Cka.py: train < Y, test == Y)
# ----------------------------------------------------------------------

def expanding_window_folds(X_all, disease_id_all, y_all, target_year_all,
                            test_years, min_test_examples=1):
    """
    Yields one dict per fold:
        {
            "test_year": Y,
            "X_train", "disease_id_train", "y_train",
            "X_test",  "disease_id_test",  "y_test",
        }

    Folds with fewer than `min_test_examples` test rows are skipped and
    reported, mirroring your Cka.py "too few test weeks" fold-5 skip.
    """
    folds = []
    for test_year in test_years:
        train_mask = target_year_all < test_year
        test_mask = target_year_all == test_year

        n_test = int(test_mask.sum())
        if n_test < min_test_examples:
            print(f"  [SKIP] test_year={test_year}: only {n_test} test examples, too few.")
            continue

        X_train_fold = X_all[train_mask]
        disease_id_train_fold = disease_id_all[train_mask]
        y_train_fold = y_all[train_mask]
        X_test_fold = X_all[test_mask]
        disease_id_test_fold = disease_id_all[test_mask]
        y_test_fold = y_all[test_mask]

        # Fit scaler on TRAIN ONLY for this fold, apply to both splits.
        # This must be re-fit per fold (not once globally) -- otherwise
        # future-fold statistics leak into earlier folds' training data.
        scaler_mean, scaler_std = fit_scaler(X_train_fold)

        folds.append({
            "test_year": test_year,
            "X_train": apply_scaler_to_X(X_train_fold, scaler_mean, scaler_std),
            "disease_id_train": disease_id_train_fold,
            "y_train": apply_scaler_to_y(y_train_fold, disease_id_train_fold, scaler_mean, scaler_std),
            "X_test": apply_scaler_to_X(X_test_fold, scaler_mean, scaler_std),
            "disease_id_test": disease_id_test_fold,
            "y_test": apply_scaler_to_y(y_test_fold, disease_id_test_fold, scaler_mean, scaler_std),
            "scaler_mean": scaler_mean,   # keep for inverse_scale_y on predictions
            "scaler_std": scaler_std,
        })
        print(f"  Fold test_year={test_year}: train={train_mask.sum()}, test={n_test}")

    return folds


# ----------------------------------------------------------------------
# 5. Full pipeline convenience wrapper
# ----------------------------------------------------------------------

def build_dctmn_folds(csv_path, test_years, seq_len=12, horizon=1, min_test_examples=1):
    """
    One-call pipeline: CSV path -> list of fold dicts ready for
    build_dctmn(...).fit(...).

    Also returns disease_id_map and disease_names so you can map
    predictions/gate-weight diagnostics back to disease names later
    (per_disease_gate_summary needs disease_ids as ints + this map
    inverted for the `disease_names` argument).
    """
    print("[LOAD] Reading and pivoting NCDC data ...")
    wide_df, disease_names = load_and_pivot(csv_path)
    print(f"    {len(wide_df)} weeks total | {len(disease_names)} diseases: {disease_names}")

    disease_id_map = build_disease_id_map(disease_names)

    print("[WINDOW] Building (window, disease_id, target) examples ...")
    X_all, disease_id_all, y_all, target_year_all = build_examples(
        wide_df, disease_names, disease_id_map, seq_len=seq_len, horizon=horizon
    )
    print(f"    {X_all.shape[0]} total examples "
          f"({X_all.shape[0] // len(disease_names)} windows x {len(disease_names)} diseases)")

    print("[FOLDS] Expanding-window split ...")
    folds = expanding_window_folds(
        X_all, disease_id_all, y_all, target_year_all,
        test_years=test_years, min_test_examples=min_test_examples,
    )

    return folds, disease_id_map, disease_names


# ----------------------------------------------------------------------
# Demo / smoke test with synthetic data shaped like your NCDC setup
# ----------------------------------------------------------------------

if __name__ == "__main__":
    rng = np.random.default_rng(0)

    # Synthetic long-format data: 2019-2026, ~52 weeks/year (2026 partial),
    # 7 diseases matching your mae_long.csv disease names.
    diseases = ["AFP", "CSM", "Cholera", "Lassa Fever", "Measles", "Mpox", "Yellow Fever"]
    rows = []
    for year in range(2019, 2027):
        n_weeks_this_year = 52 if year < 2026 else 8  # partial final year, like your data
        for week in range(1, n_weeks_this_year + 1):
            for d in diseases:
                rows.append({
                    YEAR_COL: year,
                    WEEK_COL: week,
                    DISEASE_COL: d,
                    VALUE_COL: max(0, rng.poisson(20) + (10 if d == "Cholera" and week % 13 == 0 else 0)),
                })
    # Saved next to this script, in a "data" subfolder -- not the OS temp
    # directory -- so it's easy to find and easy to replace with your
    # real NCDC CSV later.
    data_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")
    os.makedirs(data_dir, exist_ok=True)
    synthetic_csv = os.path.join(data_dir, "synthetic_ncdc_long.csv")
    pd.DataFrame(rows).to_csv(synthetic_csv, index=False)
    print(f"[DEMO] Synthetic CSV written to: {synthetic_csv}")

    folds, disease_id_map, disease_names = build_dctmn_folds(
        synthetic_csv,
        test_years=[2022, 2023, 2024, 2025, 2026],
        seq_len=12,
        horizon=1,
    )

    print("\nDisease id map:", disease_id_map)
    print("\nFirst fold shapes:")
    f0 = folds[0]
    print("  X_train:", f0["X_train"].shape, "disease_id_train:", f0["disease_id_train"].shape,
          "y_train:", f0["y_train"].shape)
    print("  X_test :", f0["X_test"].shape)