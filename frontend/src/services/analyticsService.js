import api from './api'

export async function getPerformance(sessionId, programmeId, levelId) {
    const params = { sessionId }
    if (programmeId) params.programmeId = programmeId
    if (levelId) params.levelId = levelId
    return (await api.get('/analytics/performance', { params })).data
}