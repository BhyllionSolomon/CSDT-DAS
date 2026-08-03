import numpy as np


class CholeraPredictor:

    def __init__(self):
        self.model = None

    def load_model(self):
        """
        Placeholder.

        Later we will load:

        models/cholera_model.keras
        """

        self.model = "MODEL_NOT_YET_TRAINED"

    def predict(self, request):

        weeks = request.forecastWeeks

        # Temporary dummy prediction
        # Later replaced by TensorFlow inference

        prediction = np.random.randint(
            low=5,
            high=80,
            size=weeks
        )

        return prediction.tolist()