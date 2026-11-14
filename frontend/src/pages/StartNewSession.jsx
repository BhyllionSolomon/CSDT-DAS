import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllSessions, createSession } from '../services/sessionService'
import { previewPromotion, applyPromotion } from '../services/sessionStartService'

const msg = (err) => err.response?.data?.message || err.message

function StartNewSession() {
    const [sessions, setSessions] = useState([])
    const [fromId, setFromId] = useState('')
    const [toId, setToId] = useState('')
    const [plan, setPlan] = useState(null)
    const [skip, setSkip] = useState({})
    const [showList, setShowList] = useState(false)
    const [query, setQuery] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function loadSessions() {
        const s = await getAllSessions()
        setSessions([...s].sort((a, b) => String(b.startDate).localeCompare(String(a.startDate))))
    }

    useEffect(() => { loadSessions().catch((e) => setError(msg(e))) }, [])

    // Moving into a session: the students come from the session just before it.
    function chooseTo(id) {
        setToId(id)
        const index = sessions.findIndex((s) => String(s.id) === id)
        setFromId(index >= 0 && sessions[index + 1] ? String(sessions[index + 1].id) : '')
    }

    async function refresh() {
        setPlan(null); setError(null)
        if (!fromId || !toId || fromId === toId) return
        try {
            const p = await previewPromotion(fromId, toId)
            setPlan(p)
            setSkip(Object.fromEntries(p.rows.filter((r) => r.action === 'GRADUATE' && r.outstanding > 0).map((r) => [r.studentId, true])))
        } catch (err) { setError(msg(err)) }
    }

    useEffect(() => { refresh() }, [fromId, toId])

    const selected = plan ? plan.rows.filter((r) => !skip[r.studentId]) : []
    const moving = selected.filter((r) => r.action === 'MOVE').length
    const graduating = selected.filter((r) => r.action === 'GRADUATE').length
    const shown = plan ? plan.rows.filter((r) => !query.trim() || `${r.matricNumber} ${r.fullName}`.toLowerCase().includes(query.trim().toLowerCase())) : []

    const lines = useMemo(() => (plan ? plan.lines : []), [plan])

    async function move() {
        const left = Object.keys(skip).filter((id) => skip[id]).map(Number)
        if (!window.confirm(`Move ${moving} student(s) into ${plan.to} and graduate ${graduating}?\n\nThe level each student leaves is kept on record for ${plan.from}.`)) return
        setBusy(true); setError(null); setNotice(null)
        try {
            const result = await applyPromotion(Number(fromId), Number(toId), left)
            setNotice(result.message)
            await refresh()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function createPast() {
        if (!window.confirm('Create the sessions 2016/2017 to 2025/2026 that do not exist yet? Existing sessions are left alone.')) return
        setBusy(true); setError(null); setNotice(null)
        const have = new Set(sessions.map((s) => s.name))
        let created = 0
        const failed = []
        for (let y = 2016; y <= 2026; y++) {
            const name = `${y}/${y + 1}`
            if (have.has(name)) continue
            try {
                await createSession(name, `${y}-10-01`, `${y + 1}-09-30`)
                created++
            } catch { failed.push(name) }
        }
        try { await loadSessions() } catch { /* ignore */ }
        setBusy(false)
        setNotice(`${created} session(s) created.${failed.length ? ` Could not create: ${failed.join(', ')}.` : ''}`)
    }

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Start New Session</h2>
            <p className="theme-text-muted text-sm mb-6">
                Choose the session you are moving into. Every student recorded in the session before it moves up <b>one</b> level, and 400-level students graduate.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    {notice} Next, upload the new 100-level students and place them in {plan?.to || 'the new session'} on{' '}
                    <Link to="/students-per-session" className="underline font-medium">Students Per Session</Link> (step 2).
                </div>
            )}

            <div className="card p-4 mb-6 flex gap-4 flex-wrap items-end">
                <div>
                    <label className="block text-sm font-medium mb-1">Moving into</label>
                    <select value={toId} onChange={(e) => chooseTo(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a session…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Students are in</label>
                    <select value={fromId} onChange={(e) => setFromId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a session…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
            </div>

            {plan && plan.warnings.length > 0 && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">
                    <ul className="space-y-1">{plan.warnings.map((w, i) => <li key={i}>• {w}</li>)}</ul>
                </div>
            )}

            {plan && plan.rows.length > 0 && (
                <>
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="card p-4"><p className="text-2xl font-bold">{moving}</p><p className="text-xs theme-text-muted">move up one level</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold">{graduating}</p><p className="text-xs theme-text-muted">graduate</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold">{plan.rows.length - selected.length}</p><p className="text-xs theme-text-muted">left behind (unticked)</p></div>
                    </div>

                    <div className="card p-4 mb-4">
                        <p className="font-semibold mb-2">What will happen</p>
                        <ul className="text-sm space-y-1">
                            {lines.map((l) => (
                                <li key={`${l.programme}${l.fromLevel}${l.toLevel}`}>
                                    • {l.programme}: {l.fromLevel} → {l.toLevel} <span className="theme-text-muted">· {l.count}</span>
                                </li>
                            ))}
                        </ul>
                        {plan.elsewhere.length > 0 && (
                            <ul className="text-xs theme-text-muted mt-3 space-y-1">{plan.elsewhere.map((n, i) => <li key={i}>{n}</li>)}</ul>
                        )}
                    </div>

                    <button onClick={move} disabled={busy || plan.warnings.length > 0 || selected.length === 0}
                            className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50 mb-3">
                        {busy ? 'Working…' : `Move ${selected.length} student(s) into ${plan.to}`}
                    </button>

                    <div className="mb-6">
                        <button onClick={() => setShowList(!showList)} className="text-sm underline" style={{ color: 'var(--accent)' }}>
                            {showList ? 'Hide the student list' : 'Show the student list (untick anyone you want to leave behind)'}
                        </button>

                        {showList && (
                            <div className="card overflow-hidden mt-3">
                                <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 text-sm">
                                    <span className="font-semibold">{selected.length} of {plan.rows.length} selected</span>
                                    <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search" className="rounded-lg px-3 py-1 text-sm text-black w-44" />
                                </div>
                                <div className="max-h-96 overflow-y-auto">
                                    <table className="w-full text-sm text-left">
                                        <tbody className="divide-y theme-border">
                                        {shown.map((r) => (
                                            <tr key={r.studentId}>
                                                <td className="px-3 py-2"><input type="checkbox" checked={!skip[r.studentId]} onChange={(e) => setSkip({ ...skip, [r.studentId]: !e.target.checked })} /></td>
                                                <td className="px-3 py-2">{r.matricNumber} <span className="font-medium">· {r.fullName}</span></td>
                                                <td className="px-3 py-2">{r.programme}</td>
                                                <td className="px-3 py-2">{r.fromLevel} → {r.toLevel}</td>
                                                <td className="px-3 py-2 text-xs theme-text-muted">{r.note}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </div>
                </>
            )}

            <div className="card p-4">
                <p className="font-semibold mb-1">Need the earlier sessions?</p>
                <p className="text-sm theme-text-muted mb-3">
                    To fill the past backwards (for example 400 level in 2025/2026 back to 100 level in 2022/2023), the earlier sessions must exist first.
                    This creates 2016/2017 to 2025/2026 in one click. Then go to <Link to="/students-per-session" className="underline" style={{ color: 'var(--accent)' }}>Students Per Session</Link>, step 4.
                </p>
                <button onClick={createPast} disabled={busy} className="border theme-border rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    Create sessions 2016/2017 to 2025/2026
                </button>
            </div>
        </div>
    )
}

export default StartNewSession