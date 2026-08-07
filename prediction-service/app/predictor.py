import numpy as np

from app.model_loader import ModelLoader
from app.schemas import PredictionRequest
from app.schemas import PredictionResponse


class Predictor:

    def __init__(self):

        self.loader = ModelLoader()

    # --------------------------------------------------------

    @property
    def model_loaded(self):

        return self.loader.is_loaded

    # --------------------------------------------------------

    def preprocess(
            self,
            request: PredictionRequest
    ):

        timestep = [

            float(request.suspectedCases),
            float(request.confirmedCases),
            float(request.deaths),
            float(request.rainfall),
            float(request.temperature)

        ]

        sequence = np.array(
            [timestep] * 12,
            dtype=np.float32
        )

        sequence = sequence.reshape(
            1,
            12,
            5
        )

        sequence = self.loader.scale_sequence(
            sequence
        )

        disease_id = self.loader.encode_disease(
            request.disease
        )

        return sequence, disease_id

    # --------------------------------------------------------

    def risk(self, cases):

        if cases < 20:
            return (
                "LOW",
                "Continue routine surveillance."
            )

        if cases < 100:
            return (
                "MEDIUM",
                "Increase surveillance."
            )

        return (
            "HIGH",
            "Immediate outbreak response recommended."
        )

    # --------------------------------------------------------

    def predict(
            self,
            request: PredictionRequest
    ) -> PredictionResponse:

        if not self.model_loaded:

            return PredictionResponse(

                predictedCases=0.0,
                confidenceScore=0.0,
                riskLevel="MODEL NOT LOADED",
                recommendation="No trained model found.",
                forecastHorizon=request.forecastHorizon,
                modelVersion="Unavailable"

            )

        sequence, disease_id = self.preprocess(request)

        predicted_cases = self.loader.predict(

            sequence,

            disease_id

        )

        risk_level, recommendation = self.risk(
            predicted_cases
        )

        return PredictionResponse(

            predictedCases=round(
                predicted_cases,
                2
            ),

            confidenceScore=0.95,

            riskLevel=risk_level,

            recommendation=recommendation,

            forecastHorizon=request.forecastHorizon,

            modelVersion="DCTMN v1.0"

        )