import api from './api'

/** Read-only catalogue: no order or payment is created in preview mode. */
export async function listStudentLabManualOptions() {
    return (await api.get('/student/lab-manuals')).data
}
