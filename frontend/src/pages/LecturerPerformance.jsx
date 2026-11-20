import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getLecturerPerformance } from '../services/performanceService'

const BAND_COLORS = ['#16a34a', '#84cc16', '#eab308', '#f97316', '#c2410c', '#dc2626']
const msg = (err) => err.response?.data?.message || err.message
const fmt = (v, suffix = '') => (v == null ? '—' : `${v}${suffix}`)

function BandBar({ bands, labels }) {
    const total = bands.reduce((a, b) => a + b, 0)
    if (total === 0) return <span className="text-xs theme-text-muted">No scores yet</span>
    return (
        <div className="flex h-3 rounded-full overflow-hidden w-full min-w-[120px]">
            {bands.map((n, i) => n > 0 && (
                <div key={i} title={`${labels[i]}: ${n} student(s)`} style={{ width: `${(n / total) * 100}%`, backgroundColor: BAND_COLORS[i] }} />
            ))}
        </div>
    )
}

function Trend({ points }) {
    if (points.length < 2) {
        return <p className="text-xs theme-text-muted">The trend appears once there are results from two sessions.</p>
    }
    const w = 280, h = 80, pad = 14
    const xs = points.map((_, i) => pad + (i * (w - 2 * pad)) / (points.length - 1))
    const ys = points.map((p) => h - pad - ((p.average ?? 0) / 100) * (h - 2 * pad))
    return (
        <svg viewBox={`0 0 ${w} ${h}`} className="w-full max-w-xs">
            <polyline points={xs.map((x, i) => `${x},${ys[i]}`).join(' ')} fill="none" stroke="var(--accent)" strokeWidth="2" />
            {points.map((p, i) => (
                <g key={p.session}>
                    <circle cx={xs[i]} cy={ys[i]} r="3.5" fill="var(--accent)"><title>{`${p.session}: ${p.average} average, ${p.students} students`}</title></circle>
                    <text x={xs[i]} y={h - 2} fontSize="8" textAnchor="middle" fill="currentColor" opacity="0.6">{p.session}</text>
                </g>
            ))}
        </svg>
    )
}

function Ranking({ lecturers, deptAverage }) {
    const ranked = lecturers.filter((l) => l.average != null).sort((a, b) => b.average - a.average)
    if (ranked.length < 2) return null
    return (
        <div className="card p-4 mb-6">
            <p className="font-semibold mb-3">Average score by lecturer</p>
            <div className="space-y-2">
                {ranked.map((l) => (
                    <div key={l.lecturerId} className="flex items-center gap-3 text-sm">
                        <span className="w-44 shrink-0 truncate">{l.name}</span>
                        <div className="relative flex-1 h-3 rounded-full" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            <div className="h-full rounded-full accent-gradient" style={{ width: `${Math.min(l.average, 100)}%` }} />
                            {deptAverage != null && (
                                <div title={`Department average ${deptAverage}`} className="absolute top-[-3px] bottom-[-3px] w-0.5 bg-slate-500" style={{ left: `${Math.min(deptAverage, 100)}%` }} />
                            )}
                        </div>
                        <span className="w-12 text-right font-semibold">{l.average}</span>
                    </div>
                ))}
            </div>
            <p className="text-xs theme-text-muted mt-3">The grey line is the department average{deptAverage != null ? ` (${deptAverage})` : ''}.</p>
        </div>
    )
}

