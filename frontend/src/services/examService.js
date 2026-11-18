import api from './api'
export async function configureExamTasks(sessionId, semester, tasks) { return (await api.post(`/exams/cycles/${sessionId}/${semester}/tasks`, tasks)).data }
export async function getExamBoard(cycleId) { return (await api.get(`/exams/cycles/${cycleId}`)).data }
export async function appointExamCommittee(cycleId, userId) { await api.post(`/exams/cycles/${cycleId}/committee/${userId}`) }
export async function uploadExamPaper(taskId, file) { const body = new FormData(); body.append('file', file); return (await api.post(`/exams/tasks/${taskId}/papers`, body, { headers: { 'Content-Type': 'multipart/form-data' } })).data }
export async function getExamPapers(taskId) { return (await api.get(`/exams/tasks/${taskId}/papers`)).data }
export async function reviewExamPaper(paperId, payload) { await api.put(`/exams/papers/${paperId}/review`, payload) }
export async function downloadExamPaper(paperId) { return (await api.get(`/exams/papers/${paperId}/file`, { responseType: 'blob' })).data }

export async function saveQuestionPolicy(taskId, payload) { return (await api.post(`/exams/tasks/${taskId}/question-policy`, payload)).data }
export async function validateExamPaper(taskId, paperId) { return (await api.post(`/exams/tasks/${taskId}/papers/${paperId}/validate`)).data }
export async function grantPaperAccess(paperId, userId, expiresAt = null) { await api.post(`/exams/papers/${paperId}/access/${userId}`, { expiresAt }) }
export async function revokePaperAccess(paperId, userId) { await api.delete(`/exams/papers/${paperId}/access/${userId}`) }
export async function importOfficialTimetable(cycleId, file) { const body = new FormData(); body.append('file', file); return (await api.post(`/exams/timetable-import/${cycleId}`, body, { headers: { 'Content-Type': 'multipart/form-data' } })).data }
export async function confirmImportedTimetableRow(rowId, payload) { return (await api.post(`/exams/timetable-import/${rowId}/confirm`, payload)).data }
export async function confirmExamSitting(taskId, payload) { return (await api.post(`/exams/tasks/${taskId}/sitting`, payload)).data }
export async function publishExamSitting(taskId) { return (await api.post(`/exams/tasks/${taskId}/publish`)).data }
export async function getMyExamTimetable() { return (await api.get('/exams/my-timetable')).data }
export async function assignExamDispatch(taskId, userId, venue) { return (await api.post(`/exams/tasks/${taskId}/dispatch`, { userId, venue })).data }
export async function updateExamCustody(id, payload) { return (await api.post(`/exams/dispatch/${id}/custody`, payload)).data }
export async function getExamDispatchBoard() { return (await api.get('/exams/dispatch')).data }
export async function collectExamScripts(taskId, lecturerId, quantity, notes) { return (await api.post(`/exams/tasks/${taskId}/script-batches`, { lecturerId, quantity, notes })).data }
export async function returnMarkedScripts(id, quantity, notes) { return (await api.post(`/exams/script-batches/${id}/return`, { quantity, notes })).data }
export async function getMyExamScripts() { return (await api.get('/exams/my-script-batches')).data }
export async function getExamMarkingReport() { return (await api.get('/exams/reports/marking')).data }
export async function getExamDispatchReport() { return (await api.get('/exams/reports/dispatch')).data }
export async function getExamNotifications() { return (await api.get('/exams/notifications')).data }
export async function markExamNotificationRead(id) { return (await api.post(`/exams/notifications/${id}/read`)).data }

export async function rotateExamMasterKey() { return (await api.post('/exams/security/rewrap-all')).data }
export async function getExamSubmissionReport() { return (await api.get('/exams/reports/submissions')).data }

export async function notifyScriptsReady(taskId) { return (await api.post(`/exams/tasks/${taskId}/scripts-ready`)).data }

export async function getExamSetupData() { const [courses, sessions] = await Promise.all([api.get('/courses'), api.get('/academic-sessions')]); return { courses: courses.data, sessions: sessions.data } }
