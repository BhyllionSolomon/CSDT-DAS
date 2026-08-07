from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.schemas import PredictionRequest, PredictionResponse
from app.predictor import Predictor

app = FastAPI(
    title="EpiSentinel Prediction Service",
    version="1.0.0"
)

# -----------------------------------------
# Enable CORS
# -----------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# -----------------------------------------
# Load Predictor
# -----------------------------------------
predictor = Predictor()

# -----------------------------------------
# Root Endpoint
# -----------------------------------------
@app.get("/")
def root():
    return {
        "service": "EpiSentinel AI Prediction Service",
        "status": "running",
        "version": "1.0.0"
    }

# -----------------------------------------
# Health Check
# -----------------------------------------
@app.get("/health")
def health():
    return {
        "status": "UP",
        "modelLoaded": predictor.model_loaded
    }

# -----------------------------------------
# Prediction Endpoint
# -----------------------------------------
@app.post(
    "/predict",
    response_model=PredictionResponse
)
def predict(request: PredictionRequest):

    return predictor.predict(request)