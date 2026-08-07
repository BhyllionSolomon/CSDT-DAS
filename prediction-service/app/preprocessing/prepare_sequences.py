import joblib
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder, StandardScaler

LOOKBACK = 12


class SequencePreprocessor:

    def __init__(self):

        self.scaler = StandardScaler()
        self.encoder = LabelEncoder()

        # These features are scaled
        self.feature_columns = [
            "year",
            "epiWeek",
            "suspectedCases",
            "deaths"
        ]

    # --------------------------------------------------------

    def load_dataset(self, csv_path):

        df = pd.read_csv(csv_path)

        return df

    # --------------------------------------------------------

    def encode_disease(self, df):

        df = df.copy()

        self.encoder = LabelEncoder()

        df["disease_id"] = self.encoder.fit_transform(
            df["diseaseName"].astype(str)
        )

        print("\nDEBUG encoder classes:")
        print(self.encoder.classes_)
        print()

        return df

    # --------------------------------------------------------

    def scale_features(self, df):

        df = df.copy()

        df[self.feature_columns] = self.scaler.fit_transform(
            df[self.feature_columns]
        )

        return df

    # --------------------------------------------------------

    def create_sequences(self, df):

        X = []
        disease = []
        y = []

        for disease_name in df["diseaseName"].unique():

            subset = df[
                df["diseaseName"] == disease_name
            ].copy()

            subset = subset.sort_values(
                ["year", "epiWeek"]
            )

            # -----------------------------------
            # Build model input
            # -----------------------------------

            scaled_features = subset[
                self.feature_columns
            ].values.astype(np.float32)

            confirmed_history = subset[
                ["confirmedCases"]
            ].values.astype(np.float32)

            features = np.concatenate(
                [
                    scaled_features,
                    confirmed_history
                ],
                axis=1
            )

            # Prediction target

            target = subset[
                "confirmedCases"
            ].values.astype(np.float32)

            disease_id = subset[
                "disease_id"
            ].values

            if len(subset) <= LOOKBACK:
                continue

            for i in range(LOOKBACK, len(subset)):

                X.append(
                    features[i - LOOKBACK:i]
                )

                disease.append(
                    disease_id[i]
                )

                y.append(
                    target[i]
                )

        return (
            np.asarray(X, dtype=np.float32),
            np.asarray(disease, dtype=np.int32),
            np.asarray(y, dtype=np.float32)
        )

    # --------------------------------------------------------

    def save_objects(
        self,
        output_folder
    ):

        joblib.dump(
            self.scaler,
            f"{output_folder}/scaler.pkl"
        )

        joblib.dump(
            self.encoder,
            f"{output_folder}/label_encoder.pkl"
        )