import api from './api'

export async function getStudentRegistrations(studentId) {
    const response = await api.get(`/course-registrations/student/${studentId}`)
    return response.data
}

export async function getRegistrationsForCourse(courseId, academicSessionId, semester) {
    const response = await api.get(
        `/course-registrations/course/${courseId}/session/${academicSessionId}/semester/${semester}`
    )
    return response.data
}

export async function registerCourse(studentId, courseId, academicSessionId, semester) {
    const response = await api.post('/course-registrations', {
        studentId,
        courseId,
        academicSessionId,
        semester,
    })
    return response.data
}

export async function getOutstandingCourses(studentId) {
    const response = await api.get(`/course-registrations/student/${studentId}/outstanding-courses`)
    return response.data
}

export async function registerMultiple(studentId, academicSessionId, semester, courseIds) {
    const response = await api.post('/course-registrations/bulk', {
        studentId,
        academicSessionId,
        semester,
        courseIds,
    })
    return response.data
}

export async function getRequiredUnits(programmeId, levelId, semester) {
    const response = await api.get(
        `/unit-limits/programme/${programmeId}/level/${levelId}/semester/${semester}`
    )
    return response.data
}

export async function setRequiredUnits(programmeId, levelId, semester, requiredUnits) {
    const response = await api.put(
        `/unit-limits/programme/${programmeId}/level/${levelId}/semester/${semester}`,
        { requiredUnits }
    )
    return response.data
}