import { useEffect, useState } from 'react'
import { getAllCourses, updateCourse, setCourseStatus } from '../services/courseService'

const STATUS_LABEL = { C: 'Compulsory', R: 'Required', E: 'Elective' }

function Courses() {
    const [courses, setCourses] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [editingId, setEditingId] = useState(null)
    const [draft, setDraft] = useState({})
    const [saving, setSaving] = useState(false)
    const [saveMessage, setSaveMessage] = useState(null)

    function load() {
        setLoading(true)
        getAllCourses().then(setCourses).catch((e) => setError(e.message)).finally(() => setLoading(false))
    }
    useEffect(load, [])

    function startEdit(course) {
        setEditingId(course.id)
        setDraft({
            title: course.title,
            creditUnit: course.creditUnit,
            semester: course.semester,
            status: course.status || 'C',
        })
        setSaveMessage(null)
    }
    function cancelEdit() { setEditingId(null); setDraft({}) }

    async function saveEdit(courseId) {
        setSaving(true)
        setError(null)
        try {
            const original = courses.find((c) => c.id === courseId)

            await updateCourse(courseId, {
                title: draft.title,
                creditUnit: Number(draft.creditUnit),
                semester: draft.semester,
            })

            if (draft.status && draft.status !== (original.status || 'C')) {
                await setCourseStatus(courseId, draft.status)
            }

            setSaveMessage(`${original?.code} updated successfully.`)
            setEditingId(null)
            load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSaving(false) }
    }

    if (loading) return <p className="text-slate-500">Loading courses…</p>
    if (error && courses.length === 0) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">All Courses</h2>
            <p className="theme-text-muted text-sm mb-6">
                Click Edit on any row to correct the title, credit unit, semester or status
                (C – Compulsory, R – Required, E – Elective). The status appears on the results broadsheet.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {saveMessage && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{saveMessage}</div>}

            <div className="card overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-3">Code</th>
                        <th className="px-4 py-3">Title</th>
                        <th className="px-4 py-3">Unit</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3">Level</th>
                        <th className="px-4 py-3">Semester</th>
                        <th className="px-4 py-3">Programmes</th>
                        <th className="px-4 py-3"></th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {courses.map((course) => {
                        const isEditing = editingId === course.id
                        return (
                            <tr key={course.id} className={isEditing ? 'bg-blue-50/40' : ''}>
                                <td className="px-4 py-3 font-medium">{course.code}</td>
                                <td className="px-4 py-3">
                                    {isEditing ? (
                                        <input value={draft.title} onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                                               className="border theme-border rounded px-2 py-1 text-sm w-full" />
                                    ) : course.title}
                                </td>
                                <td className="px-4 py-3">
                                    {isEditing ? (
                                        <input type="number" value={draft.creditUnit} onChange={(e) => setDraft({ ...draft, creditUnit: e.target.value })}
                                               className="border theme-border rounded px-2 py-1 text-sm w-16" />
                                    ) : course.creditUnit}
                                </td>
                                <td className="px-4 py-3">
                                    {isEditing ? (
                                        <select value={draft.status} onChange={(e) => setDraft({ ...draft, status: e.target.value })}
                                                className="border theme-border rounded px-2 py-1 text-sm">
                                            <option value="C">C – Compulsory</option>
                                            <option value="R">R – Required</option>
                                            <option value="E">E – Elective</option>
                                        </select>
                                    ) : (
                                        <span title={STATUS_LABEL[course.status || 'C']} className="font-medium">{course.status || 'C'}</span>
                                    )}
                                </td>
                                <td className="px-4 py-3">{course.levelCode}</td>
                                <td className="px-4 py-3">
                                    {isEditing ? (
                                        <select value={draft.semester} onChange={(e) => setDraft({ ...draft, semester: e.target.value })}
                                                className="border theme-border rounded px-2 py-1 text-sm">
                                            <option value="FIRST">First</option>
                                            <option value="SECOND">Second</option>
                                        </select>
                                    ) : course.semester}
                                </td>
                                <td className="px-4 py-3">
                                    <div className="flex flex-wrap gap-1">
                                        {course.programmes?.length > 0 ? course.programmes.map((p) => (
                                            <span key={p.id} className="px-2 py-0.5 rounded-full text-xs font-medium"
                                                  style={{ backgroundColor: 'var(--surface-soft)', color: 'var(--accent)' }}>
                          {p.code}
                        </span>
                                        )) : <span className="theme-text-muted text-xs">Unassigned</span>}
                                    </div>
                                </td>
                                <td className="px-4 py-3 text-right whitespace-nowrap">
                                    {isEditing ? (
                                        <div className="flex gap-2 justify-end">
                                            <button onClick={cancelEdit} className="text-xs theme-text-muted hover:underline">Cancel</button>
                                            <button onClick={() => saveEdit(course.id)} disabled={saving}
                                                    className="text-xs font-medium px-2 py-1 rounded accent-gradient text-white disabled:opacity-50">
                                                {saving ? 'Saving…' : 'Save'}
                                            </button>
                                        </div>
                                    ) : (
                                        <button onClick={() => startEdit(course)} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>
                                            Edit
                                        </button>
                                    )}
                                </td>
                            </tr>
                        )
                    })}
                    </tbody>
                </table>
                {courses.length === 0 && <p className="text-center theme-text-muted py-8">No courses found.</p>}
            </div>
        </div>
    )
}

export default Courses