import api from './api'

export async function getBroadsheet(programmeId, levelId, sessionId, semester) {
    const response = await api.get('/reports/broadsheet', {
        params: { programmeId, levelId, sessionId, semester },
    })
    return response.data
}

export async function getStanding(programmeId, levelId, sessionId) {
    const response = await api.get('/reports/standing', {
        params: { programmeId, levelId, sessionId },
    })
    return response.data
}