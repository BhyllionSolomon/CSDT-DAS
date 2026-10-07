import api from './api'

export async function createOnlineClass(payload) {
    return (await api.post('/online-classes', payload)).data
}
export async function listCourseClasses(courseId, sessionId, semester) {
    return (await api.get(`/online-classes/course/${courseId}/session/${sessionId}/semester/${semester}`)).data
}
export async function listMyClasses() {
    return (await api.get('/online-classes/mine')).data
}
export async function startOnlineClass(id) {
    return (await api.put(`/online-classes/${id}/start`)).data
}
export async function endOnlineClass(id) {
    await api.put(`/online-classes/${id}/end`)
}
export async function joinOnlineClass(id) {
    return (await api.post(`/online-classes/${id}/join`)).data
}
export async function getClassParticipants(id) {
    return (await api.get(`/online-classes/${id}/participants`)).data
}