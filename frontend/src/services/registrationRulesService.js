import api from './api'

export async function getOptions(studentId, sessionId, semester) {
    return (await api.get(`/course-registrations/student/${studentId}/options`, { params: { sessionId, semester } })).data
}
export async function listDirectEntry() {
    return (await api.get('/direct-entry')).data
}
export async function setDirectEntry(studentId, entryLevel) {
    await api.put(`/direct-entry/${studentId}`, { entryLevel })
}
export async function listGrants(sessionId) {
    return (await api.get('/extra-units', { params: { sessionId } })).data
}
export async function grantExtra(payload) {
    await api.post('/extra-units', payload)
}
export async function revokeGrant(id) {
    await api.delete(`/extra-units/${id}`)
}