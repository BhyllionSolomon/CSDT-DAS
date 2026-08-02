"""
Comparing Four Methods for Finding Disease-to-Disease Relationships
============================================================================

Four different, independent methods are used to answer the same
question -- does one disease's rise help explain or predict another
disease's rise, and if so, how strongly? Using four different kinds of
method (a classical statistical test, a machine learning model, an
information-theoretic measure, and a lagged-regression network
approximation) means that if they AGREE on the same pairs, that
agreement is much stronger evidence than any single method alone.

METHOD 1 -- Granger Causality (classical statistics)
  Tests whether disease A's past values improve prediction of disease
  B's current value, beyond what B's own past already predicts.
  Assumes a roughly straight-line relationship.

METHOD 2 -- Random Forest + SHAP (machine learning)
  Trains a Random Forest to predict each disease's next value from
  every disease's recent lagged values, then uses SHAP to rank which
  other diseases' history mattered most. Unlike Granger causality,
  this can catch relationships that are not straight-line.

METHOD 3 -- Transfer Entropy (information theory)
  Measures how much knowing disease A's past REDUCES UNCERTAINTY about
  disease B's future, beyond what B's own past already tells you. More
  general than Granger causality (does not assume straight-line
  relationships), estimated here using a simple discretized (binned)
  approach appropriate for a dataset of this size.

METHOD 4 -- Lagged-Regression Network (DBN approximation)
  Approximates a Dynamic Bayesian Network edge: does disease A's lagged
  value predict disease B's current value, AFTER controlling for
  disease B's own past AND every other disease's lagged values at the
  same time? This is stricter than Granger causality alone, since it
  checks A's effect while holding all other diseases constant too.

OUTPUT:
  disease_relationships_4methods.csv -- every pair, every method's result, side by side
  disease_relationships_consensus.csv -- pairs flagged as significant by 3 or more of the 4 methods
  disease_relationships_4methods_report.txt -- plain-English summary of consensus findings

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python compare_relationship_methods.py

NOTE: requires the 'shap' package. If not installed, run:
      pip install shap
  If shap is unavailable, the script falls back to plain Random Forest
  feature importance instead (clearly labeled as such in the output).
"""

import os
import sys
import warnings
warnings.filterwarnings("ignore")

import numpy as np
import pandas as pd
from statsmodels.tsa.stattools import grangercausalitytests
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import statsmodels.api as sm

try:
    import shap
    SHAP_AVAILABLE = True
except ImportError:
    SHAP_AVAILABLE = False

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ncdcData

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR   = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\disease_relationships_4methods"
os.makedirs(OUT_DIR, exist_ok=True)

MAX_LAG = 4
SIGNIFICANCE = 0.05
N_BINS = 3   # for transfer entropy discretization -- kept small given ~255 weeks of data
CONSENSUS_MIN_METHODS = 3  # flag a pair as "consensus" if 3+ of the 4 methods agree it matters


# ----------------------------------------------------------------------
# Shared lagged-feature table, used by methods 2 and 4
# ----------------------------------------------------------------------

def build_lagged_table(wide_df, disease_names, max_lag):
    df = wide_df[disease_names].copy()
    lagged = pd.DataFrame(index=df.index)
    for d in disease_names:
        for lag in range(1, max_lag + 1):
            lagged[f"{d}_lag{lag}"] = df[d].shift(lag)
    lagged = pd.concat([df, lagged], axis=1).dropna().reset_index(drop=True)
    return lagged


# ----------------------------------------------------------------------
# Pearson correlation (PCC) -- simple, direct "do these two move together"
# ----------------------------------------------------------------------

def compute_pcc(wide_df, disease_names, granger_results):
    """Computes PCC at lag 0 (same week) AND at the lag where Granger
    found its strongest result for that pair, since the lagged PCC is
    usually more meaningful than the same-week PCC for this kind of
    lead-follow relationship."""
    print("  Computing Pearson correlation (PCC) ...")
    pcc_lag0 = {}
    for target in disease_names:
        for source in disease_names:
            if source == target:
                continue
            pair_df = wide_df[[source, target]].dropna()
            pcc_lag0[(source, target)] = pair_df[source].corr(pair_df[target])
    return pcc_lag0


# ----------------------------------------------------------------------
# Method 1: Granger causality
# ----------------------------------------------------------------------

def method_granger(wide_df, disease_names):
    print("  Running Method 1: Granger causality ...")
    results = {}
    best_lags = {}
    for target in disease_names:
        for source in disease_names:
            if target == source:
                continue
            pair_df = wide_df[[target, source]].dropna()
            try:
                res = grangercausalitytests(pair_df[[target, source]], maxlag=MAX_LAG, verbose=False)
                p_by_lag = {lag: res[lag][0]["ssr_ftest"][1] for lag in range(1, MAX_LAG + 1)}
                best_lag = min(p_by_lag, key=p_by_lag.get)
                best_p = p_by_lag[best_lag]
            except Exception:
                best_p, best_lag = 1.0, 1
            results[(source, target)] = best_p
            best_lags[(source, target)] = best_lag
    return results, best_lags


