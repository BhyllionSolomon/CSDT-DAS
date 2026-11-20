import api from './api'

export async function getMyFeedback(sessionId, semester) {
    return (await api.get('/feedback/mine', { params: { sessionId, semester } })).data
}
export async function submitFeedback(payload) {
    return (await api.post('/feedback/submit', payload)).data
}
export async function getFeedbackResults(sessionId, semester) {
    return (await api.get('/feedback/results', { params: { sessionId, semester } })).data
}
export async function setFeedbackWindow(sessionId, semester, open) {
    return (await api.put('/feedback/window', { sessionId, semester, open })).data
}