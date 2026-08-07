from pydantic import BaseModel
from typing import Optional


class PredictionRequest(BaseModel):

    disease: str

    country: str

    state: str

    lga: str

    year: int

    epiWeek: int

    forecastHorizon: int

    suspectedCases: int

    confirmedCases: int

    deaths: int

    rainfall: Optional[float] = 0.0

    temperature: Optional[float] = 0.0

    humidity: Optional[float] = 0.0


class PredictionResponse(BaseModel):

    predictedCases: float

    confidenceScore: float

    riskLevel: str

    recommendation: str

    forecastHorizon: int

    modelVersion: str