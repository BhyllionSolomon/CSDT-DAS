from pathlib import Path

# ==========================================================
# CHANGE THIS ONLY IF YOUR PROJECT IS IN A DIFFERENT LOCATION
# ==========================================================

ROOT = Path(
    r"E:\GitHub\SolomonAi_Projects\Epidemiological-Forecaster\backend\backend"
)

JAVA_PACKAGE = ROOT / "src" / "main" / "java" / "com" / "solomon" / "epiforecaster" / "backend"

# ==========================================================
# PACKAGES TO CREATE
# ==========================================================

packages = [
    "config",
    "controller",
    "service",
    "repository",
    "entity",
    "dto",
    "mapper",
    "exception",
    "util",
    "ai"
]

print("=" * 60)
print("Creating Spring Boot package structure...")
print("=" * 60)

if not JAVA_PACKAGE.exists():
    print(f"\nERROR:")
    print(f"{JAVA_PACKAGE}")
    print("\nwas not found.")
    print("Check that your backend project was created correctly.")
    exit()

for package in packages:
    folder = JAVA_PACKAGE / package
    folder.mkdir(parents=True, exist_ok=True)

    # create an empty .gitkeep so Git tracks empty folders
    (folder / ".gitkeep").touch(exist_ok=True)

    print(f"[OK] {folder}")

print("\n" + "=" * 60)
print("Backend package structure created successfully!")
print("=" * 60)