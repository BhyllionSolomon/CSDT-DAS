import { useEffect, useState } from 'react'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getProgress } from '../services/studentProgressService'

const BAND_COLORS = ['#16a34a', '#84cc16', '#eab308', '#f97316', '#c2410c', '#dc2626']
const msg = (err) => err.response?.data?.message || err.message
const fmt = (v) => (v == null ? '—' : v)

const shortLabel = (p) => {
    const m = String(p.session).match(/^(\d{2})(\d{2})\/(\d{2})(\d{2})$/)
    return `${m ? `${m[2]}/${m[4]}` : p.session} S${p.semester}`
}

function Chart({ points }) {
    const W = 640, H = 240, L = 34, R = 14, T = 14, B = 40
    const n = points.length
    const x = (i) => (n === 1 ? (L + W - R) / 2 : L + (i * (W - L - R)) / (n - 1))
    const y = (v) => T + (1 - v / 5) * (H - T - B)
    const line = (key) => points.map((p, i) => (p[key] == null ? null : `${x(i)},${y(p[key])}`)).filter(Boolean).join(' ')

    return (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
            {[0, 1, 2, 3, 4, 5].map((g) => (
                <g key={g}>
                    <line x1={L} x2={W - R} y1={y(g)} y2={y(g)} stroke="currentColor" opacity="0.12" />
                    <text x={L - 6} y={y(g) + 3} fontSize="10" textAnchor="end" fill="currentColor" opacity="0.6">{g}</text>
                </g>
            ))}
            <polyline points={line('gpa')} fill="none" stroke="var(--accent)" strokeWidth="2.5" />
            <polyline points={line('cgpa')} fill="none" stroke="#f59e0b" strokeWidth="2.5" strokeDasharray="6 4" />
            {points.map((p, i) => (
                <g key={i}>
                    {p.gpa != null && (
                        <circle cx={x(i)} cy={y(p.gpa)} r="4" fill="var(--accent)"><title>{`${p.label}: GPA ${p.gpa}`}</title></circle>
                    )}
                    {p.cgpa != null && (
                        <circle cx={x(i)} cy={y(p.cgpa)} r="4" fill="#f59e0b"><title>{`${p.label}: CGPA ${p.cgpa}`}</title></circle>
                    )}
                    <text x={x(i)} y={H - 18} fontSize="10" textAnchor="middle" fill="currentColor" opacity="0.7">{shortLabel(p)}</text>
                </g>
            ))}
        </svg>
    )
}

function Gauge({ cgpa }) {
    const marks = [[1.5, 'Third'], [2.4, '2nd Lower'], [3.5, '2nd Upper'], [4.5, 'First']]
    return (
        <div className="pt-6 pb-8">
            <div className="relative h-3 rounded-full" style={{ backgroundColor: 'var(--surface-soft)' }}>
                <div className="h-full rounded-full accent-gradient" style={{ width: `${Math.min(cgpa / 5, 1) * 100}%` }} />
                {marks.map(([v, label]) => (
                    <div key={v} className="absolute top-[-4px] bottom-[-4px] w-px bg-slate-400" style={{ left: `${(v / 5) * 100}%` }}>
                        <span className="absolute top-5 -translate-x-1/2 text-[10px] theme-text-muted whitespace-nowrap">{label} {v}</span>
                    </div>
                ))}
                <div className="absolute -top-6 -translate-x-1/2 text-xs font-bold" style={{ left: `${Math.min(cgpa / 5, 1) * 100}%` }}>{cgpa}</div>
            </div>
        </div>
    )
}

