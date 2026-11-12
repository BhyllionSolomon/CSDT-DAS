import api from './api'

export async function getGraduates() {
    return (await api.get('/graduates')).data
}