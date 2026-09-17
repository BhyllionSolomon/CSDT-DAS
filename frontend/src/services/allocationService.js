import api from './api'

export async function getAllAllocations() {
    const response = await api.get('/allocations')
    return response.data
}

export async function getAllocationsForSession(academicSessionId, semester) {
    const response = await api.get(
        `/allocations/session/${academicSessionId}/semester/${semester}`
    )
    return response.data
}

export async function getAvailableCourses(academicSessionId, semester) {
    const response = await api.get(
        `/allocations/available?academicSessionId=${academicSessionId}&semester=${semester}`
    )
    return response.data
}

export async function claimCourse(courseId, academicSessionId, semester) {
    const response = await api.post('/allocations/claim', {
        courseId,
        academicSessionId,
        semester,
    })
    return response.data
}

export async function uploadAllocationDocx(academicSessionId, semester, file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await api.post(
        `/allocations/upload-docx?academicSessionId=${academicSessionId}&semester=${semester}`,
        formData,
        { headers: { 'Content-Type': undefined } }
    )
    return response.data
}