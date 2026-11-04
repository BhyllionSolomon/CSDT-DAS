import api from './api'

export async function getStaff() {
    return (await api.get('/projects/staff')).data
}
export async function saveStaff(rows) {
    return (await api.put('/projects/staff', rows)).data
}
export async function getProposal(sessionId) {
    return (await api.get('/projects/proposal', { params: sessionId ? { sessionId } : {} })).data
}
export async function confirmAllocation(sessionId, assignments) {
    return (await api.post('/projects/confirm', { sessionId, assignments })).data
}
export async function getAssignments(sessionId) {
    return (await api.get('/projects/assignments', { params: sessionId ? { sessionId } : {} })).data
}
export async function removeAssignment(id) {
    await api.delete(`/projects/assignments/${id}`)
}
export async function getMyStudents() {
    return (await api.get('/projects/my-students')).data
}
export async function getMySupervisor() {
    return (await api.get('/academic-sessions/project-supervisor/mine')).data
}