import api from './api'
export async function configureExamTasks(sessionId, semester, tasks) { return (await api.post(`/exams/cycles/${sessionId}/${semester}/tasks`, tasks)).data }
export async function getExamBoard(cycleId) { return (await api.get(`/exams/cycles/${cycleId}`)).data }
export async function appointExamCommittee(cycleId, userId) { await api.post(`/exams/cycles/${cycleId}/committee/${userId}`) }
export async function uploadExamPaper(taskId, file) { const body = new FormData(); body.append('file', file); return (await api.post(`/exams/tasks/${taskId}/papers`, body, { headers: { 'Content-Type': 'multipart/form-data' } })).data }
export async function getExamPapers(taskId) { return (await api.get(`/exams/tasks/${taskId}/papers`)).data }
export async function reviewExamPaper(paperId, payload) { await api.put(`/exams/papers/${paperId}/review`, payload) }
export async function downloadExamPaper(paperId) { return (await api.get(`/exams/papers/${paperId}/file`, { responseType: 'blob' })).data }
