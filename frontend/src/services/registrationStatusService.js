import api from './api'

export async function getRegistrationStatus(programmeId, levelId, academicSessionId, semester) {
    const response = await api.get(
        `/course-registrations/status/programme/${programmeId}/level/${levelId}/session/${academicSessionId}/semester/${semester}`
    )
    return response.data
}