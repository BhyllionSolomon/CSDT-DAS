from fastapi import FastAPI
from pydantic import BaseModel
from model import CholeraPredictor

app = FastAPI(
    title="Epidemiological Forecast API",
    version="1.0"
)

predictor = CholeraPredictor()
predictor.load_model()


class ForecastRequest(BaseModel):

    diseaseName: str
    country: str
    state: str
    lga: str
    forecastWeeks: int


@app.get("/")
def home():

    return {
        "service": "Prediction API",
        "status": "Running"
    }


@app.post("/predict")
def predict(request: ForecastRequest):

    prediction = predictor.predict(request)

    return {
        "predictions": prediction
    }