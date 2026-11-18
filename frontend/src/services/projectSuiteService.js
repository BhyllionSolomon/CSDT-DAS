import api from './api'

const data = (promise) => promise.then((r) => r.data)

export const getCases = (sessionId, all) => data(api.get('/project-suite/cases', { params: { ...(sessionId ? { sessionId } : {}), all } }))
export const getAlerts = () => data(api.get('/project-suite/alerts'))
export const getCase = (id) => data(api.get(`/project-suite/cases/${id}`))
export const saveStage = (id, body) => data(api.put(`/project-suite/cases/${id}/stage`, body))
export const addMeeting = (id, body) => data(api.post(`/project-suite/cases/${id}/meetings`, body))
export const deleteMeeting = (id) => data(api.delete(`/project-suite/meetings/${id}`))
export const saveAssessment = (id, body) => data(api.put(`/project-suite/cases/${id}/assessment`, body))
export const schedulePresentation = (id, body) => data(api.post(`/project-suite/cases/${id}/presentations`, body))
export const setOutcome = (presentationId, body) => data(api.put(`/project-suite/presentations/${presentationId}/outcome`, body))
export const addFeedback = (presentationId, body) => data(api.post(`/project-suite/presentations/${presentationId}/feedback`, body))

export const getMyProject = () => data(api.get('/project-suite/mine'))
export const getSurveyState = () => data(api.get('/project-survey/state'))
export const submitSurvey = (body) => data(api.post('/project-survey/submit', body))
export const setSurveyWindow = (sessionId, open) => data(api.put('/project-survey/window', { sessionId, open }))
export const getSurveyResults = (sessionId) => data(api.get('/project-survey/results', { params: sessionId ? { sessionId } : {} }))

export const STATUS_LABEL = { NOT_STARTED: 'Not started', IN_PROGRESS: 'In progress', SUBMITTED: 'Submitted', APPROVED: 'Approved' }
export const STATUS_COLOR = { NOT_STARTED: '#94a3b8', IN_PROGRESS: '#3b82f6', SUBMITTED: '#f59e0b', APPROVED: '#16a34a' }
export const OUTCOME_LABEL = { NONE: 'Not scheduled', PENDING: 'Scheduled', PASSED: 'Passed', CORRECTIONS_REQUIRED: 'Corrections required', REPRESENT: 'Re-present' }
export const OUTCOME_TONE = {
    NONE: 'bg-slate-200 text-slate-600', PENDING: 'bg-blue-100 text-blue-700', PASSED: 'bg-green-100 text-green-700',
    CORRECTIONS_REQUIRED: 'bg-amber-100 text-amber-700', REPRESENT: 'bg-red-100 text-red-700',
}
export const dateTime = (v) => (v ? new Date(v).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '')
export const day = (v) => (v ? new Date(v).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '')
export const errorOf = (err) => err.response?.data?.message || err.message