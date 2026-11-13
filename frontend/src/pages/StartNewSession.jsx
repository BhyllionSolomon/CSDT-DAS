import { useEffect, useMemo, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import {
    previewSessionStart, applySessionStart, previewCorrection, applyCorrection,
} from '../services/sessionStartService'

const msg = (err) => err.response?.data?.message || err.message

function Tile({ label, value, tone }) {
    return (
        <div className="card p-4">
            <p className={`text-2xl font-bold ${tone || ''}`}>{value}</p>
            <p className="text-xs theme-text-muted">{label}</p>
        </div>
    )
}

function StartNewSession() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [data, setData] = useState(null)
    const [skip, setSkip] = useState({})
    const [query, setQuery] = useState('')
    const [fromId, setFromId] = useState('')
    const [toId, setToId] = useState('')
    const [count, setCount] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then((s) => setSessions([...s].sort((a, b) => String(b.startDate).localeCompare(String(a.startDate)))))
            .catch((e) => setError(msg(e)))
    }, [])

    async function load(id = sessionId) {
        setError(null)
        const preview = await previewSessionStart(id)
        setData(preview)
        setSkip(Object.fromEntries(preview.rows.filter((r) => r.action === 'GRADUATE' && r.outstanding > 0).map((r) => [r.studentId, true])))

        // Suggest the correction: from the session most students are wrongly recorded in, to the session just before this one.
        const wrong = preview.stored.filter((s) => s.blocked && s.session !== 'No session recorded').sort((a, b) => b.students - a.students)[0]
        const from = wrong ? sessions.find((s) => s.name === wrong.session) : null
        const index = sessions.findIndex((s) => String(s.id) === String(id))
        const before = index >= 0 ? sessions[index + 1] : null
        setFromId(from ? String(from.id) : '')
        setToId(before ? String(before.id) : '')
        setCount(null)
    }

    async function preview() {
        setBusy(true); setData(null); setNotice(null)
        try { await load() } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    useEffect(() => {
        if (!fromId || !toId || fromId === toId) { setCount(null); return }
        previewCorrection(fromId, toId).then(setCount).catch(() => setCount(null))
    }, [fromId, toId])

    async function correct() {
        if (!window.confirm(`Change the recorded session of ${count.students} student(s) from ${count.from} to ${count.to}?\n\nTheir levels do not change. Do this only if ${count.from} is a mistake and they really are in ${count.to}.`)) return
        setBusy(true); setError(null)
        try {
            const result = await applyCorrection(fromId, toId)
            setNotice(result.message)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const movable = data ? data.rows.filter((r) => r.action !== 'BLOCKED') : []
    const selected = movable.filter((r) => !skip[r.studentId])

    const groups = useMemo(() => {
        if (!data) return []
        const map = new Map()
        data.rows.filter((r) => r.action !== 'BLOCKED').forEach((r) => {
            const key = `${r.programme}|${r.fromLevel}|${r.toLevel}`
            map.set(key, { programme: r.programme, from: r.fromLevel, to: r.toLevel, count: (map.get(key)?.count || 0) + 1 })
        })
        return [...map.values()].sort((a, b) => a.programme.localeCompare(b.programme) || a.from.localeCompare(b.from))
    }, [data])

    const shown = movable.filter((r) => !query.trim() || `${r.matricNumber} ${r.fullName}`.toLowerCase().includes(query.trim().toLowerCase()))

    async function start() {
        const skipped = Object.keys(skip).filter((id) => skip[id]).map(Number)
        if (!window.confirm(`Start ${data.sessionName}?\n\n${selected.filter((r) => r.action === 'MOVE').length} student(s) move up one level and ${selected.filter((r) => r.action === 'GRADUATE').length} graduate.\n\nEach student's current level is kept on record for the session they are leaving.`)) return
        setBusy(true); setError(null)
        try {
            const result = await applySessionStart(Number(sessionId), skipped)
            setNotice(result.message)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Start New Session</h2>
            <p className="theme-text-muted text-sm mb-6">
                Every active student moves up <b>one</b> level, and 400-level students graduate. You see exactly what will happen before anything changes,
                and each student's current level stays on record for the session they leave.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="card p-4 mb-6 flex gap-3 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session you are starting</label>
                    <select value={sessionId} onChange={(e) => { setSessionId(e.target.value); setData(null) }} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a session…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <button onClick={preview} disabled={busy || !sessionId} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy && !data ? 'Checking…' : 'Preview'}
                </button>
            </div>

            {data && (
                <>
                    {data.warnings.length > 0 && (
                        <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">
                            <p className="font-semibold mb-1">Fix these session dates first. Starting is switched off until you do.</p>
                            <ul>{data.warnings.map((w, i) => <li key={i}>• {w}</li>)}</ul>
                        </div>
                    )}

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                        <Tile label="will move up one level" value={data.toMove} />
                        <Tile label="will graduate" value={data.toGraduate} />
                        <Tile label="cannot be moved" value={data.blocked} tone={data.blocked ? 'text-red-600' : ''} />
                        <Tile label="already in this session" value={data.alreadyOnSession} />
                    </div>

                    <div className="card overflow-hidden mb-6">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Where your students are recorded now</div>
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                            <tr><th className="px-4 py-2">Recorded session</th><th className="px-4 py-2">Students</th><th className="px-4 py-2">Sessions behind</th><th className="px-4 py-2">Result</th></tr>
                            </thead>
                            <tbody className="divide-y theme-border">
                            {data.stored.map((s) => (
                                <tr key={s.session}>
                                    <td className="px-4 py-2 font-medium">{s.session}</td>
                                    <td className="px-4 py-2">{s.students}</td>
                                    <td className="px-4 py-2">{s.steps || '—'}</td>
                                    <td className="px-4 py-2">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${s.blocked ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                        {s.blocked ? 'Cannot be moved' : 'Fine'}
                      </span>
                                    </td>
                                </tr>
                            ))}
                            {data.stored.length === 0 && <tr><td colSpan={4} className="px-4 py-4 theme-text-muted">No active student is waiting to be moved.</td></tr>}
                            </tbody>
                        </table>
                    </div>

                    {data.blocked > 0 && (
                        <div className="card p-4 mb-6" style={{ borderColor: '#fca5a5' }}>
                            <p className="font-semibold mb-1">{data.blocked} student(s) are recorded in the wrong session</p>
                            <p className="text-sm theme-text-muted mb-3">
                                They are more than one session behind, or have no session. Moving them would skip levels, so they are left alone.
                                If the recorded session is simply a mistake, correct it here. Their levels do not change.
                            </p>
                            <div className="flex gap-3 flex-wrap items-end">
                                <div>
                                    <label className="block text-xs font-medium mb-1">Recorded in (wrong)</label>
                                    <select value={fromId} onChange={(e) => setFromId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="">Choose…</option>
                                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-medium mb-1">Really in (correct)</label>
                                    <select value={toId} onChange={(e) => setToId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="">Choose…</option>
                                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                                    </select>
                                </div>
                                <button onClick={correct} disabled={busy || !count || count.students === 0}
                                        className="border theme-border rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                    Correct {count ? `${count.students} student(s)` : ''}
                                </button>
                            </div>
                            <p className="text-xs theme-text-muted mt-2">If the students with no session are the problem, place them first on the Students Per Session page (step 2).</p>
                        </div>
                    )}

                    {movable.length > 0 && (
                        <>
                            <div className="card p-4 mb-4">
                                <p className="font-semibold mb-2">What will happen</p>
                                <ul className="text-sm space-y-1">
                                    {groups.map((g) => (
                                        <li key={`${g.programme}${g.from}${g.to}`}>
                                            • {g.programme}: {g.from} → {g.to} <span className="theme-text-muted">· {g.count}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>

                            <div className="card overflow-hidden mb-4">
                                <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 flex-wrap text-sm">
                                    <span className="font-semibold">{selected.length} of {movable.length} student(s) selected</span>
                                    <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search" className="rounded-lg px-3 py-1 text-sm text-black w-48" />
                                </div>
                                <div className="max-h-96 overflow-y-auto">
                                    <table className="w-full text-sm text-left">
                                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs sticky top-0">
                                        <tr><th className="px-3 py-2"></th><th className="px-3 py-2">Student</th><th className="px-3 py-2">Programme</th><th className="px-3 py-2">Level change</th><th className="px-3 py-2">Note</th></tr>
                                        </thead>
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

                            <button onClick={start} disabled={busy || data.warnings.length > 0 || selected.length === 0}
                                    className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                                {busy ? 'Working…' : `Start ${data.sessionName}`}
                            </button>
                        </>
                    )}
                </>
            )}
        </div>
    )
}

export default StartNewSession