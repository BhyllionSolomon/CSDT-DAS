import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8081/api/disease-records"
});

/*
 * Get all disease records
 */
export const getAllDiseaseRecords = async () => {
  const response = await API.get("");
  return response.data;
};

/*
 * Get one disease record
 */
export const getDiseaseRecord = async (id) => {
  const response = await API.get(`/${id}`);
  return response.data;
};

/*
 * Create disease record
 */
export const createDiseaseRecord = async (record) => {
  const response = await API.post("", record);
  return response.data;
};

/*
 * Update disease record
 */
export const updateDiseaseRecord = async (id, record) => {
  const response = await API.put(`/${id}`, record);
  return response.data;
};

/*
 * Delete disease record
 */
export const deleteDiseaseRecord = async (id) => {
  const response = await API.delete(`/${id}`);
  return response.data;
};

/*
 * Search by disease
 */
export const getDiseaseByName = async (disease) => {
  const response = await API.get(`/disease/${disease}`);
  return response.data;
};

/*
 * Search by state
 */
export const getDiseaseByState = async (state) => {
  const response = await API.get(`/state/${state}`);
  return response.data;
};

/*
 * Search by LGA
 */
export const getDiseaseByLga = async (lga) => {
  const response = await API.get(`/lga/${lga}`);
  return response.data;
};

/*
 * Dashboard statistics
 */
export const getDashboardStatistics = async () => {
  const response = await API.get("/dashboard");
  return response.data;
};