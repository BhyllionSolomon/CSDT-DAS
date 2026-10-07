import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Printer } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { getBroadsheet, getStanding } from '../services/reportService'
import api from '../services/api'

const LEVEL_CODES = ['100', '200', '300', '400']

const TABS = [
    { key: 'broadsheet', label: 'Results Broadsheet' },
    { key: 'carryover', label: 'Carry-Over Students' },
    { key: 'good', label: 'Good Standing' },
    { key: 'notgood', label: 'Not in Good Standing' },
    { key: 'withdrawal', label: 'Withdrawal List' },
]

const th = 'border border-black px-1.5 py-1 text-center font-bold align-middle'
const td = 'border border-black px-1.5 py-0.5 text-center'

const fmt2 = (v) => (v === null || v === undefined ? '-' : Number(v).toFixed(2))

function printLandscape() {
    const style = document.createElement('style')
    style.media = 'print'
    style.textContent = '@page { size: A4 landscape; margin: 8mm; }'
    document.head.appendChild(style)
    window.print()
    setTimeout(() => style.remove(), 500)
}

function TotalsCells({ t }) {
    if (!t) {
        return <>{[0, 1, 2, 3, 4].map((i) => <td key={i} className={td}>-</td>)}</>
    }
    return (
        <>
            <td className={td}>{t.tuo}</td>
            <td className={td}>{t.tup}</td>
            <td className={td}>{t.tuf}</td>
            <td className={td}>{fmt2(t.twgp)}</td>
            <td className={td}>{fmt2(t.gpa)}</td>
        </>
    )
}

