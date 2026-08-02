import subprocess
import os

project = r"C:\..PhD Thesis\CholeraPredictionResearch\NCDC-Project"

commands = [
    "git init",
    "git add .",
    'git commit -m "Initial commit"',
    "git branch -M main",
    "git remote add origin https://github.com/BhyllionSolomon/SolomonAi_Projects.git",
    "git push -u origin main"
]

os.chdir(project)

for cmd in commands:
    print(f"\nRunning: {cmd}")
    subprocess.run(cmd, shell=True)