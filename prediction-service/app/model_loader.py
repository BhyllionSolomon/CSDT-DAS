import os
import joblib
import numpy as np
import tensorflow as tf

from app.models.dctmn import build_dctmn


class ModelLoader:

    def __init__(self):

        self.model = None
        self.scaler = None
        self.label_encoder = None

        BASE_DIR = os.path.abspath(
            os.path.join(
                os.path.dirname(__file__),
                ".."
            )
        )

        self.model_path = os.path.join(
            BASE_DIR,
            "models",
            "dctmn.weights.h5"
        )

        self.scaler_path = os.path.join(
            BASE_DIR,
            "models",
            "scaler.pkl"
        )

        self.encoder_path = os.path.join(
            BASE_DIR,
            "models",
            "label_encoder.pkl"
        )

        self.load_objects()
        self.load_model()

    # --------------------------------------------------------

    def load_objects(self):

        if os.path.exists(self.scaler_path):

            self.scaler = joblib.load(
                self.scaler_path
            )

            print()
            print("================================")
            print("Scaler Loaded")
            print("================================")
            print(self.scaler_path)
            print("================================")
            print()

        else:

            print()
            print("WARNING: scaler.pkl not found.")
            print(self.scaler_path)
            print()

        if os.path.exists(self.encoder_path):

            self.label_encoder = joblib.load(
                self.encoder_path
            )

            print()
            print("================================")
            print("Label Encoder Loaded")
            print("================================")
            print(self.encoder_path)
            print()
            print("Encoder Classes:")
            print(self.label_encoder.classes_)
            print("================================")
            print()

        else:

            print()
            print("WARNING: label_encoder.pkl not found.")
            print(self.encoder_path)
            print()

    # --------------------------------------------------------

    def load_model(self):

        if self.label_encoder is None:

            print("Cannot build model.")
            print("Label encoder must be loaded first.")
            return

        if not os.path.exists(self.model_path):

            print("WARNING: model weights not found.")
            print(self.model_path)
            return

        try:

            num_diseases = len(
                self.label_encoder.classes_
            )

            feature_count = 5

            self.model = build_dctmn(

                num_diseases=num_diseases,

                feature_count=feature_count

            )

            self.model.load_weights(
                self.model_path
            )

            print()
            print("================================")
            print("DCTMN Model Loaded Successfully")
            print("================================")
            print(self.model_path)
            print("================================")
            print()

        except Exception as e:

            print()
            print("Failed to load DCTMN")
            print(e)
            print()

            self.model = None

    # --------------------------------------------------------

    @property
    def is_loaded(self):

        return self.model is not None

    # --------------------------------------------------------

    def encode_disease(self, disease_name):

        if self.label_encoder is None:

            raise RuntimeError(
                "Label encoder not loaded."
            )

        return int(

            self.label_encoder.transform(
                [str(disease_name)]
            )[0]

        )

    # --------------------------------------------------------

    def scale_sequence(self, sequence):

        if self.scaler is None:

            return sequence

        shape = sequence.shape

        sequence = sequence.reshape(

            -1,

            shape[-1]

        )

        sequence = self.scaler.transform(
            sequence
        )

        sequence = sequence.reshape(
            shape
        )

        return sequence.astype(
            np.float32
        )

    # --------------------------------------------------------

    def predict(
        self,
        sequence,
        disease_id
    ):

        if self.model is None:

            raise RuntimeError(
                "Prediction model is not loaded."
            )

        sequence = np.asarray(
            sequence,
            dtype=np.float32
        )

        disease_id = np.asarray(
            [disease_id],
            dtype=np.int32
        )

        prediction = self.model.predict(

            {
                "sequence": sequence,
                "disease": disease_id
            },

            verbose=0

        )

        return float(
            prediction.flatten()[0]
        )