function LecturerPerformance() {
    const { user } = useAuth()
    const isHod = user.role === 'HOD' || user.role === 'ADMIN'

    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('ALL')
    const [data, setData] = useState(null)
    const [open, setOpen] = useState(new Set())
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions().then((s) => {
            setSessions(s)
            const current = currentSessionOf(s)
            if (current) setSessionId(String(current.id))
        })
    }, [])

    useEffect(() => {
        if (!sessionId) return
        setData(null); setError(null)
        getLecturerPerformance(sessionId, semester)
            .then((d) => {
                setData(d)
                setOpen(isHod ? new Set() : new Set(d.lecturers.map((l) => l.lecturerId)))
            })
            .catch((e) => setError(msg(e)))
    }, [sessionId, semester])

    function toggle(id) {
        const next = new Set(open)
        next.has(id) ? next.delete(id) : next.add(id)
        setOpen(next)
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">{isHod ? 'Lecturer Performance' : 'My Performance'}</h2>
            <p className="theme-text-muted text-sm mb-6">
                {isHod
                    ? 'How students scored in each lecturer\'s courses, from approved results.'
                    : 'How students scored in the courses you teach, from approved results. Only you and the H.O.D can see this.'}
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-4 flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => setSemester(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="ALL">Both</option>
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
            </div>

            {!data && !error && <p className="theme-text-muted">Loading…</p>}

            {data && (
                <>
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="card p-4"><p className="text-2xl font-bold">{data.departmentStudents}</p><p className="text-xs theme-text-muted">approved scores in the department</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold">{fmt(data.departmentAverage)}</p><p className="text-xs theme-text-muted">department average</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold">{fmt(data.departmentPassRate, '%')}</p><p className="text-xs theme-text-muted">department pass rate (40 and above)</p></div>
                    </div>

                    {data.lecturers.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">
                            {isHod
                                ? 'No lecturer has a course on the allocation list for this session and semester yet.'
                                : 'You have no course on the allocation list for this session and semester.'}
                        </div>
                    ) : (
                        <>
                            {isHod && <Ranking lecturers={data.lecturers} deptAverage={data.departmentAverage} />}

                            <div className="flex flex-wrap gap-3 text-xs theme-text-muted mb-3">
                                {data.bandLabels.map((l, i) => (
                                    <span key={l} className="flex items-center gap-1"><span className="w-3 h-3 rounded inline-block" style={{ backgroundColor: BAND_COLORS[i] }} /> {l}</span>
                                ))}
                            </div>

                            <div className="space-y-4">
                                {data.lecturers.map((l) => (
                                    <div key={l.lecturerId} className="card overflow-hidden">
                                        <button onClick={() => toggle(l.lecturerId)} className="w-full theme-sidebar text-white px-4 py-3 flex justify-between items-center gap-4 flex-wrap text-left">
                                            <span className="font-semibold text-sm">{l.name}</span>
                                            <span className="text-xs text-white/70">
                        {l.students} scores · average {fmt(l.average)} · pass rate {fmt(l.passRate, '%')} · results in {fmt(l.completion, '%')}
                      </span>
                                        </button>

                                        {open.has(l.lecturerId) && (
                                            <div className="p-4 space-y-4">
                                                <div className="overflow-x-auto">
                                                    <table className="w-full text-sm text-left">
                                                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                                        <tr>
                                                            <th className="px-3 py-2">Course</th><th className="px-3 py-2">Semester</th>
                                                            <th className="px-3 py-2">Registered</th><th className="px-3 py-2">Results in</th>
                                                            <th className="px-3 py-2">Average</th><th className="px-3 py-2">Pass rate</th>
                                                            <th className="px-3 py-2">High / Low</th><th className="px-3 py-2 w-48">Score bands</th>
                                                        </tr>
                                                        </thead>
                                                        <tbody className="divide-y theme-border">
                                                        {l.courses.map((c) => (
                                                            <tr key={`${c.courseId}-${c.semester}`}>
                                                                <td className="px-3 py-2 font-medium">{c.code} <span className="font-normal theme-text-muted">{c.title}</span></td>
                                                                <td className="px-3 py-2">{c.semester === 'FIRST' ? 'First' : 'Second'}</td>
                                                                <td className="px-3 py-2">{c.registered}</td>
                                                                <td className="px-3 py-2">{c.resulted}{c.completion != null ? ` (${c.completion}%)` : ''}</td>
                                                                <td className="px-3 py-2">{fmt(c.average)}</td>
                                                                <td className="px-3 py-2">{fmt(c.passRate, '%')}</td>
                                                                <td className="px-3 py-2">{fmt(c.highest)} / {fmt(c.lowest)}</td>
                                                                <td className="px-3 py-2"><BandBar bands={c.bands} labels={data.bandLabels} /></td>
                                                            </tr>
                                                        ))}
                                                        </tbody>
                                                    </table>
                                                </div>

                                                <div>
                                                    <p className="text-sm font-semibold mb-1">Average score by session</p>
                                                    <Trend points={l.trend} />
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </>
                    )}

                    <p className="text-xs theme-text-muted mt-6">{data.note}</p>
                </>
            )}
        </div>
    )
}

export default LecturerPerformance