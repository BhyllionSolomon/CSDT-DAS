import api from './api'

export async function previewSessionStart(sessionId) {
    return (await api.get('/clearance/session-start/preview', { params: { sessionId } })).data
}

export async function applySessionStart(sessionId, excludeStudentIds) {
    return (await api.post('/clearance/session-start/apply', { sessionId, excludeStudentIds })).data
}

export async function previewCorrection(fromId, toId) {
    return (await api.get('/clearance/session-start/correct/preview', { params: { fromId, toId } })).data
}

export async function applyCorrection(fromId, toId) {
    return (await api.post('/clearance/session-start/correct/apply', { fromId, toId })).data
}