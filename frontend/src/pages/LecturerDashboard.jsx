import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

async function getMyAllocations(lecturerId) {
    const response = await api.get(`/allocations/lecturer/${lecturerId}`)
    return response.data
}

function LecturerDashboard() {
    const { user } = useAuth()
    const [allocations, setAllocations] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        getMyAllocations(user.id)
            .then(setAllocations)
            .finally(() => setLoading(false))
    }, [user.id])

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Welcome, {user.fullName}</h2>

            {allocations.length === 0 ? (
                <div className="bg-white rounded-lg shadow p-6">
                    <p className="text-slate-600 mb-2">
                        You have not been assigned any courses yet. Once the H.O.D allocates a
                        course to you, or you claim one from the available pool, it will appear here.
                    </p>
                    <Link to="/claim-courses" className="text-blue-600 hover:underline text-sm font-medium">
                        View courses available to claim →
                    </Link>
                </div>
            ) : (
                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <div className="bg-slate-800 text-white px-4 py-2 text-sm font-semibold">
                        Your Assigned Courses
                    </div>
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                        <tr>
                            <th className="px-4 py-3">Session</th>
                            <th className="px-4 py-3">Semester</th>
                            <th className="px-4 py-3">Code</th>
                            <th className="px-4 py-3">Title</th>
                            <th className="px-4 py-3"></th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                        {allocations.map((a) => (
                            <tr key={a.id}>
                                <td className="px-4 py-3">{a.academicSessionName}</td>
                                <td className="px-4 py-3">{a.semester}</td>
                                <td className="px-4 py-3 font-medium">{a.courseCode}</td>
                                <td className="px-4 py-3">{a.courseTitle}</td>
                                <td className="px-4 py-3 text-right">
                                    <Link
                                        to={`/bulk-enter-results?courseId=${a.courseId}&sessionId=${a.academicSessionId}&semester=${a.semester}`}
                                        className="text-blue-600 hover:underline text-xs font-medium"
                                    >
                                        Enter Scores →
                                    </Link>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    )
}

export default LecturerDashboard