function BroadsheetView({ data }) {
    const today = new Date().toLocaleDateString('en-GB')

    return (
        <div>
            <div className="bg-white text-black p-4 rounded-lg print-area">
                <div className="text-center mb-3">
                    <p className="font-bold text-base">KOLADAISI UNIVERSITY, IBADAN</p>
                    <p className="font-semibold text-sm">COMPREHENSIVE ACADEMIC RESULTS BROADSHEET</p>
                    <p className="text-xs">
                        Department of Computing Science &amp; Digital Technology | Programme: {data.programmeName} |
                        Level: {data.levelName} | Session: {data.sessionName} |
                        Semester: {data.semester === 'FIRST' ? 'First' : 'Second'}
                    </p>
                </div>

                <div className="overflow-x-auto print:overflow-visible">
                    <table className="w-full border-collapse text-[11px]">
                        <thead>
                        <tr>
                            <th rowSpan={2} className={th}>No</th>
                            <th rowSpan={2} className={th}>Student Name</th>
                            <th rowSpan={2} className={th}>Matric No.</th>
                            {data.courses.map((c) => (
                                <th key={c.code} rowSpan={2} className={`${th} px-1`}>
                                    <div style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)', whiteSpace: 'nowrap', margin: '0 auto' }}>
                                        {c.code} ({c.unit}) ({c.status})
                                    </div>
                                </th>
                            ))}
                            <th colSpan={5} className={th}>PREVIOUS</th>
                            <th colSpan={5} className={th}>CURRENT</th>
                            <th rowSpan={2} className={th}>CGPA</th>
                            <th rowSpan={2} className={th}>REMARK</th>
                        </tr>
                        <tr>
                            {['TUO', 'TUP', 'TUF', 'TWGP', 'GPA', 'TUO', 'TUP', 'TUF', 'TWGP', 'GPA'].map((h, i) => (
                                <th key={i} className={th}>{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {data.rows.map((r) => (
                            <tr key={r.studentId}>
                                <td className={td}>{r.no}</td>
                                <td className={`${td} text-left whitespace-nowrap`}>{r.fullName}</td>
                                <td className={td}>{r.matricNumber}</td>
                                {data.courses.map((c) => (
                                    <td key={c.code} className={td}>
                                        {r.scores[c.code] !== undefined && r.scores[c.code] !== null ? Number(r.scores[c.code]) : '-'}
                                    </td>
                                ))}
                                <TotalsCells t={r.previous} />
                                <TotalsCells t={r.current} />
                                <td className={`${td} font-bold text-green-700`}>{fmt2(r.cgpa)}</td>
                                <td className={`${td} font-bold text-left ${r.remark.startsWith('GS') ? 'text-green-700' : 'text-red-600'}`}>
                                    {r.remark}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                {data.rows.length === 0 && (
                    <p className="text-center text-sm py-6">No approved results found for this selection.</p>
                )}

                <p className="text-[10px] mt-3">
                    Key: TUO – Total Units Offered; TUP – Total Units Passed; TUF – Total Units Failed;
                    TWGP – Total Weighted Grade Point; GPA – Grade Point Average; CGPA – Cumulative GPA.
                    Course status: C – Compulsory, R – Required, E – Elective.
                    A dash (-) means no approved score is on record.
                </p>

                <div className="grid grid-cols-2 gap-16 mt-10 text-xs">
                    <div><p className="border-t border-black pt-1 text-center">HOD Signature</p><p className="text-center">Date: {today}</p></div>
                    <div><p className="border-t border-black pt-1 text-center">Dean Signature</p><p className="text-center">Date: {today}</p></div>
                </div>
            </div>

            {data.studentsWithoutResults.length > 0 && (
                <div className="no-print card p-4 mt-4 text-sm">
                    <p className="font-semibold mb-2">Not on this broadsheet ({data.studentsWithoutResults.length})</p>
                    <p className="theme-text-muted text-xs mb-2">
                        These students belong to this level and session but have no approved result for this semester.
                        Approve their results (Result Verification) and they will appear here automatically.
                    </p>
                    <ul className="space-y-1">
                        {data.studentsWithoutResults.map((s) => (
                            <li key={s.studentId}>
                                {s.matricNumber} — {s.fullName} <span className="theme-text-muted">· {s.note}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    )
}

function StandingView({ tab, data, threshold }) {
    const cutoff = Number(threshold)

    const byCgpaAsc = (list) => [...list].sort((a, b) => Number(a.cgpa) - Number(b.cgpa))

    const config = {
        carryover: {
            title: 'Students with Carry-Over Courses',
            list: data.rows.filter((r) => r.outstandingCourses.length > 0),
        },
        good: {
            title: 'Students in Good Standing',
            list: data.rows.filter((r) => r.standing === 'GS'),
        },
        notgood: {
            title: 'Students Not in Good Standing',
            list: byCgpaAsc(data.rows.filter((r) => r.standing === 'NGS')),
        },
        withdrawal: {
            title: `Students Due for Withdrawal (CGPA below ${Number.isFinite(cutoff) ? cutoff.toFixed(2) : threshold})`,
            list: byCgpaAsc(data.rows.filter((r) => Number(r.cgpa) < cutoff)),
        },
    }[tab]

    return (
        <div>
            <div className="bg-white text-black p-4 rounded-lg print-area">
                <div className="text-center mb-4">
                    <p className="font-bold text-base">KOLADAISI UNIVERSITY, IBADAN</p>
                    <p className="font-semibold text-sm">{config.title.toUpperCase()}</p>
                    <p className="text-xs">
                        Department of Computing Science &amp; Digital Technology | Programme: {data.programmeName} |
                        Level: {data.levelName} | Session: {data.sessionName} | Total: {config.list.length}
                    </p>
                </div>

                <table className="w-full border-collapse text-xs">
                    <thead>
                    <tr>
                        <th className={th}>S/N</th>
                        <th className={th}>Matric No.</th>
                        <th className={th}>Name</th>
                        <th className={th}>CGPA</th>
                        <th className={th}>Class</th>
                        <th className={th}>Outstanding Courses</th>
                    </tr>
                    </thead>
                    <tbody>
                    {config.list.map((r, i) => (
                        <tr key={r.studentId}>
                            <td className={td}>{i + 1}</td>
                            <td className={td}>
                                <Link to={`/student-report?studentId=${r.studentId}`}>{r.matricNumber}</Link>
                            </td>
                            <td className={`${td} text-left`}>{r.fullName}</td>
                            <td className={`${td} font-bold`}>{fmt2(r.cgpa)}</td>
                            <td className={td}>{r.degreeClass}</td>
                            <td className={`${td} text-left`}>{r.outstandingCourses.length ? r.outstandingCourses.join(', ') : '—'}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                {config.list.length === 0 && <p className="text-center text-sm py-6">No students in this list.</p>}

                <p className="text-[10px] mt-3">
                    Standing is taken from each student's cumulative record at the latest approved semester of the selected session.
                    Good Standing = CGPA of 1.50 and above.
                </p>
            </div>

            {data.studentsWithoutResults.length > 0 && (
                <div className="no-print card p-4 mt-4 text-sm">
                    <p className="font-semibold mb-2">Not included — no approved results ({data.studentsWithoutResults.length})</p>
                    <ul className="space-y-1">
                        {data.studentsWithoutResults.map((s) => (
                            <li key={s.studentId}>{s.matricNumber} — {s.fullName} <span className="theme-text-muted">· {s.note}</span></li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    )
}

function Reports() {
    const { user } = useAuth()
    const isAdviser = user.role === 'LEVEL_ADVISER'

    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [tab, setTab] = useState('broadsheet')
    const [programmeId, setProgrammeId] = useState(isAdviser && user.programmeId ? String(user.programmeId) : '')
    const [levelId, setLevelId] = useState('')
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [threshold, setThreshold] = useState('1.00')

    const [broadsheet, setBroadsheet] = useState(null)
    const [standing, setStanding] = useState(null)
    const [generating, setGenerating] = useState(false)

    useEffect(() => {
        Promise.all([api.get('/programmes'), api.get('/levels'), getAllSessions()])
            .then(([p, l, s]) => { setProgrammes(p.data); setLevels(l.data); setSessions(s) })
            .catch((err) => setError(err.response?.data?.message || err.message))
            .finally(() => setLoading(false))
    }, [])

    useEffect(() => { setBroadsheet(null) }, [programmeId, levelId, sessionId, semester])
    useEffect(() => { setStanding(null) }, [programmeId, levelId, sessionId])

    const visibleProgrammes = isAdviser ? programmes.filter((p) => String(p.id) === programmeId) : programmes

    async function handleGenerate() {
        if (!programmeId || !levelId || !sessionId) return
        setError(null)
        setGenerating(true)
        try {
            if (tab === 'broadsheet') {
                setBroadsheet(await getBroadsheet(programmeId, levelId, sessionId, semester))
            } else {
                setStanding(await getStanding(programmeId, levelId, sessionId))
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setGenerating(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const hasData = tab === 'broadsheet' ? broadsheet : standing

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Reports</h2>
            <p className="theme-text-muted text-sm mb-6">
                Choose a programme, level and session, then generate. Everything is calculated from approved
                results — nothing is typed in, so the figures cannot drift.
            </p>

            {error && <div className="no-print mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="no-print flex flex-wrap gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {TABS.map((t) => (
                    <button key={t.key} onClick={() => setTab(t.key)}
                            className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${
                                tab === t.key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                            }`}>
                        {t.label}
                    </button>
                ))}
            </div>

            <div className="no-print card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Programme</label>
                    <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)} disabled={isAdviser}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {visibleProgrammes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Level</label>
                    <select value={levelId} onChange={(e) => setLevelId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>

                {tab === 'broadsheet' && (
                    <div>
                        <label className="block text-sm font-medium mb-1">Semester</label>
                        <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="FIRST">First</option>
                            <option value="SECOND">Second</option>
                        </select>
                    </div>
                )}

                {tab === 'withdrawal' && (
                    <div>
                        <label className="block text-sm font-medium mb-1">Withdraw if CGPA below</label>
                        <input type="number" step="0.01" min="0" max="5" value={threshold} onChange={(e) => setThreshold(e.target.value)}
                               className="border theme-border rounded-lg px-3 py-2 text-sm w-24" />
                    </div>
                )}

                <button onClick={handleGenerate} disabled={!programmeId || !levelId || !sessionId || generating}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {generating ? 'Generating…' : 'Generate'}
                </button>

                {hasData && (
                    <button onClick={printLandscape}
                            className="flex items-center gap-2 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                        <Printer size={15} /> Print
                    </button>
                )}
            </div>

            {tab === 'withdrawal' && (
                <p className="no-print text-xs theme-text-muted mb-4">
                    The withdrawal cut-off above is a setting, not a built-in rule. Set it to match the department/senate
                    policy before relying on this list.
                </p>
            )}

            {tab === 'broadsheet' && broadsheet && <BroadsheetView data={broadsheet} />}
            {tab !== 'broadsheet' && standing && <StandingView tab={tab} data={standing} threshold={threshold} />}
        </div>
    )
}

export default Reports