import api from './api'

export async function getAllSessions() {
    const response = await api.get('/academic-sessions')
    return response.data
}

export async function getSession(id) {
    const response = await api.get(`/academic-sessions/${id}`)
    return response.data
}

export async function createSession(name, startDate, endDate) {
    const response = await api.post('/academic-sessions', { name, startDate, endDate })
    return response.data
}