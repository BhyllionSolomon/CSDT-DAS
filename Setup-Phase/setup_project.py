from pathlib import Path

# =====================================================
# PROJECT LOCATION
# =====================================================

PROJECT_PATH = Path(r"E:\GitHub\SolomonAi_Projects\Epidemiological-Forecaster")

# =====================================================
# PROJECT FOLDERS
# =====================================================

folders = [
    "backend",
    "frontend",
    "prediction-service",

    "database",
    "database/init",
    "database/migrations",
    "database/docker-compose",

    "datasets",
    "models",
    "docs",
    "Results",
    "scripts",
]

print("=" * 70)
print("Creating Epidemiological Forecaster Project")
print("=" * 70)

# Create the project root if it doesn't already exist
PROJECT_PATH.mkdir(parents=True, exist_ok=True)

# Create all subfolders
for folder in folders:
    path = PROJECT_PATH / folder
    path.mkdir(parents=True, exist_ok=True)
    print(f"[OK] {path}")

print("\nProject structure created successfully!")
print(f"\nLocation:\n{PROJECT_PATH}")