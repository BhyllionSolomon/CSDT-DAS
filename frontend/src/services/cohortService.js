import api from './api'

export async function getCoverage(intakeYear) {
    return (await api.get('/cohorts/coverage', { params: { intakeYear } })).data
}
export async function verifyCohort(intakeYear, sessionYear, kind, verified) {
    return (await api.post('/cohorts/verify', { intakeYear, sessionYear, kind, verified })).data
}
export async function checkStudentList(payload) {
    return (await api.post('/cohorts/check', payload)).data
}