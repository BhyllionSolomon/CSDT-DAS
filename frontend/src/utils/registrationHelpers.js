// Courses failed earlier that must be registered first, for the chosen semester only.
// A course failed in a semester is registered first in the same semester of the next session.
export function carryOversFor(outstanding, semester) {
    return (outstanding || [])
        .map((o) => ({
            id: o.id ?? o.courseId,
            code: o.code ?? o.courseCode,
            title: o.title ?? o.courseTitle,
            unit: o.creditUnit ?? o.unit ?? 0,
            semester: o.semester,
        }))
        .filter((o) => o.id && String(o.semester || '').toUpperCase() === semester)
}

// The latest session whose start date has arrived.
export function currentSessionOf(sessions) {
    const started = sessions.filter((s) => s.startDate && new Date(s.startDate) <= new Date())
    const pool = started.length ? started : sessions
    return [...pool].sort((a, b) => String(b.startDate).localeCompare(String(a.startDate)))[0] || null
}