import api from './api'

export async function getProgress(studentId) {
    return (await api.get(`/results/student/${studentId}/progress`)).data
}