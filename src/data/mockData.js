// Curated mock data modeled on NCDC Weekly Epidemiological Report structure.
// Diseases tracked reflect the 9-disease surveillance panel used in production datasets.

export const DISEASES = [
  'Cholera', 'Lassa Fever', 'Measles', 'Meningitis', 'Yellow Fever',
  'Monkeypox', 'Diphtheria', 'Cerebrospinal Meningitis', 'Acute Flaccid Paralysis',
]

export const STATES = [
  { name: 'Lagos', risk: 'high', cases: 412, forecast: 468, lat: 6.5244, lng: 3.3792 },
  { name: 'Kano', risk: 'critical', cases: 589, forecast: 702, lat: 12.0022, lng: 8.5920 },
  { name: 'Oyo', risk: 'medium', cases: 231, forecast: 245, lat: 8.1574, lng: 3.6147 },
  { name: 'Rivers', risk: 'high', cases: 356, forecast: 401, lat: 4.8156, lng: 7.0498 },
  { name: 'Borno', risk: 'critical', cases: 498, forecast: 610, lat: 11.8333, lng: 13.1500 },
  { name: 'Kaduna', risk: 'medium', cases: 187, forecast: 201, lat: 10.5105, lng: 7.4165 },
  { name: 'Enugu', risk: 'low', cases: 64, forecast: 60, lat: 6.5244, lng: 7.5086 },
  { name: 'Sokoto', risk: 'high', cases: 302, forecast: 340, lat: 13.0059, lng: 5.2476 },
  { name: 'Anambra', risk: 'low', cases: 58, forecast: 55, lat: 6.2209, lng: 7.0716 },
  { name: 'Plateau', risk: 'medium', cases: 145, forecast: 158, lat: 9.2182, lng: 9.5179 },
  { name: 'Cross River', risk: 'low', cases: 41, forecast: 38, lat: 5.9631, lng: 8.3320 },
  { name: 'Benue', risk: 'medium', cases: 168, forecast: 176, lat: 7.3369, lng: 8.7404 },
]

export const LGAS_BY_STATE = {
  Lagos: ['Ikeja', 'Eti-Osa', 'Alimosho', 'Surulere', 'Kosofe'],
  Kano: ['Nassarawa', 'Fagge', 'Dala', 'Gwale', 'Tarauni'],
  Oyo: ['Ibadan North', 'Ibadan South-West', 'Egbeda', 'Ona Ara', 'Akinyele'],
  Rivers: ['Port Harcourt', 'Obio-Akpor', 'Eleme', 'Okrika', 'Ikwerre'],
}

export const KPIS = [
  { label: 'Total Cases (YTD)', value: 24689, delta: 4.2, direction: 'up', unit: '', icon: 'Activity' },
  { label: 'Active Outbreaks', value: 7, delta: 1, direction: 'up', unit: '', icon: 'Flame' },
  { label: 'High-Risk LGAs', value: 18, delta: 2, direction: 'up', unit: '', icon: 'MapPinned' },
  { label: 'Lab Confirmed', value: 9142, delta: 1.8, direction: 'down', unit: '', icon: 'FlaskConical' },
  { label: 'Deaths', value: 312, delta: 0.6, direction: 'down', unit: '', icon: 'HeartCrack' },
  { label: 'Recovered', value: 21044, delta: 3.1, direction: 'up', unit: '', icon: 'ShieldCheck' },
]

export const DAILY_TREND = Array.from({ length: 30 }, (_, i) => {
  const day = i + 1
  const base = 180 + Math.sin(i / 3) * 60 + i * 2.2
  return {
    day: `Aug ${day}`,
    cases: Math.round(base + (Math.random() * 20 - 10)),
    confirmed: Math.round(base * 0.62 + (Math.random() * 10 - 5)),
    deaths: Math.round(base * 0.015),
  }
})

export const WEEKLY_TREND = Array.from({ length: 12 }, (_, i) => ({
  week: `W${i + 1}`,
  cases: Math.round(900 + Math.sin(i / 2) * 260 + i * 18),
  forecast: Math.round(920 + Math.sin(i / 2) * 260 + i * 24),
}))

export const MONTHLY_TREND = ['Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'].map((m, i) => ({
  month: m,
  cases: Math.round(3200 + i * 420 + Math.sin(i) * 300),
}))

export const DISEASE_DISTRIBUTION = [
  { name: 'Cholera', value: 32, color: '#3A9EFF' },
  { name: 'Lassa Fever', value: 21, color: '#22D3C7' },
  { name: 'Measles', value: 16, color: '#F59E0B' },
  { name: 'Meningitis', value: 12, color: '#F87171' },
  { name: 'Yellow Fever', value: 9, color: '#A78BFA' },
  { name: 'Others', value: 10, color: '#94A3B8' },
]

