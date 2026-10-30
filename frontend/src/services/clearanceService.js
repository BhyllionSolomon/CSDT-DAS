import api from './api'

export async function getMyClearance() {
    return (await api.get('/clearance/mine')).data
}
export async function submitInvoice(invoiceCode) {
    return (await api.post('/clearance/submit', { invoiceCode })).data
}
export async function getMyForm() {
    return (await api.get('/registration-form/mine')).data
}
export async function getStudentForm(studentId) {
    return (await api.get(`/clearance/form/${studentId}`)).data
}
export async function listClearances() {
    return (await api.get('/clearance')).data
}
export async function approveClearance(id) {
    return (await api.put(`/clearance/${id}/approve`)).data
}
export async function rejectClearance(id, note) {
    return (await api.put(`/clearance/${id}/reject`, { note })).data
}
export async function resetClearance(id) {
    await api.delete(`/clearance/${id}`)
}
export async function assistedClear(studentId, invoiceCode) {
    return (await api.post('/clearance/assisted', { studentId, invoiceCode })).data
}
export async function approveRegistered() {
    return (await api.post('/clearance/approve-registered')).data
}
export async function getGate() {
    return (await api.get('/clearance/settings')).data
}
export async function setGate(enabled) {
    return (await api.put('/clearance/settings', { enabled })).data
}