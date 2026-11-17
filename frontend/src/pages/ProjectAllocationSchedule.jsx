import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getPublicSchedule } from '../services/projectService'

const msg = e => e.response?.data?.message || e.message

export default function ProjectAllocationSchedule() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        getAllSessions().then(list => {
            setSessions(list || [])
            const current = currentSessionOf(list || [])
            if (current) setSessionId(String(current.id))
        }).catch(e => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    useEffect(() => {
        if (!sessionId) return
        getPublicSchedule(Number(sessionId)).then(setRows).catch(e => setError(msg(e)))
    }, [sessionId])

    if (loading) return <p className="theme-text-muted">Loading published project schedule…</p>

    return <div className="max-w-6xl">
        <h2 className="text-2xl font-bold mb-2">Published Final-Year Project Supervisors</h2>
        <p className="theme-text-muted text-sm mb-5">Official project supervision allocation for the selected academic session.</p>
        {error && <p className="p-3 rounded bg-red-50 text-red-700 text-sm mb-4">{error}</p>}
        <div className="card p-4 mb-4">
            <label className="block text-sm font-medium mb-1">Academic session</label>
            <select value={sessionId} onChange={e => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                {sessions.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
        </div>
        <div className="card overflow-hidden">
            <div className="px-4 py-3 border-b theme-border flex justify-between">
                <strong className="text-sm">Published students</strong><span className="text-xs theme-text-muted">{rows.length} record(s)</span>
            </div>
            {rows.length === 0 ? <p className="p-5 text-sm theme-text-muted">No published allocation is available for this session yet.</p> :
                <div className="overflow-x-auto"><table className="w-full text-sm">
                    <thead className="theme-text-muted text-xs uppercase" style={{ background: 'var(--surface-soft)' }}>
                        <tr><th className="p-3 text-left">Supervisor</th><th className="p-3 text-left">Student name</th><th className="p-3 text-left">Matric number</th><th className="p-3 text-left">Department</th></tr>
                    </thead>
                    <tbody className="divide-y theme-border">{rows.map((r,i) => <tr key={`${r.matricNumber}-${i}`}>
                        <td className="p-3">{r.supervisorName}</td><td className="p-3 font-medium">{r.fullName}</td><td className="p-3">{r.matricNumber}</td><td className="p-3">{r.department}</td>
                    </tr>)}</tbody>
                </table></div>}
        </div>
    </div>
}
