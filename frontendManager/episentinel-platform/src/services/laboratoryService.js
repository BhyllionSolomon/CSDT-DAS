import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8081/api/laboratory-results"
});

/*
 * Get all laboratory results
 */
export const getAllLaboratoryResults = async () => {
  const response = await API.get("");
  return response.data;
};

/*
 * Get laboratory result by ID
 */
export const getLaboratoryResult = async (id) => {
  const response = await API.get(`/${id}`);
  return response.data;
};

/*
 * Create laboratory result
 */
export const createLaboratoryResult = async (result) => {
  const response = await API.post("", result);
  return response.data;
};

/*
 * Update laboratory result
 */
export const updateLaboratoryResult = async (id, result) => {
  const response = await API.put(`/${id}`, result);
  return response.data;
};

/*
 * Delete laboratory result
 */
export const deleteLaboratoryResult = async (id) => {
  const response = await API.delete(`/${id}`);
  return response.data;
};

/*
 * Search by disease
 */
export const getLaboratoryResultsByDisease = async (disease) => {
  const response = await API.get(`/disease/${disease}`);
  return response.data;
};

/*
 * Search by state
 */
export const getLaboratoryResultsByState = async (state) => {
  const response = await API.get(`/state/${state}`);
  return response.data;
};

/*
 * Search by LGA
 */
export const getLaboratoryResultsByLga = async (lga) => {
  const response = await API.get(`/lga/${lga}`);
  return response.data;
};

/*
 * Search by laboratory status
 */
export const getLaboratoryResultsByStatus = async (status) => {
  const response = await API.get(`/status/${status}`);
  return response.data;
};

/*
 * Dashboard summary
 */
export const getLaboratoryDashboard = async () => {
  const response = await API.get("/dashboard");
  return response.data;
};