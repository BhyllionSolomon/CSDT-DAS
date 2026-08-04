import api from "./api";

const DailySurveillanceApi = {

    getAll() {
        return api.get("/daily-surveillance");
    },

    getStatistics() {
        return api.get("/daily-surveillance/statistics");
    },

    getById(id) {
        return api.get(`/daily-surveillance/${id}`);
    },

    create(report) {
        return api.post("/daily-surveillance", report);
    },

    update(id, report) {
        return api.put(`/daily-surveillance/${id}`, report);
    },

    delete(id) {
        return api.delete(`/daily-surveillance/${id}`);
    }

};

export default DailySurveillanceApi;