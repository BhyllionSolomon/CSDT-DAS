import { useEffect, useState } from 'react'
import { getMyStudents } from '../services/projectService'
import { classLabel, CLASS_STYLE } from '../utils/projects'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '')

function MyProjectStudents() {
    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getMyStudents().then(setRows).catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">My Project Students</h2>
            <p className="theme-text-muted text-sm mb-6">The final-year students assigned to you for project supervision this session.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {rows.length === 0 ? (
                <div className="card p-6 text-sm theme-text-muted">No student has been assigned to you yet. The H.O.D assigns supervisors once, at the start of the project year.</div>
            ) : (
                <div className="card overflow-hidden">
                    <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{rows[0].sessionName} · {rows.length} student(s)</div>
                    <table className="w-full text-sm text-left">
                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                        <tr>
                            <th className="px-4 py-2">Matric</th>
                            <th className="px-4 py-2">Name</th>
                            <th className="px-4 py-2">Programme</th>
                            <th className="px-4 py-2">Class</th>
                            <th className="px-4 py-2">Assigned</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y theme-border">
                        {rows.map((r) => (
                            <tr key={r.studentId}>
                                <td className="px-4 py-2">{r.matricNumber}</td>
                                <td className="px-4 py-2 font-medium">{r.fullName}</td>
                                <td className="px-4 py-2">{r.programme}</td>
                                <td className="px-4 py-2">
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${CLASS_STYLE[r.classification]}`}>{classLabel(r.classification)}</span>
                                </td>
                                <td className="px-4 py-2 theme-text-muted">{when(r.assignedAt)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    )
}

export default MyProjectStudents