import api from './api'

export async function login(username, password) {
    const response = await api.post('/auth/login', { username, password })
    return response.data
}

export async function signup(idNumber, fullName, email, username, password) {
    const response = await api.post('/auth/signup', {
        idNumber, fullName, email, username, password,
    })
    return response.data
}

export async function getCurrentUser() {
    const response = await api.get('/auth/me')
    return response.data
}