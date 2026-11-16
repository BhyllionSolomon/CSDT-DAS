import api from './api'

export async function getSystemCheck() {
    return (await api.get('/system-check')).data
}
export async function getAllocationPreview(sessionId) {
    return (await api.get('/projects/export/preview', { params: sessionId ? { sessionId } : {} })).data
}
export async function downloadAllocationWord(sessionId) {
    const response = await api.get('/projects/export/word', { params: sessionId ? { sessionId } : {}, responseType: 'blob' })
    return response.data
}