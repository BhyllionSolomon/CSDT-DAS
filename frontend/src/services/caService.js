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