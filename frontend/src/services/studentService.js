import api from './api'

export async function getAllStudents() {
    const response = await api.get('/students')
    return response.data
}

export async function getStudent(id) {
    const response = await api.get(`/students/${id}`)
    return response.data
}