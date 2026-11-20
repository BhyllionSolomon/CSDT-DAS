import { useEffect, useState } from 'react'
import { Download } from 'lucide-react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import {
    getAttendanceCourses, getAttendanceClasses, getAttendanceRoster, saveAttendance,
    deleteAttendanceClass, getAttendanceSummary,
} from '../services/attendanceService'

const msg = (err) => err.response?.data?.message || err.message
const today = () => new Date().toISOString().slice(0, 10)

const STATUS = [
    ['PRESENT', 'P', 'Present', 'bg-green-600'],
    ['ABSENT', 'A', 'Absent', 'bg-red-600'],
    ['LATE', 'L', 'Late', 'bg-amber-500'],
    ['EXCUSED', 'E', 'Excused', 'bg-slate-500'],
]

function Attendance() {
    const [sel, setSel] = useState(null)
    const [courses, setCourses] = useState([])
    const [courseId, setCourseId] = useState('')
    const [tab, setTab] = useState('take')
    const [date, setDate] = useState(today())
    const [kind, setKind] = useState('LECTURE')
    const [topic, setTopic] = useState('')
    const [roster, setRoster] = useState(null)
    const [marks, setMarks] = useState({})
    const [classes, setClasses] = useState([])
    const [summary, setSummary] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    const ready = sel && courseId

    useEffect(() => {
        if (!sel) return
        setCourses([]); setCourseId(''); setError(null)
        getAttendanceCourses(sel.sessionId, sel.semester).then(setCourses).catch((e) => setError(msg(e)))
    }, [sel])

    async function loadSide() {
        const [c, s] = await Promise.all([
            getAttendanceClasses(courseId, sel.sessionId, sel.semester),
            getAttendanceSummary(courseId, sel.sessionId, sel.semester),
        ])
        setClasses(c); setSummary(s)
    }

    async function loadRoster() {
        const r = await getAttendanceRoster(courseId, sel.sessionId, sel.semester, date, kind)
        setRoster(r)
        setTopic(r.topic || '')
        setMarks(Object.fromEntries(r.students.map((s) => [s.studentId, s.status])))
    }

    useEffect(() => {
        if (!ready) return
        setRoster(null); setSummary(null); setError(null); setNotice(null)
        Promise.all([loadSide(), loadRoster()]).catch((e) => setError(msg(e)))
    }, [courseId, sel])

    useEffect(() => {
        if (!ready || !date) return
        loadRoster().catch((e) => setError(msg(e)))
    }, [date, kind])

    async function save() {
        setError(null); setNotice(null); setBusy(true)
        try {
            await saveAttendance({
                courseId: Number(courseId), sessionId: Number(sel.sessionId), semester: sel.semester,
                date, kind, topic, marks: Object.entries(marks).map(([studentId, status]) => ({ studentId: Number(studentId), status })),
            })
            setNotice(`Attendance saved for ${date}.`)
            await Promise.all([loadSide(), loadRoster()])
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function removeClass(c) {
        if (!window.confirm(`Delete the ${c.kind.toLowerCase()} of ${c.date}? Its attendance marks are removed.`)) return
        try { await deleteAttendanceClass(c.id); await Promise.all([loadSide(), loadRoster()]) } catch (err) { setError(msg(err)) }
    }

    function download() {
        const rows = [['Matric number', 'Name', 'Classes', 'Attended', 'Absent', 'Excused', 'Percent', 'Eligible']]
        summary.students.forEach((s) => rows.push([s.matricNumber, s.fullName, s.classes, s.present + s.late, s.absent, s.excused, s.percent ?? '', s.eligible ? 'Yes' : 'No']))
        const csv = rows.map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'attendance-summary.csv'
        a.click()
    }

    const counts = roster ? Object.values(marks).reduce((t, s) => ({ ...t, [s]: (t[s] || 0) + 1 }), {}) : {}

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Attendance</h2>
            <p className="theme-text-muted text-sm mb-6">Take attendance for your courses and see who is at risk of missing the attendance requirement.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <SessionSemesterBar onChange={setSel}>
                <div>
                    <label className="block text-sm font-medium mb-1">Course</label>
                    <select value={courseId} onChange={(e) => setCourseId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a course…</option>
                        {courses.map((c) => <option key={c.courseId} value={c.courseId}>{c.code} · {c.title} ({c.rosterSize})</option>)}
                    </select>
                </div>
            </SessionSemesterBar>

            {sel && courses.length === 0 && !error && (
                <div className="card p-6 text-sm theme-text-muted">You have no course on the allocation list for this session and semester.</div>
            )}

            {ready && (
                <>
                    <div className="flex gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        {[['take', 'Take attendance'], ['summary', 'Summary']].map(([key, label]) => (
                            <button key={key} onClick={() => setTab(key)}
                                    className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>{label}</button>
                        ))}
                    </div>

                    {tab === 'take' && (
                        <div className="space-y-6">
                            <div className="card p-4 flex gap-3 flex-wrap items-end">
                                <div>
                                    <label className="block text-xs font-medium mb-1">Date</label>
                                    <input type="date" max={today()} value={date} onChange={(e) => setDate(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm" />
                                </div>
                                <div>
                                    <label className="block text-xs font-medium mb-1">Class</label>
                                    <select value={kind} onChange={(e) => setKind(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="LECTURE">Lecture</option>
                                        <option value="PRACTICAL">Practical</option>
                                    </select>
                                </div>
                                <div className="flex-1 min-w-[180px]">
                                    <label className="block text-xs font-medium mb-1">Topic (optional)</label>
                                    <input value={topic} maxLength={150} onChange={(e) => setTopic(e.target.value)} className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                                </div>
                                {roster?.existing && <span className="text-xs px-3 py-1 rounded-full bg-blue-100 text-blue-700 pb-1">Already taken. Saving updates it.</span>}
                            </div>

                            {roster && roster.students.length === 0 && (
                                <div className="card p-6 text-sm theme-text-muted">No student has registered for this course this semester.</div>
                            )}

                            {roster && roster.students.length > 0 && (
                                <div className="card overflow-hidden">
                                    <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 flex-wrap text-sm">
                                        <span className="font-semibold">{roster.students.length} student(s)</span>
                                        <span className="text-xs text-white/70">
                      {STATUS.map(([k, , label]) => `${counts[k] || 0} ${label.toLowerCase()}`).join(' · ')}
                    </span>
                                        <button onClick={() => setMarks(Object.fromEntries(roster.students.map((s) => [s.studentId, 'PRESENT'])))}
                                                className="text-xs underline">Mark everyone present</button>
                                    </div>
                                    <ul className="divide-y theme-border">
                                        {roster.students.map((s) => (
                                            <li key={s.studentId} className="px-4 py-2 flex items-center gap-3 text-sm">
                                                <span className="w-36 shrink-0">{s.matricNumber}</span>
                                                <span className="flex-1 truncate">{s.fullName}</span>
                                                <div className="flex gap-1">
                                                    {STATUS.map(([key, letter, label, color]) => (
                                                        <button key={key} title={label} onClick={() => setMarks({ ...marks, [s.studentId]: key })}
                                                                className={`w-8 h-8 rounded-lg text-xs font-bold btn-press ${marks[s.studentId] === key ? `${color} text-white` : 'border theme-border theme-text-muted'}`}>
                                                            {letter}
                                                        </button>
                                                    ))}
                                                </div>
                                            </li>
                                        ))}
                                    </ul>
                                    <div className="p-4">
                                        <button onClick={save} disabled={busy} className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                            {busy ? 'Saving…' : 'Save attendance'}
                                        </button>
                                    </div>
                                </div>
                            )}

                            <div className="card overflow-hidden">
                                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Classes taken ({classes.length})</div>
                                {classes.length === 0 ? <p className="p-6 text-sm theme-text-muted">No class has been recorded yet.</p> : (
                                    <ul className="divide-y theme-border">
                                        {classes.map((c) => (
                                            <li key={c.id} className="px-4 py-2 flex items-center gap-3 text-sm">
                                                <button onClick={() => { setDate(c.date); setKind(c.kind); setTab('take') }} className="font-medium w-28 text-left" style={{ color: 'var(--accent)' }}>{c.date}</button>
                                                <span className="w-24">{c.kind === 'PRACTICAL' ? 'Practical' : 'Lecture'}</span>
                                                <span className="flex-1 truncate theme-text-muted">{c.topic || ''}</span>
                                                <span>{c.present}/{c.total} present</span>
                                                <button onClick={() => removeClass(c)} className="text-xs text-red-600 hover:underline">Delete</button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>
                    )}

                    {tab === 'summary' && summary && (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 flex-wrap text-sm">
                                <span className="font-semibold">{summary.classesHeld} class(es) held · {summary.required}% needed</span>
                                <button onClick={download} className="flex items-center gap-1 text-xs underline"><Download size={13} /> Download CSV</button>
                            </div>
                            {summary.students.length === 0 ? <p className="p-6 text-sm theme-text-muted">No student has registered for this course.</p> : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm text-left">
                                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                        <tr>
                                            <th className="px-4 py-2">Matric</th><th className="px-4 py-2">Name</th><th className="px-4 py-2">Classes</th>
                                            <th className="px-4 py-2">Attended</th><th className="px-4 py-2 w-48">Attendance</th><th className="px-4 py-2">Status</th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y theme-border">
                                        {summary.students.map((s) => (
                                            <tr key={s.studentId}>
                                                <td className="px-4 py-2">{s.matricNumber}</td>
                                                <td className="px-4 py-2 font-medium">{s.fullName}</td>
                                                <td className="px-4 py-2">{s.classes}</td>
                                                <td className="px-4 py-2">{s.present + s.late}</td>
                                                <td className="px-4 py-2">
                                                    {s.percent == null ? <span className="theme-text-muted">—</span> : (
                                                        <div className="flex items-center gap-2">
                                                            <div className="flex-1 h-2.5 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                                                <div className="h-full" style={{ width: `${Math.min(s.percent, 100)}%`, backgroundColor: s.eligible ? '#16a34a' : '#dc2626' }} />
                                                            </div>
                                                            <span className="w-10 text-right font-semibold">{s.percent}%</span>
                                                        </div>
                                                    )}
                                                </td>
                                                <td className="px-4 py-2">
                                                    {s.percent == null ? '' : (
                                                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${s.eligible ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                {s.eligible ? 'Eligible' : 'At risk'}
                              </span>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default Attendance