import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { getStudentsForSession, backfillStudentHistory } from '../services/studentHistoryService'

function StudentsPerSession() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [records, setRecords] = useState(null)
    const [loading, setLoading] = useState(true)
    const [loadingRecords, setLoadingRecords] = useState(false)
    const [backfilling, setBackfilling] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    useEffect(() => {
        getAllSessions().then(setSessions).catch((e) => setError(e.message)).finally(() => setLoading(false))
    }, [])

    async function handleBackfill() {
        setBackfilling(true)
        setMessage(null)
        try {
            const count = await backfillStudentHistory()
            setMessage(`${count} historical record(s) written from current student data.`)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setBackfilling(false) }
    }

    async function handleLoad() {
        if (!sessionId) return
        setError(null)
        setLoadingRecords(true)
        try {
            const data = await getStudentsForSession(sessionId)
            setRecords(data)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setLoadingRecords(false) }
    }

    const grouped = {}
    if (records) {
        for (const r of records) {
            if (!grouped[r.programmeName]) grouped[r.programmeName] = {}
            if (!grouped[r.programmeName][r.levelCode]) grouped[r.programmeName][r.levelCode] = []
            grouped[r.programmeName][r.levelCode].push(r)
        }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Students Per Session</h2>
            <p className="theme-text-muted text-sm mb-6">
                See which students were in which programme and level during a given
                academic session. If you're setting this up for the first time, run
                the one-time sync below first.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <button onClick={handleBackfill} disabled={backfilling}
                        className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {backfilling ? 'Syncing…' : 'One-Time Sync (Backfill from Current Data)'}
                </button>

                <div>
                    <label className="block text-sm font-medium mb-1">Academic Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <button onClick={handleLoad} disabled={!sessionId || loadingRecords}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {loadingRecords ? 'Loading…' : 'Show Students'}
                </button>
            </div>

            {records && (
                Object.keys(grouped).length === 0 ? (
                    <p className="theme-text-muted">
                        No records found for this session yet. Try running the one-time sync above,
                        or this session may be older than the history feature.
                    </p>
                ) : (
                    Object.entries(grouped).map(([programmeName, levels]) => (
                        <div key={programmeName} className="mb-8">
                            <h3 className="text-lg font-bold mb-3">{programmeName}</h3>
                            <div className="grid grid-cols-2 gap-4">
                                {Object.entries(levels).sort(([a], [b]) => a.localeCompare(b)).map(([levelCode, students]) => (
                                    <div key={levelCode} className="card overflow-hidden">
                                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">
                                            {levelCode} Level ({students.length})
                                        </div>
                                        <table className="w-full text-sm text-left">
                                            <tbody className="divide-y theme-border">
                                            {students.map((s) => (
                                                <tr key={s.studentId}>
                                                    <td className="px-4 py-2 font-medium">{s.matricNumber}</td>
                                                    <td className="px-4 py-2">{s.fullName}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))
                )
            )}
        </div>
    )
}
export default StudentsPerSession