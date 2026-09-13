import api from './api'

export async function getAllSessions() {
    const response = await api.get('/academic-sessions')
    return response.data
}

export async function getSession(id) {
    const response = await api.get(`/academic-sessions/${id}`)
    return response.data
}