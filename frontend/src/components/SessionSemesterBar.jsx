import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'

function SessionSemesterBar({ onChange, children }) {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    useEffect(() => {
        getAllSessions().then((s) => {
            setSessions(s)
            const current = currentSessionOf(s)
            if (current) setSessionId(String(current.id))
        })
    }, [])

    useEffect(() => {
        if (sessionId) onChange({ sessionId, semester })
    }, [sessionId, semester])

    return (
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
            {children}
        </div>
    )
}

export default SessionSemesterBar