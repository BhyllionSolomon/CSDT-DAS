import api from './api'

export async function getNotices() { return (await api.get('/academic-sessions/notices')).data }
export async function readNotice(id) { return (await api.get(`/academic-sessions/notices/${id}/read`)).data }
export async function getNoticeTargets() { return (await api.get('/notices/targets')).data }
export async function postNotice(payload) { return (await api.post('/notices', payload)).data }
export async function deleteNotice(id) { return (await api.delete(`/notices/${id}`)).data }