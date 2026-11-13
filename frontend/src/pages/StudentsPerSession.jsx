import { useEffect, useMemo, useState } from 'react'
import { Download } from 'lucide-react'
import { getAllSessions } from '../services/sessionService'
import {
    checkRecords, assignSession, syncRecords, estimateRecords, undoEstimate, recordsForSession,
} from '../services/sessionRecordsService'

const msg = (err) => err.response?.data?.message || err.message

function Step({ n, title, children }) {
    return (
        <div className="card p-4 mb-4">
            <p className="font-semibold mb-3"><span className="inline-flex w-6 h-6 rounded-full accent-gradient text-white text-xs items-center justify-center mr-2">{n}</span>{title}</p>
            {children}
        </div>
    )
}

function Tile({ label, value, tone }) {
    return (
        <div className="rounded-xl p-3" style={{ backgroundColor: 'var(--surface-soft)' }}>
            <p className={`text-xl font-bold ${tone || ''}`}>{value}</p>
            <p className="text-xs theme-text-muted">{label}</p>
        </div>
    )
}

function StudentsPerSession() {
    const [sessions, setSessions] = useState([])
    const [check, setCheck] = useState(null)
    const [fixId, setFixId] = useState('')
    const [report, setReport] = useState(null)
    const [estimate, setEstimate] = useState(null)
    const [viewId, setViewId] = useState('')
    const [rows, setRows] = useState(null)
    const [level, setLevel] = useState('ALL')
    const [query, setQuery] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        getAllSessions().then(setSessions).catch((e) => setError(msg(e)))
        checkRecords().then(setCheck).catch((e) => setError(msg(e)))
    }, [])

    async function act(action, after) {
        setError(null); setNotice(null); setBusy(true)
        try {
            const result = await action()
            if (after) after(result)
            setCheck(await checkRecords())
            if (viewId) setRows(await recordsForSession(viewId))
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function fix() {
        const name = sessions.find((s) => String(s.id) === fixId)?.name
        if (!window.confirm(`Place the ${check.noSession} student(s) who have no session into ${name}? Students who already have a session are not changed.`)) return
        act(() => assignSession(Number(fixId)), (r) => setNotice(r.message))
    }

    function sync() { act(syncRecords, setReport) }

    function previewEstimate() {
        setEstimate(null)
        act(() => estimateRecords(false), setEstimate)
    }

    function applyEstimate() {
        if (!window.confirm(`Create ${estimate.rows} estimated record(s)? They are marked Estimated, and you can undo them per session.`)) return
        act(() => estimateRecords(true), (r) => { setEstimate(null); setNotice(`${r.rows} estimated record(s) created.`) })
    }

    function undo() {
        const name = sessions.find((s) => String(s.id) === viewId)?.name
        if (!window.confirm(`Remove the estimated records of ${name}? Records from live data are not touched.`)) return
        act(() => undoEstimate(Number(viewId)), (r) => setNotice(r.message))
    }

    async function show(id) {
        setViewId(id); setRows(null); setLevel('ALL'); setQuery('')
        if (!id) return
        try { setRows(await recordsForSession(id)) } catch (err) { setError(msg(err)) }
    }

    const levels = useMemo(() => (rows ? [...new Set(rows.map((r) => r.level))].sort() : []), [rows])
    const shown = (rows || []).filter((r) =>
        (level === 'ALL' || r.level === level) &&
        (!query.trim() || `${r.matricNumber} ${r.fullName}`.toLowerCase().includes(query.trim().toLowerCase())))
    const estimated = rows ? rows.filter((r) => r.source === 'ESTIMATED').length : 0

    function download() {
        const head = ['Matric number', 'Name', 'Department', 'Programme', 'Level', 'Status', 'Source']
        const body = shown.map((r) => [r.matricNumber, r.fullName, r.department, r.programme, r.level, r.status || '', r.source])
        const csv = [head, ...body].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'students-in-session.csv'
        a.click()
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Students Per Session</h2>
            <p className="theme-text-muted text-sm mb-6">
                Which students were in which programme and level during each academic session. Records are only ever added. Nothing here deletes or changes another session's record.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <Step n="1" title="Check your data">
                {!check ? <p className="text-sm theme-text-muted">Checking…</p> : (
                    <>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
                            <Tile label="students" value={check.students} />
                            <Tile label="without a session" value={check.noSession} tone={check.noSession ? 'text-red-600' : ''} />
                            <Tile label="without a level" value={check.noLevel} tone={check.noLevel ? 'text-red-600' : ''} />
                            <Tile label="without a programme" value={check.noProgramme} tone={check.noProgramme ? 'text-red-600' : ''} />
                        </div>
                        <ul className="text-sm mb-3 space-y-1">
                            {check.findings.map((f, i) => <li key={i}>• {f}</li>)}
                        </ul>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr><th className="px-3 py-2">Session</th><th className="px-3 py-2">Students currently in it</th><th className="px-3 py-2">Saved records</th><th className="px-3 py-2">Missing</th></tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {check.sessions.map((s) => (
                                    <tr key={s.sessionId}>
                                        <td className="px-3 py-2 font-medium">{s.name}</td><td className="px-3 py-2">{s.studentsInSession}</td>
                                        <td className="px-3 py-2">{s.records}</td><td className={`px-3 py-2 ${s.missing ? 'text-red-600 font-semibold' : ''}`}>{s.missing}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </>
                )}
            </Step>

            {check && check.noSession > 0 && (
                <Step n="2" title={`Place the ${check.noSession} student(s) who have no session`}>
                    <div className="flex gap-3 flex-wrap items-end">
                        <select value={fixId} onChange={(e) => setFixId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Their current session…</option>
                            {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                        </select>
                        <button onClick={fix} disabled={busy || !fixId} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Place them</button>
                    </div>
                    <p className="text-xs theme-text-muted mt-2">Choose the session they are in right now (for example 2025/2026). Students who already have a session are never changed.</p>
                </Step>
            )}

            <Step n="3" title="Save session records">
                <button onClick={sync} disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Working…' : 'Save records now'}
                </button>
                <p className="text-xs theme-text-muted mt-2">Safe to run again and again. It saves each student's current session, level and programme, and never creates duplicates.</p>
                {report && (
                    <div className="mt-3">
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            <Tile label="created" value={report.created} tone="text-green-600" />
                            <Tile label="updated" value={report.updated} />
                            <Tile label="already up to date" value={report.unchanged} />
                            <Tile label="skipped" value={report.skipped} tone={report.skipped ? 'text-amber-600' : ''} />
                        </div>
                        {Object.entries(report.reasons).length > 0 && (
                            <ul className="text-sm mt-2">{Object.entries(report.reasons).map(([why, n]) => <li key={why}>• {n} skipped: {why}</li>)}</ul>
                        )}
                    </div>
                )}
            </Step>

            <Step n="4" title="Rebuild earlier sessions (estimate)">
                <p className="text-sm theme-text-muted mb-3">
                    Moves each student back one level for each earlier session, so for example a 400 level student in 2025/2026 appears at 100 level in 2022/2023.
                    It only uses sessions that exist, never goes below a direct-entry student's entry level, and marks every record <b>Estimated</b>.
                </p>
                <button onClick={previewEstimate} disabled={busy} className="border theme-border rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Preview</button>

                {estimate && (
                    <div className="mt-3">
                        {estimate.rows === 0 ? <p className="text-sm theme-text-muted">There is nothing to add right now.</p> : (
                            <>
                                <p className="text-sm font-medium mb-2">{estimate.rows} record(s) would be created:</p>
                                <ul className="text-sm mb-3">{estimate.sessions.map((l) => <li key={l.session}>• {l.session}: {l.rows}</li>)}</ul>
                                <button onClick={applyEstimate} disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Create these records</button>
                            </>
                        )}
                        <ul className="text-xs theme-text-muted mt-3 space-y-1">{estimate.notes.map((n, i) => <li key={i}>• {n}</li>)}</ul>
                    </div>
                )}
            </Step>

            <Step n="5" title="View a session">
                <div className="flex gap-3 flex-wrap items-end mb-3">
                    <select value={viewId} onChange={(e) => show(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a session…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                    {rows && (
                        <>
                            <select value={level} onChange={(e) => setLevel(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="ALL">All levels</option>
                                {levels.map((l) => <option key={l} value={l}>{l}</option>)}
                            </select>
                            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search name or matric number" className="border theme-border rounded-lg px-3 py-2 text-sm w-60" />
                            <button onClick={download} className="flex items-center gap-1.5 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press"><Download size={14} /> CSV</button>
                            {estimated > 0 && <button onClick={undo} disabled={busy} className="text-sm text-red-600 hover:underline">Undo the {estimated} estimated record(s)</button>}
                        </>
                    )}
                </div>

                {rows && rows.length === 0 && (
                    <p className="text-sm theme-text-muted">No records for this session yet. If students are in it, run steps 2 and 3. For earlier sessions, use step 4.</p>
                )}

                {rows && rows.length > 0 && (
                    <div className="overflow-x-auto">
                        <p className="text-xs theme-text-muted mb-2">{shown.length} student(s). Programme and level are as they were in this session, not as they are now.</p>
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                            <tr>
                                <th className="px-3 py-2">Matric</th><th className="px-3 py-2">Name</th><th className="px-3 py-2">Department</th>
                                <th className="px-3 py-2">Programme</th><th className="px-3 py-2">Level</th><th className="px-3 py-2">Status</th><th className="px-3 py-2">Source</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y theme-border">
                            {shown.map((r) => (
                                <tr key={r.studentId}>
                                    <td className="px-3 py-2">{r.matricNumber}</td><td className="px-3 py-2 font-medium">{r.fullName}</td>
                                    <td className="px-3 py-2">{r.department}</td><td className="px-3 py-2">{r.programme}</td>
                                    <td className="px-3 py-2">{r.level}</td><td className="px-3 py-2">{r.status || <span className="theme-text-muted">Not recorded</span>}</td>
                                    <td className="px-3 py-2">
                      <span title={r.note || ''} className={`text-xs px-2 py-0.5 rounded-full font-medium ${r.source === 'ESTIMATED' ? 'bg-amber-100 text-amber-700' : 'bg-green-100 text-green-700'}`}>
                        {r.source === 'ESTIMATED' ? 'Estimated' : 'Saved'}
                      </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </Step>
        </div>
    )
}

export default StudentsPerSession