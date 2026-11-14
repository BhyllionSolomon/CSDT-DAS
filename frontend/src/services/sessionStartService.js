import api from './api'

export async function previewPromotion(fromId, toId) {
    return (await api.get('/promotion/preview', { params: { fromId, toId } })).data
}

export async function applyPromotion(fromId, toId, excludeStudentIds) {
    return (await api.post('/promotion/apply', { fromId, toId, excludeStudentIds })).data
}