import api from './api'

export async function getActivationStatus(programmeId, levelId) {
    return (await api.get('/activation-codes/status', { params: { programmeId, levelId } })).data
}

export async function issueActivationCodes(programmeId, levelId, regenerate) {
    return (await api.post('/activation-codes/issue', { programmeId, levelId, regenerate })).data
}