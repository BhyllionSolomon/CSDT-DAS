import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8081/api/case-investigations"
});

/*
 * Get all case investigations
 */
export const getAllCaseInvestigations = async () => {
  const response = await API.get("");
  return response.data;
};

/*
 * Get one case investigation
 */
export const getCaseInvestigation = async (id) => {
  const response = await API.get(`/${id}`);
  return response.data;
};

/*
 * Create case investigation
 */
export const createCaseInvestigation = async (investigation) => {
  const response = await API.post("", investigation);
  return response.data;
};

/*
 * Update case investigation
 */
export const updateCaseInvestigation = async (id, investigation) => {
  const response = await API.put(`/${id}`, investigation);
  return response.data;
};

/*
 * Delete case investigation
 */
export const deleteCaseInvestigation = async (id) => {
  const response = await API.delete(`/${id}`);
  return response.data;
};

/*
 * Search by disease
 */
export const getCaseInvestigationsByDisease = async (disease) => {
  const response = await API.get(`/disease/${disease}`);
  return response.data;
};

/*
 * Search by state
 */
export const getCaseInvestigationsByState = async (state) => {
  const response = await API.get(`/state/${state}`);
  return response.data;
};

/*
 * Search by LGA
 */
export const getCaseInvestigationsByLga = async (lga) => {
  const response = await API.get(`/lga/${lga}`);
  return response.data;
};

/*
 * Search by investigation status
 */
export const getCaseInvestigationsByStatus = async (status) => {
  const response = await API.get(`/status/${status}`);
  return response.data;
};

/*
 * Dashboard summary
 */
export const getCaseInvestigationDashboard = async () => {
  const response = await API.get("/dashboard");
  return response.data;
};