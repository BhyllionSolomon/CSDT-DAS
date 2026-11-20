import { useEffect, useMemo, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import api from '../services/api'
import { getCoverage, verifyCohort, checkStudentList } from '../services/cohortService'

const msg = (err) => err.response?.data?.message || err.message
const FIRST = 2016
const LEVELS = [100, 200, 300, 400]
const yearOfName = (name) => { const m = /^\s*(\d{4})/.exec(name || ''); return m ? Number(m[1]) : null }
const full = (y) => `${y}/${y + 1}`
const short = (y) => `${y}/${String(y + 1).slice(2)}`

const STATUS = {
    COMPLETE: ['Complete', 'bg-green-100 text-green-700'],
    IMPORTED: ['Imported', 'bg-blue-100 text-blue-700'],
    PENDING: ['Pending', 'bg-slate-200 text-slate-600'],
}
const CATEGORY = {
    EXPLAINED: ['Explained', 'bg-blue-100 text-blue-700'],
    UNVERIFIED: ['Unverified', 'bg-red-100 text-red-700'],
    MISSING: ['Missing information', 'bg-amber-100 text-amber-700'],
    NOT_FOUND: ['Not in database', 'bg-red-100 text-red-700'],
}

// Reads matric numbers from a CSV or a pasted list. A column headed "matric" is used when there is one.
function parseMatrics(text) {
    const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
    if (lines.length === 0) return []
    const split = (l) => l.split(/[,;\t]/).map((c) => c.replace(/^"|"$/g, '').trim())
    let col = split(lines[0]).findIndex((c) => /matric/i.test(c))
    const start = col >= 0 ? 1 : 0
    if (col < 0) col = 0
    return [...new Set(lines.slice(start).map((l) => split(l)[col]).filter((v) => v && /[A-Za-z0-9]{4,}/.test(v)))]
}

function CohortGuide() {
    const [tab, setTab] = useState('guide')
    const [sessions, setSessions] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [error, setError] = useState(null)

    // guide
    const [picked, setPicked] = useState(null)
    // coverage
    const [intake, setIntake] = useState('')
    const [coverage, setCoverage] = useState(null)
    // check
    const [form, setForm] = useState({ sessionId: '', programmeId: '', level: '300', semester: 'FIRST' })
    const [text, setText] = useState('')
    const [report, setReport] = useState(null)
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        getAllSessions().then(setSessions).catch((e) => setError(msg(e)))
        api.get('/programmes').then((r) => setProgrammes(r.data)).catch(() => {})
    }, [])

    const { minYear, maxYear } = useMemo(() => {
        const years = sessions.map((s) => yearOfName(s.name)).filter(Boolean)
        return { minYear: Math.min(FIRST, ...(years.length ? years : [FIRST])), maxYear: Math.max(2026, ...years) }
    }, [sessions])

    const rows = useMemo(() => {
        const out = []
        for (let y = minYear; y <= maxYear; y++) out.push(y)
        return out
    }, [minYear, maxYear])

    const cohortAt = (sessionYear, level) => {
        const iy = sessionYear - (level / 100 - 1)
        return iy < minYear ? null : iy
    }

    async function loadCoverage(year) {
        setIntake(year); setCoverage(null); setError(null)
        if (!year) return
        try { setCoverage(await getCoverage(Number(year))) } catch (err) { setError(msg(err)) }
    }

    async function toggle(cell, kind, verified) {
        try {
            await verifyCohort(coverage.intakeYear, cell.sessionYear, kind, verified)
            setCoverage(await getCoverage(coverage.intakeYear))
        } catch (err) { setError(msg(err)) }
    }

    async function readFile(e) {
        const file = e.target.files?.[0]
        e.target.value = ''
        if (file) setText(await file.text())
    }

    async function check() {
        setError(null); setReport(null); setBusy(true)
        try {
            setReport(await checkStudentList({
                sessionId: Number(form.sessionId), programmeId: Number(form.programmeId), levelNumber: Number(form.level),
                semester: form.semester, matricNumbers: parseMatrics(text),
            }))
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const selectedYear = picked ? yearOfName(picked) : null
    const guideSession = form.sessionId ? sessions.find((s) => String(s.id) === form.sessionId) : null
    const tabs = [['guide', 'Guide'], ['coverage', 'Coverage'], ['check', 'Check a student list']]

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Cohort Guide</h2>
            <p className="theme-text-muted text-sm mb-6">
                Which admission cohort normally sits at each level in each session. It describes the normal four-year path, not proof that every student followed it.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="flex gap-1 mb-6 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {tabs.map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)}
                            className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>{label}</button>
                ))}
            </div>

            {tab === 'guide' && (
                <>
                    <div className="card overflow-hidden mb-6">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Academic progression, viewed from each session (click a session)</div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr><th className="px-4 py-2">Session</th>{LEVELS.map((l) => <th key={l} className="px-4 py-2">{l} Level</th>)}</tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {rows.map((y) => (
                                    <tr key={y} onClick={() => setPicked(full(y))} className="cursor-pointer"
                                        style={picked === full(y) ? { backgroundColor: 'var(--surface-soft)' } : undefined}>
                                        <td className="px-4 py-2 font-semibold">{full(y)}</td>
                                        {LEVELS.map((l) => {
                                            const c = cohortAt(y, l)
                                            return <td key={l} className={`px-4 py-2 ${c ? '' : 'theme-text-muted'}`}>{c ? `${short(c)} intake` : '—'}</td>
                                        })}
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <p className="px-4 py-3 text-xs theme-text-muted">
                            A dash means that cohort had not been admitted yet. The table grows by itself as new sessions are created. Cohorts that have finished 400 level no longer appear here, and their records stay available.
                        </p>
                    </div>

                    {selectedYear && (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">In {picked}</div>
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr><th className="px-4 py-2">Level</th><th className="px-4 py-2">Expected admission cohort</th></tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {LEVELS.map((l) => {
                                    const c = cohortAt(selectedYear, l)
                                    return (
                                        <tr key={l}>
                                            <td className="px-4 py-2 font-medium">{l} Level</td>
                                            <td className="px-4 py-2">{c ? `${full(c)} intake` : <span className="theme-text-muted">— (before the department started)</span>}</td>
                                        </tr>
                                    )
                                })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            {tab === 'coverage' && (
                <>
                    <div className="card p-4 mb-4 flex gap-3 items-end flex-wrap">
                        <div>
                            <label className="block text-sm font-medium mb-1">Admission cohort</label>
                            <select value={intake} onChange={(e) => loadCoverage(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Choose a cohort…</option>
                                {rows.map((y) => <option key={y} value={y}>{full(y)} intake</option>)}
                            </select>
                        </div>
                        <p className="text-xs theme-text-muted pb-2">Only real records are counted. Estimated ones never are.</p>
                    </div>

                    {coverage && (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">
                                {coverage.intake} intake{coverage.cohortSize ? ` · ${coverage.cohortSize} student(s) on record at 100 level` : ''}
                            </div>
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr><th className="px-4 py-2">Session</th><th className="px-4 py-2">Expected level</th><th className="px-4 py-2">Student list</th><th className="px-4 py-2">Results</th></tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {coverage.cells.map((c) => (
                                    <tr key={c.sessionYear}>
                                        <td className="px-4 py-3 font-medium">{c.session}{!c.sessionId && <span className="block text-[11px] text-amber-600 font-normal">session not created yet</span>}</td>
                                        <td className="px-4 py-3">{c.level} Level</td>
                                        {[['STUDENTS', c.studentsStatus, c.students], ['RESULTS', c.resultsStatus, c.results]].map(([kind, status, count]) => (
                                            <td key={kind} className="px-4 py-3">
                                                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS[status][1]}`}>{STATUS[status][0]}</span>
                                                <span className="text-xs theme-text-muted ml-2">{count} student(s)</span>
                                                {c.sessionId && (count > 0 || status === 'COMPLETE') && (
                                                    <button onClick={() => toggle(c, kind, status !== 'COMPLETE')} className="block text-[11px] mt-1 underline" style={{ color: 'var(--accent)' }}>
                                                        {status === 'COMPLETE' ? 'Remove verification' : 'Mark verified complete'}
                                                    </button>
                                                )}
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            {tab === 'check' && (
                <>
                    <div className="card p-4 mb-4 space-y-3">
                        <div className="flex gap-3 flex-wrap">
                            <select value={form.sessionId} onChange={(e) => setForm({ ...form, sessionId: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Session…</option>
                                {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                            </select>
                            <select value={form.programmeId} onChange={(e) => setForm({ ...form, programmeId: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Programme…</option>
                                {programmes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                            </select>
                            <select value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                {LEVELS.map((l) => <option key={l} value={l}>{l} Level</option>)}
                            </select>
                            <select value={form.semester} onChange={(e) => setForm({ ...form, semester: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="FIRST">First semester</option>
                                <option value="SECOND">Second semester</option>
                            </select>
                        </div>

                        {guideSession && (() => {
                            const y = yearOfName(guideSession.name)
                            const c = y ? cohortAt(y, Number(form.level)) : null
                            return (
                                <div className="px-4 py-3 rounded-lg bg-blue-50 text-blue-800 text-sm">
                                    {c ? `EXPECTED COHORT: Students admitted in ${full(c)} would normally occupy ${form.level} Level in ${guideSession.name}.`
                                        : `No cohort normally occupies ${form.level} Level in ${guideSession.name}.`}
                                </div>
                            )
                        })()}

                        <textarea value={text} onChange={(e) => setText(e.target.value)} rows={5}
                                  placeholder="Paste matric numbers here, one per line, or choose a CSV file below"
                                  className="w-full border theme-border rounded-lg px-3 py-2 text-sm font-mono" />
                        <div className="flex gap-3 items-center flex-wrap">
                            <input type="file" accept=".csv,.txt" onChange={readFile} className="text-sm" />
                            <button onClick={check} disabled={busy || !form.sessionId || !form.programmeId || !text.trim()}
                                    className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                {busy ? 'Checking…' : 'Check the list'}
                            </button>
                            <span className="text-xs theme-text-muted">{text.trim() ? `${parseMatrics(text).length} matric number(s) read` : 'CSV files and pasted lists work for now'}</span>
                        </div>
                    </div>

                    {report && (
                        <>
                            <div className="card p-4 mb-4">
                                <p className="text-sm font-semibold mb-1">{report.session} · {report.programme} · {report.level} · {report.semester === 'SECOND' ? 'Second' : 'First'} semester</p>
                                <p className="text-sm mb-3">{report.guidance}</p>
                                <div className="grid grid-cols-3 md:grid-cols-6 gap-3">
                                    {[['In the file', report.inFile], ['Match the cohort', report.expected], ['Explained exceptions', report.explained],
                                        ['Unverified mismatches', report.unverified], ['Missing admission info', report.missing], ['Not in database', report.notFound]].map(([label, n]) => (
                                        <div key={label} className="rounded-xl p-3" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                            <p className="text-xl font-bold">{n}</p><p className="text-[11px] theme-text-muted leading-tight">{label}</p>
                                        </div>
                                    ))}
                                </div>
                                <div className={`mt-4 px-4 py-3 rounded-lg text-sm font-medium ${report.ready ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
                                    {report.ready
                                        ? 'Ready to import. Every student matches the expected cohort or has an explained exception.'
                                        : 'Review needed before importing. Unverified, missing or unknown records must be resolved first.'}
                                </div>
                            </div>

                            {report.flagged.length > 0 && (
                                <div className="card overflow-hidden">
                                    <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{report.flagged.length} student(s) to look at</div>
                                    <ul className="divide-y theme-border">
                                        {report.flagged.map((r) => (
                                            <li key={r.matric} className="px-4 py-3 text-sm">
                                                <div className="flex items-center gap-3 flex-wrap">
                                                    <span className="font-mono">{r.matric}</span>
                                                    <span className="font-medium">{r.name}</span>
                                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${CATEGORY[r.category][1]}`}>{CATEGORY[r.category][0]}</span>
                                                </div>
                                                <p className="text-xs theme-text-muted mt-1">{r.reason}</p>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </>
                    )}
                </>
            )}
        </div>
    )
}

export default CohortGuide