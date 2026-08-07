import os
import joblib
import pandas as pd
from sklearn.model_selection import train_test_split

from app.models.random_forest_model import RandomForestModel


LOOKBACK = 12


def create_sequences(df):

    X = []
    y = []

    values = df["confirmedCases"].values

    for i in range(LOOKBACK, len(values)):

        X.append(values[i - LOOKBACK:i])
        y.append(values[i])

    return X, y


def train_disease(disease_name):

    dataset = pd.read_csv(
        "../datasets/curated_dataset.csv"
    )

    disease = dataset[
        dataset["diseaseName"] == disease_name
    ].copy()

    disease = disease.sort_values(
        ["year", "epiWeek"]
    )

    X, y = create_sequences(disease)

    if len(X) < 30:

        print(
            f"{disease_name}: Not enough data."
        )

        return

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        shuffle=False
    )

    model = RandomForestModel()

    model.train(
        X_train,
        y_train
    )

    metrics = model.evaluate(
        X_test,
        y_test
    )

    os.makedirs(
        "../models",
        exist_ok=True
    )

    filename = disease_name.lower()

    filename = filename.replace(" ", "_")

    filename = filename.replace("-", "_")

    model.save(
        f"../models/{filename}_random_forest.pkl"
    )

    print("--------------------------------")

    print(disease_name)

    print(metrics)

    print("Saved.")

    print("--------------------------------")


if __name__ == "__main__":

    dataset = pd.read_csv(
        "../datasets/curated_dataset.csv"
    )

    diseases = sorted(
        dataset["diseaseName"].unique()
    )

    for disease in diseases:

        train_disease(disease)