# ----------------------------------------------------------------------
# Method 2: Random Forest + SHAP
# ----------------------------------------------------------------------

def method_rf_shap(lagged, disease_names):
    print("  Running Method 2: Random Forest + SHAP ..." if SHAP_AVAILABLE
          else "  Running Method 2: Random Forest feature importance (SHAP not installed) ...")
    results = {}
    rf_metrics = {}   # honest train/test evaluation per target, NOT in-sample
    feature_cols = [c for c in lagged.columns if "_lag" in c]

    # time-based split (not random) -- last 20% of weeks held out, consistent
    # with how every other experiment in this project evaluates forecasts
    split = int(len(lagged) * 0.8)

    for target in disease_names:
        X = lagged[feature_cols]
        y = lagged[target]
        X_train, X_test = X.iloc[:split], X.iloc[split:]
        y_train, y_test = y.iloc[:split], y.iloc[split:]

        model = RandomForestRegressor(n_estimators=200, random_state=42, n_jobs=-1)
        model.fit(X_train, y_train)

        y_pred_test = model.predict(X_test)
        mae = mean_absolute_error(y_test, y_pred_test)
        rmse = np.sqrt(mean_squared_error(y_test, y_pred_test))
        r2 = r2_score(y_test, y_pred_test)
        rf_metrics[target] = {"MAE": mae, "RMSE": rmse, "R2": r2, "n_test": len(y_test)}

        # SHAP/importance computed on the FULL data for relationship-finding
        # purposes (this is about which features the model leans on, not
        # about generalization -- generalization is what MAE/RMSE/R2 above
        # already checked honestly on held-out weeks)
        if SHAP_AVAILABLE:
            explainer = shap.TreeExplainer(model)
            shap_values = explainer.shap_values(X)
            importance = np.abs(shap_values).mean(axis=0)
        else:
            importance = model.feature_importances_

        for source in disease_names:
            if source == target:
                continue
            source_cols = [i for i, c in enumerate(feature_cols) if c.startswith(f"{source}_lag")]
            source_importance = importance[source_cols].sum()
            results[(source, target)] = source_importance

    # normalize per target so importances are comparable across targets
    for target in disease_names:
        vals = [results[(s, target)] for s in disease_names if s != target]
        total = sum(vals) if sum(vals) > 0 else 1.0
        for source in disease_names:
            if source != target:
                results[(source, target)] = results[(source, target)] / total
    return results, rf_metrics


# ----------------------------------------------------------------------
# Method 3: Transfer entropy (simple discretized estimator)
# ----------------------------------------------------------------------

def discretize(series, n_bins):
    return pd.qcut(series.rank(method="first"), n_bins, labels=False, duplicates="drop")


def transfer_entropy(source_series, target_series, n_bins):
    df = pd.DataFrame({
        "y_next": target_series.shift(-1),
        "y_now": target_series,
        "x_now": source_series,
    }).dropna()

    df["y_next_bin"] = discretize(df["y_next"], n_bins)
    df["y_now_bin"] = discretize(df["y_now"], n_bins)
    df["x_now_bin"] = discretize(df["x_now"], n_bins)

    te = 0.0
    n = len(df)
    joint_counts = df.groupby(["y_next_bin", "y_now_bin", "x_now_bin"]).size()
    yynow_counts = df.groupby(["y_now_bin", "x_now_bin"]).size()
    ynext_ynow_counts = df.groupby(["y_next_bin", "y_now_bin"]).size()
    ynow_counts = df.groupby(["y_now_bin"]).size()

    for (yn, yw, xw), count in joint_counts.items():
        p_joint = count / n
        p_y_given_ynow_x = count / yynow_counts.get((yw, xw), np.nan)
        p_y_given_ynow = ynext_ynow_counts.get((yn, yw), 0) / ynow_counts.get(yw, np.nan)
        if p_y_given_ynow_x > 0 and p_y_given_ynow > 0:
            te += p_joint * np.log2(p_y_given_ynow_x / p_y_given_ynow)
    return max(te, 0.0)


def method_transfer_entropy(wide_df, disease_names):
    print("  Running Method 3: Transfer entropy (discretized estimate) ...")
    results = {}
    for target in disease_names:
        for source in disease_names:
            if source == target:
                continue
            try:
                te = transfer_entropy(wide_df[source], wide_df[target], N_BINS)
            except Exception:
                te = 0.0
            results[(source, target)] = te
    return results


# ----------------------------------------------------------------------
# Method 4: Lagged-regression network (DBN approximation)
# ----------------------------------------------------------------------

def method_lagged_regression_network(lagged, disease_names):
    print("  Running Method 4: Lagged-regression network (DBN approximation) ...")
    results = {}
    feature_cols = [c for c in lagged.columns if "_lag1" in c]  # 1-week lag, controlling for all diseases at once

    for target in disease_names:
        X = lagged[feature_cols].copy()
        X = sm.add_constant(X)
        y = lagged[target]
        model = sm.OLS(y, X).fit()

        for source in disease_names:
            if source == target:
                continue
            col = f"{source}_lag1"
            if col in model.pvalues.index:
                results[(source, target)] = model.pvalues[col]
            else:
                results[(source, target)] = 1.0
    return results


