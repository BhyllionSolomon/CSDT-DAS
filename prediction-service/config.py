from pathlib import Path

# ---------------------------------------------------
# Project Paths
# ---------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent

DATASET_DIR = BASE_DIR / "datasets"

MODEL_DIR = BASE_DIR / "models"

MODEL_DIR.mkdir(exist_ok=True)
DATASET_DIR.mkdir(exist_ok=True)

# ---------------------------------------------------
# Dataset
# ---------------------------------------------------

DATASET_FILE = DATASET_DIR / "curated_dataset.csv"

# ---------------------------------------------------
# Model
# ---------------------------------------------------

MODEL_FILE = MODEL_DIR / "cholera_model.keras"

# ---------------------------------------------------
# Forecast Configuration
# ---------------------------------------------------

LOOKBACK_WINDOW = 12

FORECAST_HORIZON = 8

# ---------------------------------------------------
# Training Configuration
# ---------------------------------------------------

BATCH_SIZE = 32

EPOCHS = 100

LEARNING_RATE = 0.001

VALIDATION_SPLIT = 0.2

RANDOM_SEED = 42