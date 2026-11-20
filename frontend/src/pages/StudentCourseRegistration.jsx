import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import RegistrationPlanner from '../components/RegistrationPlanner'

function StudentCourseRegistration() {
    const { user } = useAuth()
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [registeredNow, setRegisteredNow] = useState(false)

    useEffect(() => {
        getAllSessions().then((s) => {
            setSessions(s)
            const current = currentSessionOf(s)
            if (current) setSessionId(String(current.id))
        })
    }, [])

    if (!user.studentId) {
        return (
            <div className="card p-6 max-w-xl">
                <h2 className="text-xl font-bold mb-2">Course Registration</h2>
                <p className="text-sm theme-text-muted">
                    Your account isn't linked to a student record yet. If you have just been added to the department's roster,
                    sign up with your matric number. Otherwise contact your Level Adviser or the H.O.D.
                </p>
            </div>
        )
    }

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-1">Course Registration</h2>
            <p className="theme-text-muted text-sm mb-6">{user.fullName} · {user.programmeName} · {user.levelName}</p>

            {registeredNow && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    Next, open <Link to="/my-registration" className="underline font-medium">Registration &amp; Payment</Link> and enter your invoice code.
                </div>
            )}

            <div className="card p-4 mb-6 flex gap-4 flex-wrap">
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
            </div>

            <RegistrationPlanner studentId={user.studentId} sessionId={sessionId} semester={semester}
                                 onRegistered={() => setRegisteredNow(true)} />
        </div>
    )
}

export default StudentCourseRegistration