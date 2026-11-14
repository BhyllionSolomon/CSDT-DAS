import { useEffect, useState } from 'react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import { getMyCa, caLabel } from '../services/caService'

const msg = (err) => err.response?.data?.message || err.message

function MyCaScores() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!sel) return
        setData(null); setError(null)
        getMyCa(sel.sessionId, sel.semester).then(setData).catch((e) => setError(msg(e)))
    }, [sel])

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-2">My C.A. Scores</h2>
            <p className="theme-text-muted text-sm mb-6">
                Your continuous assessment for each course. Scores appear when your lecturer releases them, and the C.A. is out of {data?.cap ?? 30} marks in total.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <SessionSemesterBar onChange={setSel} />

            {data && data.courses.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">You have no registered course for this semester.</div>
            )}

            <div className="space-y-4">
                {data && data.courses.map((c) => (
                    <div key={c.courseId} className="card p-4">
                        <div className="flex justify-between gap-4 flex-wrap items-center mb-3">
                            <p className="font-semibold">{c.code} <span className="font-normal theme-text-muted">· {c.title}</span></p>
                            {c.releasedMax > 0 && (
                                <span className="text-sm font-semibold">{c.earned} <span className="font-normal theme-text-muted">of {c.releasedMax} released · /{data.cap} total</span></span>
                            )}
                        </div>

                        {c.items.length === 0 ? (
                            <p className="text-sm theme-text-muted">Nothing has been released yet.</p>
                        ) : (
                            <ul className="space-y-2">
                                {c.items.map((i, k) => (
                                    <li key={k} className="flex items-center gap-3 text-sm">
                    <span className="w-32 shrink-0">
                      <span className="block font-medium">{i.title}</span>
                      <span className="block text-[11px] theme-text-muted">{caLabel(i.category)}</span>
                    </span>
                                        <div className="flex-1 h-2.5 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                            <div className="h-full accent-gradient" style={{ width: `${i.score == null ? 0 : (i.score / i.max) * 100}%` }} />
                                        </div>
                                        <span className="w-20 text-right font-semibold">{i.score == null ? 'Not marked' : `${i.score} / ${i.max}`}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                ))}
            </div>
        </div>
    )
}

export default MyCaScores