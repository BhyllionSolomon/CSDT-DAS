import api from './api'

export async function uploadManualSubmission({ title, courseId, pageCount, suggestedLevy, file }) {
    const data = new FormData()
    data.append('title', title)
    data.append('courseId', courseId)
    data.append('pageCount', pageCount)
    if (suggestedLevy) data.append('suggestedLevy', suggestedLevy)
    data.append('file', file)
    return (await api.post('/lab/manual-workflow/submissions', data)).data
}
export async function getMyManualSubmissions() { return (await api.get('/lab/manual-workflow/mine')).data }
export async function getManualApprovalQueue() { return (await api.get('/lab/manual-workflow/hod-queue')).data }
export async function getLabManualQueue() { return (await api.get('/lab/manual-workflow/lab-queue')).data }
export async function approveManualSubmission(id, approvedLevy, comment) { return (await api.put('/lab/manual-workflow/' + id + '/approve', { approvedLevy, comment })).data }
export async function returnManualSubmission(id, comment) { return (await api.put('/lab/manual-workflow/' + id + '/return', { comment })).data }
export async function dispatchManualSubmission(id, targetLab) { return (await api.put('/lab/manual-workflow/' + id + '/dispatch', { targetLab })).data }
export async function receiveManualSubmission(id) { return (await api.put('/lab/manual-workflow/' + id + '/receive')).data }
export async function getManualBuyers(id) { return (await api.get('/lab/manual-workflow/' + id + '/buyers')).data }
export function manualFileUrl(id) { return '/api/lab/manual-workflow/' + id + '/file' }
