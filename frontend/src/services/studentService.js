import api from './api'

export async function getAllStudents() {
    const response = await api.get('/students')
    return response.data
}

export async function getStudent(id) {
    const response = await api.get(`/students/${id}`)
    return response.data
}

export async function uploadStudentsCsv(departmentId, academicSessionId, file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await api.post(
        `/students/csv/upload?departmentId=${departmentId}&academicSessionId=${academicSessionId}`,
        formData,
        { headers: { 'Content-Type': undefined } }
    )
    return response.data
}