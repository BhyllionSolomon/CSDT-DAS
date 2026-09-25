import api from './api'

export async function extractSelfResults(file) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post('/self-results/extract', formData, { headers: { 'Content-Type': undefined } })
    return response.data
}
export async function saveSelfResults(studentId, rows) {
    const response = await api.post('/self-results/save', { studentId, rows })
    return response.data
}
export async function getSelfResultAnalytics(studentId) {
    const response = await api.get(`/self-results/analytics/${studentId}`)
    return response.data
}