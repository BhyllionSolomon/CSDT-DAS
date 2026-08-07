import api from "./api";

const analyticsService = {

    async getSummary() {
        return await api.get("/api/analytics/summary");
    },

    async getRiskMap() {
        return await api.get("/api/analytics/risk-map");
    },

    async getHotspots() {
        return await api.get("/api/analytics/hotspots");
    },

    async getForecastOverview() {
        return await api.get("/api/analytics/forecast");
    }

};

export default analyticsService;