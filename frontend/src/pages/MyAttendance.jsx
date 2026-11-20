import { useEffect, useState } from 'react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import { getMyAttendance } from '../services/attendanceService'

const msg = (err) => err.response?.data?.message || err.message
const LABEL = { PRESENT: 'Present', ABSENT: 'Absent', LATE: 'Late', EXCUSED: 'Excused' }
const TONE = { PRESENT: 'bg-green-100 text-green-700', ABSENT: 'bg-red-100 text-red-700', LATE: 'bg-amber-100 text-amber-700', EXCUSED: 'bg-slate-200 text-slate-600' }

function MyAttendance() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [open, setOpen] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!sel) return
        setData(null); setError(null); setOpen(null)
        getMyAttendance(sel.sessionId, sel.semester).then(setData).catch((e) => setError(msg(e)))
    }, [sel])

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-2">My Attendance</h2>
            <p className="theme-text-muted text-sm mb-6">How often you have been in class for each course you registered.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <SessionSemesterBar onChange={setSel} />

            {data && data.courses.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">You have no registered course for this semester.</div>
            )}

            <div className="space-y-4">
                {data && data.courses.map((c, i) => (
                    <div key={c.courseId} className="card p-4">
                        <div className="flex justify-between gap-4 flex-wrap items-center">
                            <div>
                                <p className="font-semibold">{c.code} <span className="font-normal theme-text-muted">· {c.title}</span></p>
                                <p className="text-xs theme-text-muted">{c.attended} of {c.classes} class(es) attended{c.excused ? ` · ${c.excused} excused` : ''}</p>
                            </div>
                            {c.percent == null ? (
                                <span className="text-xs theme-text-muted">No class recorded yet</span>
                            ) : (
                                <span className={`text-xs px-3 py-1 rounded-full font-medium ${c.eligible ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                  {c.eligible ? 'Eligible' : 'At risk'}
                </span>
                            )}
                        </div>

                        {c.percent != null && (
                            <div className="mt-3">
                                <div className="relative h-3 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                    <div className="h-full" style={{ width: `${Math.min(c.percent, 100)}%`, backgroundColor: c.eligible ? '#16a34a' : '#dc2626' }} />
                                    <div className="absolute top-0 bottom-0 w-0.5 bg-slate-500" style={{ left: `${data.required}%` }} title={`${data.required}% needed`} />
                                </div>
                                <p className="text-xs mt-1 theme-text-muted">
                                    <span className="font-semibold" style={{ color: 'inherit' }}>{c.percent}%</span> · {data.required}% is needed to be eligible for the exam
                                </p>
                            </div>
                        )}

                        {c.history.length > 0 && (
                            <>
                                <button onClick={() => setOpen(open === i ? null : i)} className="text-xs font-medium mt-3" style={{ color: 'var(--accent)' }}>
                                    {open === i ? 'Hide dates' : 'Show dates'}
                                </button>
                                {open === i && (
                                    <ul className="mt-2 flex flex-wrap gap-2">
                                        {c.history.map((h, j) => (
                                            <li key={j} className={`text-xs px-2 py-1 rounded-lg ${TONE[h.status]}`}>
                                                {h.date}{h.kind === 'PRACTICAL' ? ' (practical)' : ''}: {LABEL[h.status]}
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </>
                        )}
                    </div>
                ))}
            </div>
        </div>
    )
}

export default MyAttendance