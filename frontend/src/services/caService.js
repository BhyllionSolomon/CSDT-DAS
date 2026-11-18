import api from './api'

export async function getCaCourses(sessionId, semester) {
    return (await api.get('/ca/courses', { params: { sessionId, semester } })).data
}
export async function getCaSheet(courseId, sessionId, semester) {
    return (await api.get('/ca/sheet', { params: { courseId, sessionId, semester } })).data
}
export async function createCaItem(payload) {
    return (await api.post('/ca/items', payload)).data
}
export async function updateCaItem(id, payload) {
    return (await api.put(`/ca/items/${id}`, payload)).data
}
export async function deleteCaItem(id) {
    await api.delete(`/ca/items/${id}`)
}
export async function saveCaScores(itemId, entries) {
    await api.put(`/ca/items/${itemId}/scores`, entries)
}
export async function aggregateCa(payload) {
    return (await api.post('/ca/aggregate', payload)).data
}
export async function getCaSubmissions(itemId) {
    return (await api.get(`/ca/items/${itemId}/submissions`)).data
}
export async function gradeCaSubmission(id, payload) {
    await api.put(`/ca/submissions/${id}/grade`, payload)
}
export async function downloadCaSubmission(id) {
    const response = await api.get(`/ca/submissions/${id}/file`, { responseType: 'blob' })
    return response.data
}
export async function submitCaFile(itemId, file) {
    const body = new FormData()
    body.append('file', file)
    return (await api.post(`/ca/items/${itemId}/submissions`, body, {
        headers: { 'Content-Type': 'multipart/form-data' },
    })).data
}
export async function getMyCa(sessionId, semester) {
    return (await api.get('/academic-sessions/ca/mine', { params: { sessionId, semester } })).data
}

export const CA_CATEGORIES = [
    ['PRACTICAL', 'Practical'],
    ['ASSIGNMENT', 'Assignment'],
    ['PRESENTATION', 'Class presentation'],
    ['TEST', 'Test'],
]
export const caLabel = (code) => (CA_CATEGORIES.find(([c]) => c === code) || [code, code])[1]
