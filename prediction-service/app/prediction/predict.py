import numpy as np

from app.model_loader import ModelLoader
from app.schemas import PredictionRequest
from app.schemas import PredictionResponse


LOOKBACK = 12


class Predictor:

    def __init__(self):

        self.loader = ModelLoader()

        self.model_loaded = self.loader.is_loaded

    # ---------------------------------------------------------

    def preprocess(
            self,
            request: PredictionRequest
    ):

        """
        Builds one LOOKBACK-length sequence exactly
        matching the trained model.

        NOTE:
        During training the model used 5 numerical features.
        We therefore recreate the same 5-feature timestep.

        Current feature order:

        1 suspectedCases
        2 confirmedCases
        3 deaths
        4 rainfall
        5 temperature

        The same timestep is repeated LOOKBACK times because
        the API currently receives only the latest week's data.
        """

        timestep = np.array(

            [

                float(request.suspectedCases),

                float(request.confirmedCases),

                float(request.deaths),

                float(request.rainfall),

                float(request.temperature)

            ],

            dtype=np.float32

        )

        sequence = np.tile(

            timestep,

            (LOOKBACK, 1)

        )

        sequence = sequence.reshape(

            1,

            LOOKBACK,

            5

        )

        sequence = self.loader.scale_sequence(

            sequence

        )

        disease_id = self.loader.encode_disease(

            request.disease

        )

        return sequence, disease_id

    # ---------------------------------------------------------

    def infer(

            self,

            sequence,

            disease_id

    ):

        prediction = self.loader.predict(

            sequence,

            disease_id

        )

        return float(prediction)

    # ---------------------------------------------------------

    def risk(

            self,

            predicted_cases

    ):

        if predicted_cases < 20:

            return (

                "LOW",

                "Continue routine surveillance."

            )

        elif predicted_cases < 100:

            return (

                "MEDIUM",

                "Increase surveillance and monitor trends."

            )

        else:

            return (

                "HIGH",

                "Immediate outbreak response recommended."

            )

    # ---------------------------------------------------------

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

        try:

            sequence, disease_id = self.preprocess(

                request

            )

            predicted_cases = self.infer(

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

        except Exception as e:

            return PredictionResponse(

                predictedCases=0.0,

                confidenceScore=0.0,

                riskLevel="ERROR",

                recommendation=str(e),

                forecastHorizon=request.forecastHorizon,

                modelVersion="DCTMN v1.0"

            )