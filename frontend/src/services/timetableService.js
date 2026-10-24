import api from './api'

export async function listRooms() {
    return (await api.get('/timetable/rooms')).data
}
export async function addRoom(name, type) {
    return (await api.post('/timetable/rooms', { name, type })).data
}
export async function setRoomActive(id, active) {
    return (await api.put(`/timetable/rooms/${id}/active`, { active })).data
}
export async function getAdminStatus(sessionId, semester) {
    return (await api.get('/timetable/status', { params: { sessionId, semester } })).data
}
export async function openWindow(sessionId, semester, deadline) {
    return (await api.post('/timetable/windows', { sessionId, semester, deadline })).data
}
export async function updateDeadline(id, deadline) {
    return (await api.put(`/timetable/windows/${id}/deadline`, { deadline })).data
}
export async function reopenWindow(id, deadline) {
    return (await api.post(`/timetable/windows/${id}/reopen`, { deadline })).data
}
export async function autoFillWindow(id) {
    return (await api.post(`/timetable/windows/${id}/auto-fill`)).data
}
export async function publishWindow(id, force) {
    return (await api.post(`/timetable/windows/${id}/publish`, { force })).data
}
export async function getMineTimetable(sessionId, semester) {
    return (await api.get('/timetable/mine', { params: { sessionId, semester } })).data
}
export async function getTimetableView(sessionId, semester) {
    return (await api.get('/timetable/view', { params: { sessionId, semester } })).data
}
export async function getStudentTimetable(sessionId, semester) {
    return (await api.get(`/academic-sessions/${sessionId}/timetable/mine`, { params: { semester } })).data
}
export async function addEntry(payload) {
    return (await api.post('/timetable/entries', payload)).data
}
export async function deleteEntry(id) {
    await api.delete(`/timetable/entries/${id}`)
}