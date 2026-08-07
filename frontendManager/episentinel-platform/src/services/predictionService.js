import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8081/api"
});

export const generateForecast = async ({
  disease,
  country = "Nigeria",
  state,
  lga,
  forecastHorizon,
  suspectedCases,
  confirmedCases,
  deaths,
  rainfall,
  temperature,
  humidity
}) => {
  const response = await API.post("/predictions", {
    disease,
    country,
    state,
    lga,
    year: new Date().getFullYear(),
    epiWeek: 1,
    forecastHorizon,
    suspectedCases,
    confirmedCases,
    deaths,
    rainfall,
    temperature,
    humidity
  });

  return response.data;
};