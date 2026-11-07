import { useEffect, useState } from 'react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import FeedbackResults from '../components/FeedbackResults'
import { getFeedbackResults, setFeedbackWindow } from '../services/feedbackService'

const msg = (err) => err.response?.data?.message || err.message

function FeedbackAdmin() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    async function load() {
        setData(await getFeedbackResults(sel.sessionId, sel.semester))
    }

    useEffect(() => {
        if (!sel) return
        setData(null); setError(null); setMessage(null)
        load().catch((e) => setError(msg(e)))
    }, [sel])

    async function toggle(open) {
        const question = open
            ? 'Open feedback? Students can then rate their lecturers anonymously until you close it.'
            : 'Close feedback? Students can no longer rate, and the results become visible to you and to each lecturer for their own courses.'
        if (!window.confirm(question)) return

        setError(null); setMessage(null); setBusy(true)
        try {
            await setFeedbackWindow(Number(sel.sessionId), sel.semester, open)
            setMessage(open ? 'Feedback is open.' : 'Feedback is closed. Results are now available.')
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const open = data?.window.open
    const totalResponses = data ? data.rows.reduce((t, r) => t + r.responses, 0) : 0

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Lecturer Feedback</h2>
            <p className="theme-text-muted text-sm mb-6">
                Open feedback near the end of a semester so students can rate their lecturers anonymously. While it is open you see only how many
                students have responded. Results appear when you close it.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <SessionSemesterBar onChange={setSel}>
                {data && (
                    <div className="flex items-center gap-3 pb-1">
            <span className={`text-xs px-3 py-1 rounded-full font-medium ${open ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600'}`}>
              {open ? 'Open' : 'Not open'}
            </span>
                        {open ? (
                            <button onClick={() => toggle(false)} disabled={busy}
                                    className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">Close feedback</button>
                        ) : (
                            <button onClick={() => toggle(true)} disabled={busy}
                                    className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">Open feedback</button>
                        )}
                        <span className="text-xs theme-text-muted">{totalResponses} response(s) so far</span>
                    </div>
                )}
            </SessionSemesterBar>

            {data && <FeedbackResults rows={data.rows} minimum={data.minimum} showSummary />}
        </div>
    )
}

export default FeedbackAdmin