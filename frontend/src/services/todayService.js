import api from './api'

export async function getToday() {
    return (await api.get('/academic-sessions/today')).data
}