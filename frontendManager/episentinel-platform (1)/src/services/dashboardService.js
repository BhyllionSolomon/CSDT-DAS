import api from './api'
import { getStateCoordinates } from '../data/stateCoordinates'

/* ------------------------------------------------------------------ *
 * ASSUMED BACKEND CONTRACTS
 * ------------------------------------------------------------------
 * These are the request/response shapes this file was written against.
 * They are assumptions, not confirmed contracts — see the chat message
 * for the exact list of things to verify against your Spring Boot DTOs.
 * If your real response differs, only the "adapt*" functions below need
 * to change — nothing in Dashboard.jsx or any chart component does.
 * ------------------------------------------------------------------ */

// GET /dashboard
// {
//   "totalCases":      { "value": 24689, "deltaPercent": 4.2, "trend": "up" },
//   "activeOutbreaks": { "value": 7,     "deltaPercent": 1,   "trend": "up" },
//   "highRiskLgas":    { "value": 18,    "deltaPercent": 2,   "trend": "up" },
//   "labConfirmed":    { "value": 9142,  "deltaPercent": 1.8, "trend": "down" },
//   "deaths":          { "value": 312,   "deltaPercent": 0.6, "trend": "down" },
//   "recovered":       { "value": 21044, "deltaPercent": 3.1, "trend": "up" }
// }
export async function fetchDashboardSummary() {
  const { data } = await api.get('/dashboard')
  return data
}

// GET /dashboard/disease-distribution
// [ { "disease": "Cholera", "percentage": 32 }, ... ]
export async function fetchDiseaseDistribution() {
  const { data } = await api.get('/dashboard/disease-distribution')
  return data
}

// GET /dashboard/state-distribution
// [ { "state": "Lagos", "activeCases": 412, "forecastCases": 468, "riskLevel": "HIGH" }, ... ]
export async function fetchStateDistribution() {
  const { data } = await api.get('/dashboard/state-distribution')
  return data
}

// GET /dashboard/weekly-trend
// [ { "week": "W1", "cases": 900, "forecastCases": 920 }, ... ]
// NOTE: not wired into the UI yet — see chat message, this doesn't have the
// fields the "Daily Case Trend" chart needs (daily granularity + lab-confirmed).
export async function fetchWeeklyTrend() {
  const { data } = await api.get('/dashboard/weekly-trend')
  return data
}

// GET /dashboard/prediction-summary
// {
//   "compositeRiskScore": 68,
//   "riskTier": "ELEVATED",
//   "topActions": [ { "title": "...", "priority": "High", "target": "..." }, ... ]
// }
export async function fetchPredictionSummary() {
  const { data } = await api.get('/dashboard/prediction-summary')
  return data
}

/* ------------------------------------------------------------------ *
 * ADAPTERS — reshape backend JSON into the exact props the existing
 * Dashboard.jsx / KpiCard / DiseasePie / NigeriaMap / RiskGauge expect.
 * ------------------------------------------------------------------ */

// Order + labels + icons are UI decisions and are preserved exactly as
// they were in mockData.js. Only the numbers come from the backend.
const KPI_META = [
  { key: 'totalCases', label: 'Total Cases (YTD)', icon: 'Activity' },
  { key: 'activeOutbreaks', label: 'Active Outbreaks', icon: 'Flame' },
  { key: 'highRiskLgas', label: 'High-Risk LGAs', icon: 'MapPinned' },
  { key: 'labConfirmed', label: 'Lab Confirmed', icon: 'FlaskConical' },
  { key: 'deaths', label: 'Deaths', icon: 'HeartCrack' },
  { key: 'recovered', label: 'Recovered', icon: 'ShieldCheck' },
]

export function adaptKpis(summary) {
  if (!summary) return []
  return KPI_META.map((meta) => {
    const metric = summary[meta.key]
    if (!metric) {
      console.warn(`[dashboardService] /dashboard response is missing "${meta.key}" — rendering 0.`)
    }
    return {
      label: meta.label,
      icon: meta.icon,
      value: metric?.value ?? 0,
      delta: metric?.deltaPercent ?? 0,
      direction: metric?.trend ?? 'up',
    }
  })
}

// Fixed palette so slice colors stay stable regardless of API ordering.
// (Color is a design decision, not something the backend should own.)
const PIE_PALETTE = ['#3A9EFF', '#22D3C7', '#F59E0B', '#F87171', '#A78BFA', '#94A3B8', '#34D399', '#FB923C']

export function adaptDiseaseDistribution(rows) {
  if (!Array.isArray(rows)) return []
  return rows.map((r, i) => ({
    name: r.disease,
    value: r.percentage,
    color: PIE_PALETTE[i % PIE_PALETTE.length],
  }))
}

const RISK_LEVEL_MAP = { CRITICAL: 'critical', HIGH: 'high', MEDIUM: 'medium', LOW: 'low' }

export function adaptStateDistribution(rows) {
  if (!Array.isArray(rows)) return []
  return rows.map((r) => {
    const coords = getStateCoordinates(r.state)
    if (!coords) {
      console.warn(`[dashboardService] No lat/lng registered for state "${r.state}" — it won't be plotted on the map. Add it to src/data/stateCoordinates.js.`)
    }
    return {
      name: r.state,
      cases: r.activeCases ?? 0,
      forecast: r.forecastCases ?? 0,
      risk: RISK_LEVEL_MAP[r.riskLevel?.toUpperCase()] || 'low',
      lat: coords?.lat ?? 0,
      lng: coords?.lng ?? 0,
    }
  })
}

export function adaptPredictionSummary(summary) {
  if (!summary) return { riskScore: 0, topActions: [] }
  return {
    riskScore: summary.compositeRiskScore ?? 0,
    topActions: Array.isArray(summary.topActions) ? summary.topActions : [],
  }
}

/**
 * Fetches everything the Dashboard page needs and returns it already
 * shaped for the existing components. Calls run in parallel; if one
 * endpoint fails the others still resolve (each field falls back to
 * an empty/zeroed shape and logs a console error via the api interceptor).
 */
export async function loadDashboardData() {
  const [summaryRes, diseaseRes, stateRes, predictionRes] = await Promise.allSettled([
    fetchDashboardSummary(),
    fetchDiseaseDistribution(),
    fetchStateDistribution(),
    fetchPredictionSummary(),
  ])

  return {
    kpis: adaptKpis(summaryRes.status === 'fulfilled' ? summaryRes.value : null),
    diseaseDistribution: adaptDiseaseDistribution(diseaseRes.status === 'fulfilled' ? diseaseRes.value : []),
    states: adaptStateDistribution(stateRes.status === 'fulfilled' ? stateRes.value : []),
    prediction: adaptPredictionSummary(predictionRes.status === 'fulfilled' ? predictionRes.value : null),
    errors: {
      summary: summaryRes.status === 'rejected' ? summaryRes.reason : null,
      diseaseDistribution: diseaseRes.status === 'rejected' ? diseaseRes.reason : null,
      states: stateRes.status === 'rejected' ? stateRes.reason : null,
      prediction: predictionRes.status === 'rejected' ? predictionRes.reason : null,
    },
  }
}
