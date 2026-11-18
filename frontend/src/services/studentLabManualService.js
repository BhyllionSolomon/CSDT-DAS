import api from './api'
export async function getStudentLabManuals() { return (await api.get('/lab/student/manuals')).data }
export async function requestStudentLabManual(manualId) {
    return (await api.post('/lab/student/manuals/orders', { manualId })).data
}
