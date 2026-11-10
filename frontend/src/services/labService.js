import api from './api'

export async function listLabCourses() { return (await api.get('/lab/courses')).data }
export async function getManuals() { return (await api.get('/lab/manuals')).data }
export async function saveManual(payload) { return (await api.post('/lab/manuals', payload)).data }
export async function addPurchase(manualId, payload) { return (await api.post(`/lab/manuals/${manualId}/purchases`, payload)).data }
export async function searchLabStudents(q) { return (await api.get('/lab/students/search', { params: { q } })).data }
export async function sellManual(manualId, studentId) { return (await api.post('/lab/sales', { manualId, studentId })).data }
export async function getSales() { return (await api.get('/lab/sales')).data }
export async function voidSale(id) { return (await api.delete(`/lab/sales/${id}`)).data }
export async function getFinance() { return (await api.get('/lab/finance')).data }
export async function getImpact(sessionId, manualId) { return (await api.get('/lab/impact', { params: { sessionId, manualId } })).data }

export const naira = (v) => '₦' + Number(v || 0).toLocaleString('en-NG', { minimumFractionDigits: 2, maximumFractionDigits: 2 })