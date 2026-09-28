import api from './api'

export async function getStudentsForSession(academicSessionId) {
    const response = await api.get(`/students/session/${academicSessionId}/history`)
    return response.data
}
export async function backfillStudentHistory() {
    const response = await api.post('/students/history/backfill')
    return response.data
}