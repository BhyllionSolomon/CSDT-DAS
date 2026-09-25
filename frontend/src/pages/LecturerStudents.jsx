import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

async function getMyAllocations(lecturerId) {
    const response = await api.get(`/allocations/lecturer/${lecturerId}`)
    return response.data
}

async function getRegistrationsForCourse(courseId, sessionId, semester) {
    const response = await api.get(
        `/course-registrations/course/${courseId}/session/${sessionId}/semester/${semester}`
    )
    return response.data
}

function LecturerStudents() {
    const { user } = useAuth()
    const [groups, setGroups] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        async function load() {
            try {
                const allocations = await getMyAllocations(user.id)
                const withStudents = await Promise.all(
                    allocations.map(async (a) => {
                        const students = await getRegistrationsForCourse(
                            a.courseId, a.academicSessionId, a.semester
                        )
                        return { ...a, students }
                    })
                )
                setGroups(withStudents)
            } catch (err) {
                setError(err.response?.data?.message || err.message)
            } finally {
                setLoading(false)
            }
        }
        load()
    }, [user.id])

    if (loading) return <p className="text-slate-500">Loading…</p>
    if (error) return <p className="text-red-600">{error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-6">My Students</h2>

            {groups.length === 0 && (
                <p className="text-slate-500">You have no assigned courses yet.</p>
            )}

            {groups.map((g) => (
                <div key={g.id} className="bg-white rounded-lg shadow overflow-hidden mb-6">
                    <div className="bg-slate-800 text-white px-4 py-2 text-sm font-semibold">
                        {g.courseCode} — {g.courseTitle} ({g.students.length} students)
                    </div>
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                        <tr>
                            <th className="px-4 py-2">Matric No.</th>
                            <th className="px-4 py-2">Name</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                        {g.students.map((s) => (
                            <tr key={s.id}>
                                <td className="px-4 py-2 font-medium">{s.matricNumber}</td>
                                <td className="px-4 py-2">{s.studentName}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            ))}
        </div>
    )
}

export default LecturerStudents