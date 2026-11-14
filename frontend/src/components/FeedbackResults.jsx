const CRITERIA = [
    ['clarity', 'Clarity'],
    ['preparation', 'Preparation'],
    ['punctuality', 'Punctuality'],
    ['helpfulness', 'Helpfulness'],
    ['fairness', 'Fair assessment'],
]

function Bar({ label, value }) {
    return (
        <div className="flex items-center gap-3 text-sm">
            <span className="w-32 shrink-0">{label}</span>
            <div className="flex-1 h-3 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                <div className="h-full accent-gradient" style={{ width: `${(value / 5) * 100}%` }} />
            </div>
            <span className="w-10 text-right font-semibold">{value.toFixed(2)}</span>
        </div>
    )
}

function FeedbackResults({ rows, minimum, showSummary }) {
    if (rows.length === 0) {
        return <div className="card p-6 text-sm theme-text-muted">No lecturer is listed for this semester yet.</div>
    }

    // Overall score per lecturer, weighted by how many students rated each course.
    const byLecturer = new Map()
    rows.filter((r) => r.visible).forEach((r) => {
        const t = byLecturer.get(r.lecturerId) || { name: r.lecturerName, total: 0, count: 0 }
        t.total += r.overall * r.responses
        t.count += r.responses
        byLecturer.set(r.lecturerId, t)
    })
    const ranking = [...byLecturer.values()]
        .map((t) => ({ name: t.name, value: t.total / t.count, count: t.count }))
        .sort((a, b) => b.value - a.value)

    return (
        <div className="space-y-6">
            {showSummary && ranking.length > 0 && (
                <div className="card p-4">
                    <p className="font-semibold mb-3">Overall score by lecturer</p>
                    <div className="space-y-2">
                        {ranking.map((r) => <Bar key={r.name} label={r.name} value={r.value} />)}
                    </div>
                    <p className="text-xs theme-text-muted mt-3">Scores run from 1 to 5. Each lecturer's score is weighted by the number of students who rated them.</p>
                </div>
            )}

            {rows.map((r) => (
                <div key={`${r.courseId}-${r.lecturerId}`} className="card overflow-hidden">
                    <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-4 flex-wrap">
            <span className="font-semibold text-sm">
              {r.lecturerName}
                <span className="font-normal text-white/60"> · {r.code} {r.title}</span>
            </span>
                        <span className="text-xs text-white/70">{r.responses} response(s)</span>
                    </div>

                    {!r.visible ? (
                        <p className="px-4 py-4 text-sm theme-text-muted">{r.note}</p>
                    ) : (
                        <div className="p-4 grid md:grid-cols-3 gap-6">
                            <div className="md:col-span-2 space-y-2">
                                {CRITERIA.map(([key, label]) => <Bar key={key} label={label} value={r[key]} />)}
                            </div>
                            <div className="text-center self-center">
                                <p className="text-4xl font-bold">{r.overall.toFixed(2)}</p>
                                <p className="text-xs theme-text-muted">overall out of 5</p>
                            </div>

                            <div className="md:col-span-3">
                                <p className="text-sm font-semibold mb-2">Comments ({r.comments.length})</p>
                                {r.comments.length === 0 ? (
                                    <p className="text-sm theme-text-muted">No comments were written.</p>
                                ) : (
                                    <ul className="space-y-2">
                                        {r.comments.map((c, i) => (
                                            <li key={i} className="text-sm px-3 py-2 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>{c}</li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            ))}

            <p className="text-xs theme-text-muted">
                Ratings are anonymous. Scores appear only when at least {minimum} students have rated a lecturer for a course, and comments are shown in a shuffled order.
            </p>
        </div>
    )
}

export default FeedbackResults