function Highlights({ title, rows, tone }) {
    return (
        <div className="card p-4">
            <p className="font-semibold mb-2">{title}</p>
            {rows.length === 0 ? <p className="text-sm theme-text-muted">Not enough results yet.</p> : (
                <ul className="space-y-1.5">
                    {rows.map((r, i) => (
                        <li key={i} className="flex justify-between gap-3 text-sm">
                            <span>{r.code} <span className="theme-text-muted">{r.title}</span></span>
                            <span className={`font-semibold ${tone}`}>{r.score}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}

function MyProgress() {
    const { user } = useAuth()
    const [data, setData] = useState(null)
    const [open, setOpen] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!user.studentId) return
        getProgress(user.studentId).then(setData).catch((e) => setError(msg(e)))
    }, [user.studentId])

    if (!user.studentId) {
        return <div className="card p-6 max-w-xl text-sm theme-text-muted">Your account isn't linked to a student record yet, so there is no progress to show.</div>
    }
    if (error) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm max-w-xl">{error}</div>
    if (!data) return <p className="theme-text-muted">Loading…</p>

    const totalScores = data.bands.reduce((a, b) => a + b, 0)
    const TrendIcon = data.trend === 'IMPROVING' ? TrendingUp : data.trend === 'DECLINING' ? TrendingDown : Minus
    const trendText = { IMPROVING: 'Improving', DECLINING: 'Declining', STEADY: 'Steady' }[data.trend]
    const trendTone = { IMPROVING: 'text-green-600', DECLINING: 'text-red-600', STEADY: 'theme-text-muted' }[data.trend]

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-1">My Progress</h2>
            <p className="theme-text-muted text-sm mb-6">{data.studentName} · {data.matricNumber} · {data.programme} · {data.level}</p>

            {data.semesters.length === 0 ? (
                <div className="card p-6 text-sm theme-text-muted">
                    There are no approved results for you yet. Your progress appears here once your results have been approved.
                </div>
            ) : (
                <>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                        <div className="card p-4"><p className="text-2xl font-bold">{fmt(data.cgpa)}</p><p className="text-xs theme-text-muted">CGPA</p></div>
                        <div className="card p-4"><p className="text-base font-bold leading-tight">{fmt(data.degreeClass)}</p><p className="text-xs theme-text-muted">current class</p></div>
                        <div className="card p-4">
                            <p className={`text-base font-bold flex items-center gap-1 ${trendTone}`}>{data.trend ? <><TrendIcon size={16} /> {trendText}</> : '—'}</p>
                            <p className="text-xs theme-text-muted">recent GPA</p>
                        </div>
                        <div className="card p-4"><p className="text-2xl font-bold">{data.carryOvers}</p><p className="text-xs theme-text-muted">carry-over course(s)</p></div>
                    </div>

                    {data.carryOvers > 0 && (
                        <div className="mb-6 px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">
                            Courses to clear: <span className="font-medium">{data.carryOverCodes.join(', ')}</span>. You register them first each semester.
                        </div>
                    )}

                    <div className="card p-4 mb-6">
                        <p className="font-semibold mb-1">Where you stand</p>
                        <Gauge cgpa={data.cgpa} />
                        <p className="text-sm">
                            {data.nextClassName
                                ? <>You are <span className="font-semibold">{data.gap}</span> CGPA points from <span className="font-semibold">{data.nextClassName}</span> ({data.nextClassAt}).</>
                                : 'You are in the highest class. Keep it up.'}
                        </p>
                    </div>

                    <div className="card p-4 mb-6">
                        <div className="flex justify-between items-center mb-2 flex-wrap gap-2">
                            <p className="font-semibold">GPA and CGPA by semester</p>
                            <div className="flex gap-4 text-xs theme-text-muted">
                                <span className="flex items-center gap-1"><span className="w-4 h-0.5 inline-block" style={{ backgroundColor: 'var(--accent)' }} /> GPA</span>
                                <span className="flex items-center gap-1"><span className="w-4 h-0.5 inline-block bg-amber-500" /> CGPA</span>
                            </div>
                        </div>
                        <Chart points={data.semesters} />
                    </div>

                    <div className="card overflow-hidden mb-6">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Semester by semester</div>
                        <div className="divide-y theme-border">
                            {data.semesters.map((s, i) => (
                                <div key={i}>
                                    <button onClick={() => setOpen(open === i ? null : i)} className="w-full px-4 py-3 flex justify-between items-center gap-4 flex-wrap text-left text-sm">
                                        <span className="font-medium">{s.label}</span>
                                        <span className="theme-text-muted">
                      GPA <b>{fmt(s.gpa)}</b> · CGPA <b>{fmt(s.cgpa)}</b> · {s.unitsPassed}/{s.unitsOffered} units passed{s.degreeClass ? ` · ${s.degreeClass}` : ''}
                    </span>
                                    </button>
                                    {open === i && (
                                        <div className="px-4 pb-4 space-y-2">
                                            {s.courses.map((c) => (
                                                <div key={c.code} className="flex items-center gap-3 text-sm">
                                                    <span className="w-24 font-medium">{c.code}</span>
                                                    <span className="flex-1 truncate theme-text-muted">{c.title}</span>
                                                    <div className="w-32 h-2.5 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                                        <div className="h-full" style={{ width: `${Math.min(c.score ?? 0, 100)}%`, backgroundColor: c.passed ? '#16a34a' : '#dc2626' }} />
                                                    </div>
                                                    <span className="w-10 text-right font-semibold">{fmt(c.score)}</span>
                                                    <span className="w-8 text-center text-xs">{c.grade}</span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="grid md:grid-cols-2 gap-4 mb-6">
                        <Highlights title="Your strongest courses" rows={data.strongest} tone="text-green-600" />
                        <Highlights title="Courses to look at again" rows={data.weakest} tone="text-red-600" />
                    </div>

                    <div className="card p-4 mb-6">
                        <p className="font-semibold mb-3">How your scores are spread</p>
                        <div className="flex h-4 rounded-full overflow-hidden mb-3">
                            {data.bands.map((n, i) => n > 0 && (
                                <div key={i} title={`${data.bandLabels[i]}: ${n} course(s)`} style={{ width: `${(n / totalScores) * 100}%`, backgroundColor: BAND_COLORS[i] }} />
                            ))}
                        </div>
                        <div className="flex flex-wrap gap-4 text-xs theme-text-muted">
                            {data.bandLabels.map((l, i) => (
                                <span key={l} className="flex items-center gap-1"><span className="w-3 h-3 rounded inline-block" style={{ backgroundColor: BAND_COLORS[i] }} /> {l}: {data.bands[i]}</span>
                            ))}
                        </div>
                    </div>

                    <p className="text-xs theme-text-muted">{data.note}</p>
                </>
            )}
        </div>
    )
}

export default MyProgress