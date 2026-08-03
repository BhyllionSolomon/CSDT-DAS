import pandas as pd
import numpy as np

from sklearn.preprocessing import MinMaxScaler

from config import (
    DATASET_FILE,
    LOOKBACK_WINDOW,
    FORECAST_HORIZON
)


class DataPreprocessor:

    def __init__(self):

        self.scaler = MinMaxScaler()

    def load_dataset(self):

        df = pd.read_csv(DATASET_FILE)

        return df

    def filter_cholera(self, df):

        return df[
            df["diseaseName"].str.lower() == "cholera"
        ].copy()

    def sort_time(self, df):

        return df.sort_values(
            by=["year", "epiWeek"]
        ).reset_index(drop=True)

    def prepare_series(self, df):

        series = df["confirmedCases"].values.astype(float)

        scaled = self.scaler.fit_transform(
            series.reshape(-1, 1)
        )

        return scaled.flatten()

    def create_sequences(self, series):

        X = []
        y = []

        total_window = (
                LOOKBACK_WINDOW +
                FORECAST_HORIZON
        )

        for i in range(
                len(series) - total_window + 1
        ):

            X.append(
                series[
                    i:
                    i + LOOKBACK_WINDOW
                ]
            )

            y.append(
                series[
                    i + LOOKBACK_WINDOW:
                    i + total_window
                ]
            )

        X = np.array(X)

        y = np.array(y)

        X = X.reshape(
            (
                X.shape[0],
                X.shape[1],
                1
            )
        )

        return X, y

    def preprocess(self):

        df = self.load_dataset()

        df = self.filter_cholera(df)

        df = self.sort_time(df)

        series = self.prepare_series(df)

        X, y = self.create_sequences(series)

        return X, y, self.scaler


if __name__ == "__main__":

    processor = DataPreprocessor()

    X, y, scaler = processor.preprocess()

    print("--------------------------------")

    print("Dataset Loaded Successfully")

    print("--------------------------------")

    print("Input Shape :", X.shape)

    print("Target Shape:", y.shape)