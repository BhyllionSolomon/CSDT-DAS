import api from './api'

export async function getAllocationsForSession(sessionId, semester) {
    return (await api.get(`/allocations/session/${sessionId}/semester/${semester}`)).data
}

export async function getMyOptions(sessionId, semester) {
    return (await api.get('/allocations/my-options', {
        params: { academicSessionId: sessionId, semester },
    })).data
}

export async function claimCourse(courseId, academicSessionId, semester) {
    return (await api.post('/allocations/claim', { courseId, academicSessionId, semester })).data
}

export async function offerCourse(courseId, academicSessionId, semester) {
    return (await api.post('/allocations/offer', { courseId, academicSessionId, semester })).data
}

export async function addLecturer(payload) {
    return (await api.post('/allocations/add-lecturer', payload)).data
}

export async function linkAllocation(allocationId, userId) {
    return (await api.put(`/allocations/${allocationId}/link/${userId}`)).data
}

export async function removeAllocation(allocationId) {
    await api.delete(`/allocations/${allocationId}`)
}

export async function previewAllocationDoc(file) {
    const formData = new FormData()
    formData.append('file', file)
    return (await api.post('/allocations/docx/preview', formData, {
        headers: { 'Content-Type': undefined },
    })).data
}

export async function commitAllocationDoc(payload) {
    return (await api.post('/allocations/docx/commit', payload)).data
}