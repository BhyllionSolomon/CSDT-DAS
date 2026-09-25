import api from './api'

export async function getAllProgrammes() {
    const response = await api.get('/programmes')
    return response.data
}