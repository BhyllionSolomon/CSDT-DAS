import os
import numpy as np
import tensorflow as tf
from sklearn.model_selection import train_test_split

from app.preprocessing.prepare_sequences import SequencePreprocessor
from app.models.dctmn import build_dctmn


# -------------------------------------------------------
# Configuration
# -------------------------------------------------------

LOOKBACK = 12

from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[3]
DATASET = BASE_DIR / "datasets" / "curated_dataset.csv"
MODEL_DIR = BASE_DIR / "prediction-service" / "models"

MODEL_DIR.mkdir(parents=True, exist_ok=True)


# -------------------------------------------------------
# Dataset
# -------------------------------------------------------

print("Loading dataset...")

processor = SequencePreprocessor()

dataset = processor.load_dataset(DATASET)

print(dataset.columns.tolist())
print(dataset.head())
print(dataset["diseaseName"].head())
print(dataset["diseaseName"].dtype)

dataset = processor.encode_disease(dataset)

dataset = processor.scale_features(dataset)

X, disease, y = processor.create_sequences(dataset)

print()

print("Samples :", len(X))

print("Sequence Shape :", X.shape)

print("Targets :", y.shape)

print()


# -------------------------------------------------------
# Train Test Split
# -------------------------------------------------------

X_train, X_test, disease_train, disease_test, y_train, y_test = train_test_split(

    X,

    disease,

    y,

    test_size=0.20,

    random_state=42,

    shuffle=False

)


feature_count = X_train.shape[2]

num_diseases = len(

    processor.encoder.classes_

)

print("Diseases :", num_diseases)

print("Features :", feature_count)

print()


# -------------------------------------------------------
# Build Model
# -------------------------------------------------------

model = build_dctmn(

    num_diseases=num_diseases,

    feature_count=feature_count

)

model.summary()


model.compile(

    optimizer=tf.keras.optimizers.Adam(

        learning_rate=0.001

    ),

    loss="mse",

    metrics=[

        "mae"

    ]

)


# -------------------------------------------------------
# Callbacks
# -------------------------------------------------------

early_stop = tf.keras.callbacks.EarlyStopping(

    monitor="val_loss",

    patience=20,

    restore_best_weights=True

)

reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(

    monitor="val_loss",

    factor=0.5,

    patience=8,

    verbose=1

)

checkpoint = tf.keras.callbacks.ModelCheckpoint(

    filepath=str(MODEL_DIR / "dctmn.weights.h5"),

    monitor="val_loss",

    save_best_only=True,

    save_weights_only=True,

    verbose=1

)


# -------------------------------------------------------
# Training
# -------------------------------------------------------

print()

print("===============================")

print("Training DCTMN")

print("===============================")

print()

history = model.fit(

    {

        "sequence": X_train,

        "disease": disease_train

    },

    y_train,

    validation_split=0.20,

    epochs=200,

    batch_size=32,

    callbacks=[

        early_stop,

        reduce_lr,

        checkpoint,

        

    ],

    verbose=1

)


# -------------------------------------------------------
# Evaluation
# -------------------------------------------------------

print()

print("===============================")

print("Evaluation")

print("===============================")

print()

results = model.evaluate(

    {

        "sequence": X_test,

        "disease": disease_test

    },

    y_test,

    verbose=1

)

print()

print("Loss :", results[0])

print("MAE  :", results[1])

print()


# -------------------------------------------------------
# Save preprocessing objects
# -------------------------------------------------------
print("\nDEBUG encoder before saving:")
print(processor.encoder.classes_)
print() 

processor.save_objects(

     str(MODEL_DIR)

)

print()

print("Scaler saved.")

print("Label encoder saved.")

print()

# -------------------------------------------------------
# Final Save
# -------------------------------------------------------

model.load_weights(
    str(MODEL_DIR / "dctmn.weights.h5")
)

model.save_weights(
    str(MODEL_DIR / "dctmn_final.weights.h5")
)

print()

print("===============================")
print("Training Complete")
print("===============================")
print()

print("Saved:")
print()

print("models/dctmn.weights.h5")
print("models/dctmn_final.weights.h5")
print("models/scaler.pkl")
print("models/label_encoder.pkl")