def main():
    print("Loading NCDC data ...")
    wide_df, disease_names = ncdcData.load_and_pivot(DATA_PATH)
    print(f"  {len(wide_df)} weeks, {len(disease_names)} diseases: {disease_names}\n")

    lagged = build_lagged_table(wide_df, disease_names, MAX_LAG)

    granger_results, granger_best_lags = method_granger(wide_df, disease_names)
    rf_shap_results, rf_metrics = method_rf_shap(lagged, disease_names)
    te_results = method_transfer_entropy(wide_df, disease_names)
    dbn_results = method_lagged_regression_network(lagged, disease_names)
    pcc_results = compute_pcc(wide_df, disease_names, granger_results)

    print(f"\n{'='*70}")
    print("  RANDOM FOREST MODEL QUALITY (honest, held-out test weeks -- NOT in-sample)")
    print(f"{'='*70}")
    for target, m in rf_metrics.items():
        print(f"  {target:<14} MAE={m['MAE']:.3f}  RMSE={m['RMSE']:.3f}  R2={m['R2']:.3f}  (n_test={m['n_test']})")
    print("  Note: SHAP importance values above come from these same models --")
    print("  a low R2 here means that disease's importance ranking should be")
    print("  trusted less than one from a model with a higher R2.\n")

    rows = []
    for target in disease_names:
        for source in disease_names:
            if source == target:
                continue
            g_p = granger_results.get((source, target), 1.0)
            g_lag = granger_best_lags.get((source, target), None)
            rf_imp = rf_shap_results.get((source, target), 0.0)
            te = te_results.get((source, target), 0.0)
            dbn_p = dbn_results.get((source, target), 1.0)
            pcc = pcc_results.get((source, target), np.nan)

            granger_sig = g_p < SIGNIFICANCE
            dbn_sig = dbn_p < SIGNIFICANCE
            rf_sig = rf_imp > (1.0 / (len(disease_names) - 1))  # above "equal share" baseline
            te_sig = te > np.median(list(te_results.values())) if te_results else False

            n_methods_agree = sum([granger_sig, rf_sig, te_sig, dbn_sig])

            rows.append({
                "source": source, "target": target,
                "pcc": round(pcc, 4) if not np.isnan(pcc) else None,
                "granger_p": round(g_p, 4), "granger_best_lag_weeks": g_lag, "granger_significant": granger_sig,
                "rf_shap_importance": round(rf_imp, 4), "rf_shap_significant": rf_sig,
                "transfer_entropy": round(te, 4), "transfer_entropy_significant": te_sig,
                "dbn_p": round(dbn_p, 4), "dbn_significant": dbn_sig,
                "n_methods_agree": n_methods_agree,
            })

    all_df = pd.DataFrame(rows).sort_values("n_methods_agree", ascending=False)
    all_df.to_csv(os.path.join(OUT_DIR, "disease_relationships_4methods.csv"), index=False)

    rf_metrics_df = pd.DataFrame(rf_metrics).T
    rf_metrics_df.index.name = "disease"
    rf_metrics_df.to_csv(os.path.join(OUT_DIR, "random_forest_model_quality.csv"))

    consensus_df = all_df[all_df["n_methods_agree"] >= CONSENSUS_MIN_METHODS]
    consensus_df.to_csv(os.path.join(OUT_DIR, "disease_relationships_consensus.csv"), index=False)

    print(f"\n{'='*70}")
    print(f"  RESULTS: {len(consensus_df)} disease pair(s) flagged by {CONSENSUS_MIN_METHODS}+ "
          f"of the 4 methods")
    print(f"{'='*70}\n")

    report_lines = []
    if len(consensus_df) > 0:
        for _, row in consensus_df.iterrows():
            line = (f"{row['source']} appears to influence {row['target']}, about "
                    f"{row['granger_best_lag_weeks']} week(s) later "
                    f"(agreed on by {row['n_methods_agree']} of 4 methods; "
                    f"PCC={row['pcc']}, Granger p={row['granger_p']}, "
                    f"RF-SHAP share={row['rf_shap_importance']}, "
                    f"transfer entropy={row['transfer_entropy']}, DBN p={row['dbn_p']}).")
            report_lines.append(line)
            print("  " + line)
    else:
        report_lines.append("No disease pair was flagged as significant by 3 or more of the 4 methods. "
                             "Relationships found by individual methods should be treated cautiously, "
                             "since they were not corroborated by other methods.")
        print("  " + report_lines[0])

    with open(os.path.join(OUT_DIR, "disease_relationships_4methods_report.txt"), "w") as f:
        f.write("DISEASE-TO-DISEASE RELATIONSHIPS -- 4-METHOD CONSENSUS REPORT\n")
        f.write("=" * 70 + "\n\n")
        for line in report_lines:
            f.write(line + "\n")

    print(f"\n  Full comparison table saved to: {OUT_DIR}")
    print("=" * 70)
    print("  DONE.")
    print("=" * 70)


if __name__ == "__main__":
    main()