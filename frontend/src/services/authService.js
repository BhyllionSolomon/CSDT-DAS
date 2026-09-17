import api from './api'

export async function login(username, password) {
    const response = await api.post('/auth/login', { username, password })
    return response.data
}