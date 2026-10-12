import api from './api'

export async function login(username, password) {
    return (await api.post('/auth/login', { username, password })).data
}

export async function signup(idNumber, fullName, email, username, password) {
    return (await api.post('/auth/signup', { idNumber, fullName, email, username, password })).data
}

export async function getCurrentUser() {
    return (await api.get('/auth/me')).data
}

export async function resetPassword(identifier, email, newPassword) {
    return (await api.post('/auth/reset-password', { identifier, email, newPassword })).data
}