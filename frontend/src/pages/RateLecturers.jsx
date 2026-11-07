import { useEffect, useState } from 'react'
import { ShieldCheck } from 'lucide-react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import { getMyFeedback, submitFeedback } from '../services/feedbackService'

const QUESTIONS = [
    ['clarity', 'Explains the course clearly'],
    ['preparation', 'Comes to class well prepared'],
    ['punctuality', 'Starts and ends classes on time'],
    ['helpfulness', 'Is approachable and helpful'],
    ['fairness', 'Assesses students fairly'],
]
const SCORE_WORDS = { 1: 'Poor', 2: 'Fair', 3: 'Good', 4: 'Very good', 5: 'Excellent' }
const BLANK = { clarity: 0, preparation: 0, punctuality: 0, helpfulness: 0, fairness: 0, comment: '' }

const msg = (err) => err.response?.data?.message || err.message
const keyOf = (item) => `${item.courseId}-${item.lecturerId}`

function RateLecturers() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [active, setActive] = useState(null)
    const [form, setForm] = useState(BLANK)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function load() {
        setData(await getMyFeedback(sel.sessionId, sel.semester))
    }

    useEffect(() => {
        if (!sel) return
        setData(null); setActive(null); setError(null); setNotice(null)
        load().catch((e) => setError(msg(e)))
    }, [sel])

    function openForm(item) {
        setActive(keyOf(item)); setForm(BLANK); setError(null); setNotice(null)
    }

    async function submit(e, item) {
        e.preventDefault()
        if (QUESTIONS.some(([key]) => !form[key])) {
            setError('Please give every question a score from 1 to 5.')
            return
        }
        setError(null); setBusy(true)
        try {
            const result = await submitFeedback({
                sessionId: Number(sel.sessionId), semester: sel.semester,
                courseId: item.courseId, lecturerId: item.lecturerId, ...form,
            })
            setNotice(result.message)
            setActive(null)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-2">Rate My Lecturers</h2>
            <p className="theme-text-muted text-sm mb-4">
                Tell the department how your courses are being taught. You can rate each lecturer once for each course you registered.
            </p>

            <div className="flex items-start gap-3 mb-6 px-4 py-3 rounded-lg bg-green-50 text-green-800 text-sm">
                <ShieldCheck size={18} className="shrink-0 mt-0.5" />
                <span>Your feedback is anonymous. Your name is not stored with your scores or your comment, and lecturers see only combined results.</span>
            </div>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <SessionSemesterBar onChange={setSel} />

            {data && !data.open && (
                <div className="card p-6 text-sm theme-text-muted">Feedback is not open for this semester. The H.O.D opens it near the end of the semester.</div>
            )}

            {data && data.open && data.items.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">
                    There is nobody to rate yet. Lecturers appear here for the courses you registered this semester, once the H.O.D has listed who teaches them.
                </div>
            )}

            {data && data.open && (
                <div className="space-y-4">
                    {data.items.map((item) => (
                        <div key={keyOf(item)} className="card p-4">
                            <div className="flex justify-between gap-4 flex-wrap items-center">
                                <div>
                                    <p className="font-semibold">{item.lecturerName}</p>
                                    <p className="text-sm theme-text-muted">{item.code} — {item.title}</p>
                                </div>
                                {item.rated ? (
                                    <span className="text-xs px-3 py-1 rounded-full bg-green-100 text-green-700 font-medium">Rated, thank you</span>
                                ) : active === keyOf(item) ? (
                                    <button onClick={() => setActive(null)} className="text-sm theme-text-muted hover:underline">Cancel</button>
                                ) : (
                                    <button onClick={() => openForm(item)} className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press">Rate</button>
                                )}
                            </div>

                            {active === keyOf(item) && !item.rated && (
                                <form onSubmit={(e) => submit(e, item)} className="mt-4 space-y-4">
                                    {QUESTIONS.map(([key, text]) => (
                                        <div key={key}>
                                            <p className="text-sm font-medium mb-1">{text}</p>
                                            <div className="flex gap-2 flex-wrap items-center">
                                                {[1, 2, 3, 4, 5].map((n) => (
                                                    <button key={n} type="button" onClick={() => setForm({ ...form, [key]: n })}
                                                            className={`w-10 h-10 rounded-lg text-sm font-semibold border theme-border btn-press ${form[key] === n ? 'accent-gradient text-white' : ''}`}>
                                                        {n}
                                                    </button>
                                                ))}
                                                <span className="text-xs theme-text-muted ml-1">{form[key] ? SCORE_WORDS[form[key]] : '1 = poor, 5 = excellent'}</span>
                                            </div>
                                        </div>
                                    ))}

                                    <div>
                                        <label className="block text-sm font-medium mb-1">Comment (optional)</label>
                                        <textarea value={form.comment} maxLength={500} rows={3}
                                                  onChange={(e) => setForm({ ...form, comment: e.target.value })}
                                                  placeholder="What works well, and what could improve? Please do not include your name."
                                                  className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                                        <p className="text-xs theme-text-muted text-right">{form.comment.length}/500</p>
                                    </div>

                                    <button type="submit" disabled={busy}
                                            className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                        {busy ? 'Saving…' : 'Submit anonymously'}
                                    </button>
                                </form>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}

export default RateLecturers