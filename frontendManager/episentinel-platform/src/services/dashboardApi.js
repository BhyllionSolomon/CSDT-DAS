const API = "http://localhost:8081/api/dashboard";

export async function getDashboardSummary() {
    const response = await fetch(`${API}/overview`);

    if (!response.ok) {
        throw new Error("Failed to load dashboard summary");
    }

    return await response.json();
}

export async function getDiseaseDistribution() {
    const response = await fetch(`${API}/disease-distribution`);

    if (!response.ok) {
        throw new Error("Failed to load disease distribution");
    }

    return await response.json();
}

export async function getStateDistribution() {
    const response = await fetch(`${API}/state-distribution`);

    if (!response.ok) {
        throw new Error("Failed to load state distribution");
    }

    return await response.json();
}

export async function getWeeklyTrend() {
    const response = await fetch(`${API}/weekly-trend`);

    if (!response.ok) {
        throw new Error("Failed to load weekly trend");
    }

    return await response.json();
}

export async function getPredictionSummary() {
    const response = await fetch(`${API}/prediction-summary`);

    if (!response.ok) {
        throw new Error("Failed to load prediction summary");
    }

    return await response.json();
}