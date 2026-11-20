import api from './api'

export async function getAttendanceCourses(sessionId, semester) {
    return (await api.get('/attendance/courses', { params: { sessionId, semester } })).data
}
export async function getAttendanceClasses(courseId, sessionId, semester) {
    return (await api.get('/attendance/classes', { params: { courseId, sessionId, semester } })).data
}
export async function getAttendanceRoster(courseId, sessionId, semester, date, kind) {
    return (await api.get('/attendance/roster', { params: { courseId, sessionId, semester, date, kind } })).data
}
export async function saveAttendance(payload) {
    return (await api.post('/attendance/save', payload)).data
}
export async function deleteAttendanceClass(id) {
    await api.delete(`/attendance/classes/${id}`)
}
export async function getAttendanceSummary(courseId, sessionId, semester) {
    return (await api.get('/attendance/summary', { params: { courseId, sessionId, semester } })).data
}
export async function getMyAttendance(sessionId, semester) {
    return (await api.get('/academic-sessions/attendance/mine', { params: { sessionId, semester } })).data
}