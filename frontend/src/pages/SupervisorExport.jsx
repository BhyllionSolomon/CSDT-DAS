import { useEffect, useState } from 'react'
import { Download } from 'lucide-react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getAllocationPreview, downloadAllocationWord } from '../services/deployService'

const msg = (err) => err.response?.data?.message || err.message

function SupervisorExport() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions().then((s) => {
            setSessions(s)
            const current = currentSessionOf(s)
            if (current) setSessionId(String(current.id))
        }).catch((e) => setError(msg(e)))
    }, [])

    useEffect(() => {
        if (!sessionId) return
        setData(null); setError(null)
        getAllocationPreview(sessionId).then(setData).catch((e) => setError(msg(e)))
    }, [sessionId])

    async function download() {
        setBusy(true); setError(null)
        try {
            const blob = await downloadAllocationWord(sessionId)
            const name = sessions.find((s) => String(s.id) === sessionId)?.name.replace('/', '-') || 'session'
            const a = document.createElement('a')
            a.href = URL.createObjectURL(blob)
            a.download = `supervision-allocation-${name}.docx`
            a.click()
        } catch (err) {
            // A failed download comes back as a blob; read the message out of it.
            try { setError(JSON.parse(await err.response.data.text()).message) } catch { setError(msg(err)) }
        } finally { setBusy(false) }
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Project Allocation (Word)</h2>
            <p className="theme-text-muted text-sm mb-6">
                The approved final-year supervisor allocation for a session. Download the Word document to print or share.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-3 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <button onClick={download} disabled={busy || !data || data.count === 0}
                        className="flex items-center gap-2 accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    <Download size={15} /> {busy ? 'Preparing…' : 'Download Word'}
                </button>
            </div>

            {data && data.count === 0 && (
                <div className="card p-6 text-sm theme-text-muted">No student has been assigned a supervisor for this session yet. Allocate them first under Final-Year Projects.</div>
            )}

            {data && data.count > 0 && (
                <div className="card overflow-hidden">
                    <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{data.session} · {data.count} student(s)</div>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                            <tr><th className="px-4 py-2">Supervisor</th><th className="px-4 py-2">Student name</th><th className="px-4 py-2">Matriculation number</th><th className="px-4 py-2">Department</th></tr>
                            </thead>
                            <tbody className="divide-y theme-border">
                            {data.rows.map((r, i) => (
                                <tr key={i}>
                                    <td className="px-4 py-2 font-medium">{r.supervisor}</td><td className="px-4 py-2">{r.student}</td>
                                    <td className="px-4 py-2">{r.matric}</td><td className="px-4 py-2">{r.department}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    )
}

export default SupervisorExport