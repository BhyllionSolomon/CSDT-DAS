"""
Stationarity Check (Augmented Dickey-Fuller Test)
============================================================================

Checks whether each disease's weekly case-count series is stationary
(no trend or seasonal drift) before trusting Granger causality results
built on it. Granger causality assumes stationarity; testing this
directly, rather than assuming it, closes a real gap flagged in peer
review.

For any disease whose series is NOT stationary (p >= 0.05), this script
also differences the series (this week minus last week) and re-tests,
since differencing is the standard fix.

USAGE:
  Run from the NCDC-CKA-Architecture folder:
      python check_stationarity.py

OUTPUT: prints a plain result per disease, and saves a CSV.
"""

import os
import sys
import pandas as pd
from statsmodels.tsa.stattools import adfuller

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ncdcData

DATA_PATH = r"C:\..PhD Thesis\CholeraPredictionResearch\Data\ncdc_wer_full.csv"
OUT_DIR = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project\Results\stationarity_check"
os.makedirs(OUT_DIR, exist_ok=True)


def main():
    wide_df, disease_names = ncdcData.load_and_pivot(DATA_PATH)
    rows = []

    print("=" * 70)
    print("  STATIONARITY CHECK (Augmented Dickey-Fuller test)")
    print("=" * 70 + "\n")

    for d in disease_names:
        series = wide_df[d].dropna()
        result = adfuller(series)
        p_value = result[1]
        stationary = p_value < 0.05

        diff_series = series.diff().dropna()
        diff_result = adfuller(diff_series)
        diff_p = diff_result[1]
        diff_stationary = diff_p < 0.05

        verdict = ("STATIONARY as-is" if stationary else
                   "NOT stationary as-is, but stationary after differencing" if diff_stationary else
                   "NOT stationary even after differencing -- needs further attention")

        print(f"  {d:<14} raw p={p_value:.4f}  diff p={diff_p:.4f}  -> {verdict}")
        rows.append({
            "disease": d, "raw_adf_p": p_value, "raw_stationary": stationary,
            "diff_adf_p": diff_p, "diff_stationary": diff_stationary, "verdict": verdict,
        })

    df = pd.DataFrame(rows)
    df.to_csv(os.path.join(OUT_DIR, "stationarity_results.csv"), index=False)
    print(f"\n  Saved to: {OUT_DIR}")


if __name__ == "__main__":
    main()