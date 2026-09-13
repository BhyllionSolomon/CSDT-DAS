import api from './api'

export async function getStudentResults(studentId) {
    const response = await api.get(`/results/student/${studentId}`)
    return response.data
}

export async function getStudentResultsForSession(studentId, sessionId) {
    const response = await api.get(
        `/results/student/${studentId}/session/${sessionId}`
    )
    return response.data
}

export async function calculateSemesterResult(studentId, sessionId, semester) {
    const response = await api.get(
        `/results/student/${studentId}/session/${sessionId}/semester/${semester}/calculate`
    )
    return response.data
}

export async function calculateFullHistory(studentId) {
    const response = await api.get(`/results/student/${studentId}/history`)
    return response.data
}

export async function approveResult(resultId) {
    const response = await api.put(`/results/${resultId}/approve`)
    return response.data
}

export async function rejectResult(resultId) {
    const response = await api.put(`/results/${resultId}/reject`)
    return response.data
}
export async function createResult(payload) {
    const response = await api.post('/results', payload)
    return response.data
}