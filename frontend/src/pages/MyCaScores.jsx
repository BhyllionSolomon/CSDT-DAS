import { useEffect, useState } from 'react'
import { Upload, Clock, CheckCircle2, AlertCircle } from 'lucide-react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import { getMyCa, submitCaFile, caLabel } from '../services/caService'

const msg = (err) => err.response?.data?.message || err.message
const dateText = (v) => v ? new Date(v).toLocaleString() : 'No deadline'
const isPast = (v) => v && new Date(v).getTime() < Date.now()
const isSoon = (v) => v && !isPast(v) && new Date(v).getTime() - Date.now() < 48 * 60 * 60 * 1000

function MyCaScores() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [busy, setBusy] = useState({})
    const [files, setFiles] = useState({})

    async function refresh() {
        if (sel) setData(await getMyCa(sel.sessionId, sel.semester))
    }
    useEffect(() => {
        if (!sel) return
        setData(null); setError(null); setNotice(null)
        getMyCa(sel.sessionId, sel.semester).then(setData).catch((e) => setError(msg(e)))
    }, [sel])

    async function submit(item) {
        const file = files[item.id]
        if (!file) { setError('Choose a file before submitting.'); return }
        setBusy({ ...busy, [item.id]: true }); setError(null); setNotice(null)
        try {
            await submitCaFile(item.id, file)
            await refresh()
            setFiles({ ...files, [item.id]: null })
            setNotice(`${item.title} submitted successfully. Keep your submission confirmation.`)
        } catch (e) { setError(msg(e)) } finally { setBusy({ ...busy, [item.title]: false }) }
    }

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">My Continuous Assessment</h2>
            <p className="theme-text-muted text-sm mb-6">
                View assignments and practicals, check deadlines, upload your work and track marks.
                Your lecturer's combined CA score is calculated proportionally out of 30.
            </p>
            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div role="status" className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}
            <SessionSemesterBar onChange={setSel} />
            {data && data.courses.length === 0 && <div className="card p-6 text-sm theme-text-muted">You have no registered course for this semester.</div>}
            <div className="space-y-5">
                {data && data.courses.map((course) => (
                    <section key={course.courseId} className="card p-4">
                        <div className="flex justify-between items-center gap-3 flex-wrap mb-3">
                            <div><h3 className="font-bold">{course.code} · {course.title}</h3><p className="text-xs theme-text-muted">Continuous assessment activities</p></div>
                            <div className="text-right"><p className="text-xl font-bold">{course.aggregated ? course.finalScore : course.earned} / 30</p><p className="text-xs theme-text-muted">{course.aggregated ? 'Final aggregated CA score' : 'Provisional CA score'}</p></div>
                        </div>
                        {course.items.length === 0 ? <p className="text-sm theme-text-muted">No assessment has been posted yet.</p> :
                            <div className="space-y-3">{course.items.map((item, index) => {
                                const closed = isPast(item.dueAt)
                                const submitted = item.submissionStatus === 'SUBMITTED' || item.submissionStatus === 'GRADED'
                                return <article key={index} className="rounded-xl border theme-border p-4">
                                    <div className="flex justify-between gap-3 flex-wrap">
                                        <div><span className="text-[11px] uppercase theme-text-muted">{caLabel(item.category)}</span><h4 className="font-semibold">{item.title}</h4></div>
                                        <span className="text-sm font-semibold">{item.score == null ? 'Not marked' : `${item.score} / ${item.max}`}</span>
                                    </div>
                                    {item.instructions && <p className="text-sm mt-2 whitespace-pre-wrap">{item.instructions}</p>}
                                    <div className="flex items-center gap-2 text-xs mt-3 theme-text-muted"><Clock size={14}/> Deadline: {dateText(item.dueAt)}</div>
                                    {item.dueAt && isSoon(item.dueAt) && !submitted && <div className="mt-2 text-sm text-amber-700 bg-amber-50 rounded-lg p-2"><AlertCircle size={14} className="inline mr-1"/>Reminder: this assessment is due within 48 hours.</div>}
                                    {submitted ? <div className="mt-3 rounded-lg p-3 text-sm" style={{backgroundColor:'var(--surface-soft)'}}>
                                        <p className="font-semibold text-green-700"><CheckCircle2 size={15} className="inline mr-1"/>Submitted</p>
                                        <p className="text-xs theme-text-muted mt-1">{item.submissionFileName} · {dateText(item.submittedAt)}</p>
                                        {item.feedback && <p className="mt-2"><b>Lecturer feedback:</b> {item.feedback}</p>}
                                        {item.submissionStatus === 'GRADED' && <p className="mt-1 font-semibold">Marked: {item.score} / {item.max}</p>}
                                        {!closed && <p className="text-xs theme-text-muted mt-2">You may replace your file before the deadline.</p>}
                                    </div> : closed ? <div className="mt-3 text-sm text-red-700 bg-red-50 rounded-lg p-3">Deadline passed. Online submission is closed; this activity will not accept a late upload.</div> :
                                        <div className="mt-3 flex flex-wrap items-center gap-2">
                                            <input type="file" onChange={(e) => setFiles({ ...files, [item.id]: e.target.files?.[0] || null })} className="text-sm max-w-full" />
                                            <button onClick={() => submit(item)} disabled={!!busy[item.id] || !files[item.id]} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm disabled:opacity-50"><Upload size={14} className="inline mr-1"/>{busy[item.id] ? 'Uploading…' : 'Submit assignment'}</button>
                                            <span className="text-xs theme-text-muted">Maximum file size: 20 MB</span>
                                        </div>}
                                </article>
                            })}</div>}
                    </section>
                ))}
            </div>
        </div>
    )
}

export default MyCaScores
