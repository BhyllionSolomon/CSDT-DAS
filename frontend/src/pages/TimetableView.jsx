import { useEffect, useState } from 'react'
import { Printer } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getStudentTimetable, getTimetableView } from '../services/timetableService'
import TimetableGrid, { printLandscape } from '../components/TimetableGrid'

const msg = (err) => err.response?.data?.message || err.message

function TimetableView() {
    const { user } = useAuth()
    const isStudent = user.role === 'STUDENT'

    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [data, setData] = useState(null)
    const [onlyMine, setOnlyMine] = useState(false)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then((s) => {
                setSessions(s)
                const current = currentSessionOf(s)
                if (current) setSessionId(String(current.id))
            })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    useEffect(() => {
        if (!sessionId) return
        setError(null)
        const request = isStudent ? getStudentTimetable(sessionId, semester) : getTimetableView(sessionId, semester)
        request.then(setData).catch((e) => setError(msg(e)))
    }, [sessionId, semester, isStudent])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const entries = data ? data.entries.filter((e) => !onlyMine || e.lecturerId === user.id) : []

    return (
        <div>
            <div className="no-print">
                <h2 className="text-2xl font-bold mb-2">{isStudent ? 'My Timetable' : 'Department Timetable'}</h2>
                <p className="theme-text-muted text-sm mb-6">
                    {isStudent
                        ? 'Your classes for the semester, based on the courses you registered.'
                        : 'The approved timetable for the department.'}
                </p>

                {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

                <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                    <div>
                        <label className="block text-sm font-medium mb-1">Session</label>
                        <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Semester</label>
                        <select value={semester} onChange={(e) => setSemester(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="FIRST">First</option>
                            <option value="SECOND">Second</option>
                        </select>
                    </div>
                    {!isStudent && (
                        <label className="text-sm flex items-center gap-2 pb-2">
                            <input type="checkbox" checked={onlyMine} onChange={(e) => setOnlyMine(e.target.checked)} />
                            Only my classes
                        </label>
                    )}
                    {data?.published && (
                        <button onClick={printLandscape} className="flex items-center gap-2 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                            <Printer size={15} /> Print
                        </button>
                    )}
                </div>

                {data && !data.published && (
                    <div className="card p-6 text-sm theme-text-muted">
                        The H.O.D has not published the timetable for this semester yet. It will appear here as soon as it is approved.
                    </div>
                )}
            </div>

            {data?.published && (
                <TimetableGrid entries={entries} sessionName={data.window.sessionName} semester={data.window.semester} />
            )}
        </div>
    )
}

export default TimetableView