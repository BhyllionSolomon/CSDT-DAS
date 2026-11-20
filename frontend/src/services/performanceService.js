import api from './api'

export async function getLecturerPerformance(sessionId, semester) {
    return (await api.get('/lecturer-performance', { params: { sessionId, semester } })).data
}