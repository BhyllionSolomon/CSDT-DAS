import api from './api'
export async function getStudentLabManuals() { return (await api.get('/lab/student/manuals')).data }
export async function requestStudentLabManual(manualId) {
    return (await api.post('/lab/student/manuals/orders', { manualId })).data
}

export async function initiateStudentLabManualPayment(orderId) {
    return (await api.post('/lab/student/manuals/orders/' + orderId + '/pay')).data
}
export async function verifyStudentLabManualPayment(reference) {
    return (await api.get('/lab/student/manuals/payments/verify', { params: { reference } })).data
}
export async function getStudentLabManualReceipt(orderId) {
    return (await api.get('/lab/student/manuals/orders/' + orderId + '/receipt')).data
}
