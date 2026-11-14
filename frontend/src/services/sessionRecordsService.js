import api from './api'

export async function checkRecords() { return (await api.get('/session-records/check')).data }
export async function assignSession(sessionId) { return (await api.post('/session-records/assign-session', { sessionId })).data }
export async function syncRecords() { return (await api.post('/session-records/sync')).data }
export async function estimateRecords(apply) { return (await api.post('/session-records/estimate', null, { params: { apply } })).data }
export async function undoEstimate(sessionId) { return (await api.post('/session-records/estimate/undo', null, { params: { sessionId } })).data }
export async function recordsForSession(sessionId) { return (await api.get(`/session-records/session/${sessionId}`)).data }