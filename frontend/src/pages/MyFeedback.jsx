import { useEffect, useState } from 'react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import FeedbackResults from '../components/FeedbackResults'
import { getFeedbackResults } from '../services/feedbackService'

const msg = (err) => err.response?.data?.message || err.message

function MyFeedback() {
    const [sel, setSel] = useState(null)
    const [data, setData] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        if (!sel) return
        setData(null); setError(null)
        getFeedbackResults(sel.sessionId, sel.semester).then(setData).catch((e) => setError(msg(e)))
    }, [sel])

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">My Feedback</h2>
            <p className="theme-text-muted text-sm mb-6">
                What students said about the courses you teach. It is anonymous and combined. Results appear after the H.O.D closes feedback for the semester.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <SessionSemesterBar onChange={setSel} />

            {data && <FeedbackResults rows={data.rows} minimum={data.minimum} />}
        </div>
    )
}

export default MyFeedback