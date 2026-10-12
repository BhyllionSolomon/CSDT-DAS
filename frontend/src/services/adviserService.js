import api from './api'

export async function assignAdviser(userId, programmeId, levelId) {
    return (await api.put(`/advisers/${userId}/assign`, { programmeId, levelId })).data
}

export async function removeAdviser(userId, reason) {
    return (await api.put(`/advisers/${userId}/remove`, { reason })).data
}

export async function getAdviserHistory() {
    return (await api.get('/advisers/history')).data
}

export async function getMyAdviser() {
    const response = await api.get('/advisers/mine')
    return response.status === 204 ? null : response.data
}