import api from './api'

export async function getAllUsers() {
    return (await api.get('/users')).data
}

export async function createUser(payload) {
    return (await api.post('/users', payload)).data
}

export async function updateUser(id, payload) {
    return (await api.put(`/users/${id}`, payload)).data
}

export async function deleteUser(id) {
    await api.delete(`/users/${id}`)
}

export async function setUserPhone(id, phoneNumber) {
    return (await api.put(`/users/${id}/phone`, { phoneNumber })).data
}

export async function resetUserPassword(id, newPassword) {
    return (await api.put(`/users/${id}/reset-password`, { newPassword })).data
}