export const FORECAST_CURVE = Array.from({ length: 16 }, (_, i) => {
  const observed = i < 10
  const val = 240 + Math.sin(i / 2.2) * 90 + i * 14
  return {
    period: `W${i + 1}`,
    actual: observed ? Math.round(val) : null,
    predicted: Math.round(val * (observed ? 1 : 1.05)),
    lower: Math.round(val * 0.82),
    upper: Math.round(val * 1.2),
  }
})

export const DISEASE_RECORDS = Array.from({ length: 48 }, (_, i) => {
  const disease = DISEASES[i % DISEASES.length]
  const state = STATES[i % STATES.length]
  const statusPool = ['Confirmed', 'Suspected', 'Under Investigation', 'Ruled Out']
  return {
    id: `NCDC-${2026000 + i}`,
    disease,
    state: state.name,
    lga: (LGAS_BY_STATE[state.name] || ['Central'])[i % 5] || 'Central',
    reportedDate: new Date(2026, 6, (i % 28) + 1).toISOString().slice(0, 10),
    status: statusPool[i % statusPool.length],
    cases: 4 + (i % 40),
    deaths: i % 7 === 0 ? 1 : 0,
    officer: ['O. Adeyemi', 'F. Musa', 'C. Nwosu', 'B. Yusuf'][i % 4],
  }
})

export const MODEL_STATUS = {
  current: {
    name: 'DCTMN-v3.2 (TCN + Bi-LSTM + Transformer)',
    accuracy: 91.4,
    mae: 12.6,
    rmse: 18.9,
    trainedOn: '2026-07-28',
    status: 'Deployed',
  },
  previous: {
    name: 'DCTMN-v3.1',
    accuracy: 88.7,
    mae: 14.8,
    rmse: 21.4,
    trainedOn: '2026-05-12',
    status: 'Archived',
  },
  trainingRuns: Array.from({ length: 8 }, (_, i) => ({
    epoch: (i + 1) * 10,
    trainLoss: +(0.62 - i * 0.06 + Math.random() * 0.02).toFixed(3),
    valLoss: +(0.68 - i * 0.055 + Math.random() * 0.02).toFixed(3),
  })),
}

export const DATASET_HISTORY = Array.from({ length: 10 }, (_, i) => ({
  id: `DS-${2026}-${String(40 - i).padStart(2, '0')}`,
  name: `NCDC WER Merge — Week ${40 - i}`,
  uploadedBy: ['O. Adeyemi', 'F. Musa', 'System (Auto-Scrape)'][i % 3],
  date: new Date(2026, 6, 30 - i * 3).toISOString().slice(0, 10),
  rows: 2394 - i * 12,
  valid: 2394 - i * 12 - (i * 3),
  invalid: i * 3,
  status: i === 0 ? 'Validated' : i % 4 === 0 ? 'Needs Review' : 'Validated',
}))

export const NOTIFICATIONS = [
  { id: 1, type: 'forecast', title: 'Forecast ready', desc: 'Cholera 4-week forecast for Kano generated.', time: '6m ago' },
  { id: 2, type: 'outbreak', title: 'Outbreak detected', desc: 'Anomalous case cluster in Borno — Konduga LGA.', time: '32m ago' },
  { id: 3, type: 'model', title: 'Model training completed', desc: 'DCTMN-v3.2 finished training — accuracy 91.4%.', time: '2h ago' },
  { id: 4, type: 'data', title: 'Data imported', desc: 'WER Week 40 dataset imported — 2,394 rows.', time: '5h ago' },
  { id: 5, type: 'validation', title: 'Validation completed', desc: 'Dataset DS-2026-40 passed schema validation.', time: '1d ago' },
]

export const RECOMMENDATIONS = [
  { title: 'Deploy Mass Drug Administration', priority: 'High', target: 'Kano State — Cholera', rationale: 'Forecast exceeds outbreak threshold within 3 weeks.' },
  { title: 'Accelerate Vaccination Campaign', priority: 'High', target: 'Borno State — Measles', rationale: 'Coverage gap identified in displaced-persons camps.' },
  { title: 'Reinforce Laboratory Capacity', priority: 'Medium', target: 'Sokoto State', rationale: 'Sample backlog exceeds 72-hour turnaround target.' },
  { title: 'Deploy Rapid Surveillance Team', priority: 'High', target: 'Borno — Konduga LGA', rationale: 'Anomalous cluster flagged by anomaly-detection model.' },
  { title: 'Increase Chlorine Distribution', priority: 'Medium', target: 'Rivers State — flood-affected wards', rationale: 'Water contamination risk factor elevated.' },
  { title: 'Launch Public Awareness Campaign', priority: 'Low', target: 'Oyo State', rationale: 'Community reporting delay above 48-hour target.' },
  { title: 'Hospital Preparedness Review', priority: 'Medium', target: 'Lagos State', rationale: 'Bed-occupancy forecast approaching 80% threshold.' },
]
