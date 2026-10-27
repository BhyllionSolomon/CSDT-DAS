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

// Forgotten password: a code is emailed to the address on the account.
export async function requestPasswordReset(identifier) {
    return (await api.post('/auth/forgot-password/request', { identifier })).data
}

export async function confirmPasswordReset(identifier, code, newPassword) {
    return (await api.post('/auth/forgot-password/confirm', { identifier, code, newPassword })).data
}

// My Account (signed in)
export async function getProfile() {
    return (await api.get('/account/profile')).data
}

export async function requestChange(purpose, newValue, currentPassword) {
    return (await api.post('/account/request-change', { purpose, newValue, currentPassword })).data
}

export async function confirmChange(purpose, code, newPassword) {
    return (await api.post('/account/confirm-change', { purpose, code, newPassword })).data
}