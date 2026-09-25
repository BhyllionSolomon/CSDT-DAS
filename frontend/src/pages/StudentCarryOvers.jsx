import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getOutstandingCourses } from '../services/registrationService'

function StudentCarryOvers() {
    const { user } = useAuth()
    const [courses, setCourses] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!user.studentId) { setLoading(false); return }
        getOutstandingCourses(user.studentId)
            .then(setCourses)
            .catch((e) => setError(e.response?.data?.message || e.message))
            .finally(() => setLoading(false))
    }, [user.studentId])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Carry-Over Courses</h2>
            <p className="theme-text-muted text-sm mb-6">
                Courses you have previously failed and not yet passed. These must be
                registered first, before your normal semester courses, when you next register.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {courses.length === 0 ? (
                <div className="card p-8 text-center">
                    <p className="text-2xl mb-2">🎉</p>
                    <p className="font-medium">You have no outstanding carry-over courses.</p>
                    <p className="theme-text-muted text-sm mt-1">Keep it up!</p>
                </div>
            ) : (
                <div className="card overflow-hidden">
                    <table className="w-full text-sm text-left">
                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                        <tr>
                            <th className="px-4 py-3">Code</th>
                            <th className="px-4 py-3">Title</th>
                            <th className="px-4 py-3">Unit</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y theme-border">
                        {courses.map((c) => (
                            <tr key={c.id}>
                                <td className="px-4 py-3 font-medium">{c.code}</td>
                                <td className="px-4 py-3">{c.title}</td>
                                <td className="px-4 py-3">{c.creditUnit}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    )
}
export default StudentCarryOvers