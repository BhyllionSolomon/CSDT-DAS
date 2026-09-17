import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { getAvailableCourses, claimCourse } from '../services/allocationService'

function ClaimCourses() {
    const [sessions, setSessions] = useState([])
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [available, setAvailable] = useState([])
    const [loaded, setLoaded] = useState(false)
    const [loading, setLoading] = useState(true)
    const [loadingList, setLoadingList] = useState(false)
    const [claimingId, setClaimingId] = useState(null)
    const [message, setMessage] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then(setSessions)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    async function handleLoad() {
        if (!academicSessionId) return
        setError(null)
        setMessage(null)
        setLoadingList(true)
        try {
            const data = await getAvailableCourses(academicSessionId, semester)
            setAvailable(data)
            setLoaded(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setLoadingList(false)
        }
    }

    async function handleClaim(courseId) {
        setError(null)
        setMessage(null)
        setClaimingId(courseId)
        try {
            await claimCourse(courseId, academicSessionId, semester)
            setMessage('Course claimed successfully.')
            handleLoad()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setClaimingId(null)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Claim Courses</h2>
            <p className="text-sm text-slate-500 mb-6">
                Select a session and semester to see courses still available. You must
                be logged in as a Lecturer or Adjunct to claim.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}
            {message && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    {message}
                </div>
            )}

            <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Session</label>
                    <select
                        value={academicSessionId}
                        onChange={(e) => setAcademicSessionId(e.target.value)}
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select…</option>
                        {sessions.map((s) => (
                            <option key={s.id} value={s.id}>{s.name}</option>
                        ))}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                    <select
                        value={semester}
                        onChange={(e) => setSemester(e.target.value)}
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
                <button
                    onClick={handleLoad}
                    disabled={!academicSessionId || loadingList}
                    className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {loadingList ? 'Loading…' : 'Show Available Courses'}
                </button>
            </div>

            {loaded && (
                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                        <tr>
                            <th className="px-4 py-3">Code</th>
                            <th className="px-4 py-3">Title</th>
                            <th className="px-4 py-3">Unit</th>
                            <th className="px-4 py-3"></th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                        {available.map((c) => (
                            <tr key={c.id}>
                                <td className="px-4 py-3 font-medium">{c.code}</td>
                                <td className="px-4 py-3">{c.title}</td>
                                <td className="px-4 py-3">{c.creditUnit}</td>
                                <td className="px-4 py-3 text-right">
                                    <button
                                        onClick={() => handleClaim(c.id)}
                                        disabled={claimingId === c.id}
                                        className="bg-green-600 text-white rounded-md px-3 py-1 text-xs font-medium hover:bg-green-700 disabled:opacity-50"
                                    >
                                        {claimingId === c.id ? 'Claiming…' : 'Claim'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    {available.length === 0 && (
                        <p className="text-center text-slate-400 py-8">
                            No unclaimed courses for this session/semester.
                        </p>
                    )}
                </div>
            )}
        </div>
    )
}

export default ClaimCourses