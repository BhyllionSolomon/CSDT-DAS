import api from './api'

export async function extractRoster(file) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post('/students/roster/extract', formData, {
        headers: { 'Content-Type': undefined },
    })
    return response.data
}

export async function saveRoster(programmeId, levelId, academicSessionId, rows) {
    const response = await api.post('/students/roster/save', {
        programmeId, levelId, academicSessionId, rows,
    })
    return response.data
}