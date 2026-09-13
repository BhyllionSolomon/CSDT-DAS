import api from './api'

export async function getAllCourses() {
    const response = await api.get('/courses')
    return response.data
}

export async function getCourse(id) {
    const response = await api.get(`/courses/${id}`)
    return response.data
}