import { useEffect, useMemo, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { previewSessionStart, applySessionStart } from '../services/sessionStartService'

const msg = (err) => err.response?.data?.message || err.message

function StartNewSession() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [data, setData] = useState(null)
    const [skip, setSkip] = useState({})
    const [query, setQuery] = useState('')
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

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

    async function loadPreview() {
        setData(await previewSessionStart(sessionId))
        setSkip({})
    }

    async function handlePreview() {
        setError(null); setMessage(null); setBusy(true)
        try { await loadPreview() } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const groups = useMemo(() => {
        const map = new Map()
        for (const r of data?.rows || []) {
            const key = `${r.programme}: ${r.fromLevel} → ${r.toLevel}`
            map.set(key, (map.get(key) || 0) + 1)
        }
        return [...map.entries()]
    }, [data])

    const term = query.trim().toLowerCase()
    const shown = (data?.rows || []).filter((r) =>
        !term || r.matricNumber.toLowerCase().includes(term) || r.fullName.toLowerCase().includes(term))
    const excluded = Object.keys(skip).filter((k) => skip[k]).map(Number)
    const moving = data ? data.rows.length - excluded.length : 0

    async function handleApply() {
        const ok = window.confirm(
            `Move ${moving} student(s) into ${data.sessionName}?\n\n` +
            'Their previous level is kept in the history. Students already moved are skipped, so running this again is safe.'
        )
        if (!ok) return

        setError(null); setMessage(null); setBusy(true)
        try {
            const result = await applySessionStart(sessionId, excluded)
            await loadPreview()
            setMessage(result.message)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Start New Session</h2>
            <p className="theme-text-muted text-sm mb-6">
                Moves active students up to the level they belong in for the chosen session. Do this once, after the session is created and before
                students register. Students already moved into the session are skipped, so pressing it twice does no harm.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">New session</label>
                    <select value={sessionId} onChange={(e) => { setSessionId(e.target.value); setData(null) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <button onClick={handlePreview} disabled={!sessionId || busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Working…' : 'Preview the move'}
                </button>
            </div>

            {data && (
                <div className="space-y-6">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {[
                            ['To move', data.toMove],
                            ['Already in this session', data.alreadyOnSession],
                            ['Stay in final year (400)', data.atTopLevel],
                            ['No session on record', data.noSessionRecorded],
                        ].map(([label, value]) => (
                            <div key={label} className="card p-4">
                                <p className="text-2xl font-bold">{value}</p>
                                <p className="text-xs theme-text-muted">{label}</p>
                            </div>
                        ))}
                    </div>

                    {data.rows.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">
                            Nobody needs moving into {data.sessionName}. Everyone is already in this session.
                        </div>
                    ) : (
                        <>
                            <div className="card p-4">
                                <p className="font-semibold mb-2">What will happen</p>
                                <ul className="text-sm grid md:grid-cols-2 gap-x-8 gap-y-1">
                                    {groups.map(([label, count]) => (
                                        <li key={label} className="flex justify-between"><span>{label}</span><span className="font-medium">{count}</span></li>
                                    ))}
                                </ul>
                            </div>

                            <div className="card overflow-hidden">
                                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold flex justify-between items-center gap-4">
                                    <span>Students ({data.rows.length}). Untick anyone who should not move.</span>
                                    <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search…"
                                           className="rounded px-2 py-1 text-sm text-black w-48" />
                                </div>
                                <div className="max-h-96 overflow-y-auto">
                                    <table className="w-full text-sm text-left">
                                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs sticky top-0">
                                        <tr>
                                            <th className="px-4 py-2">Move</th>
                                            <th className="px-4 py-2">Matric</th>
                                            <th className="px-4 py-2">Name</th>
                                            <th className="px-4 py-2">Programme</th>
                                            <th className="px-4 py-2">Level</th>
                                            <th className="px-4 py-2">Note</th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y theme-border">
                                        {shown.map((r) => (
                                            <tr key={r.studentId} className={skip[r.studentId] ? 'opacity-50' : ''}>
                                                <td className="px-4 py-2">
                                                    <input type="checkbox" checked={!skip[r.studentId]}
                                                           onChange={(e) => setSkip({ ...skip, [r.studentId]: !e.target.checked })} />
                                                </td>
                                                <td className="px-4 py-2">{r.matricNumber}</td>
                                                <td className="px-4 py-2">{r.fullName}</td>
                                                <td className="px-4 py-2">{r.programme}</td>
                                                <td className="px-4 py-2">{r.fromLevel} → {r.toLevel}</td>
                                                <td className="px-4 py-2 text-xs theme-text-muted">{r.note}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <button onClick={handleApply} disabled={busy || moving === 0}
                                    className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                                {busy ? 'Working…' : `Move ${moving} student(s) into ${data.sessionName}`}
                            </button>
                        </>
                    )}
                </div>
            )}
        </div>
    )
}

export default StartNewSession