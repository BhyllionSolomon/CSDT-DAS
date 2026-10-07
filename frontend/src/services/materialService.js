import api from './api'

export async function listCourseMaterials(courseId, sessionId, semester) {
    return (await api.get(`/materials/course/${courseId}/session/${sessionId}/semester/${semester}`)).data
}

export async function listMyMaterials() {
    return (await api.get('/materials/mine')).data
}

export async function addLinkMaterial(payload) {
    return (await api.post('/materials/link', payload)).data
}

export async function addFileMaterial({ courseId, academicSessionId, semester, title, description, file }) {
    const formData = new FormData()
    formData.append('courseId', courseId)
    formData.append('academicSessionId', academicSessionId)
    formData.append('semester', semester)
    formData.append('title', title)
    if (description) formData.append('description', description)
    formData.append('file', file)

    return (await api.post('/materials/file', formData, { headers: { 'Content-Type': undefined } })).data
}

export async function deleteMaterial(id) {
    await api.delete(`/materials/${id}`)
}

export async function downloadMaterial(material) {
    const response = await api.get(`/materials/${material.id}/download`, { responseType: 'blob' })
    const url = window.URL.createObjectURL(response.data)
    const a = document.createElement('a')
    a.href = url
    a.download = material.originalFilename || 'download'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
}

// Download errors arrive as a Blob; this turns any error into readable text.
export async function messageFrom(err) {
    const data = err.response?.data
    if (data instanceof Blob) {
        try {
            return JSON.parse(await data.text()).message || err.message
        } catch {
            return err.message
        }
    }
    return data?.message || err.message
}