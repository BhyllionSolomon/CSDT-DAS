import api from './api'

export async function extractCourseRoster(file) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post('/courses/roster/extract', formData, { headers: { 'Content-Type': undefined } })
    return response.data
}
export async function saveCourseRoster(programmeId, levelId, semester, rows) {
    const response = await api.post('/courses/roster/save', { programmeId, levelId, semester, rows })
    return response.data
}