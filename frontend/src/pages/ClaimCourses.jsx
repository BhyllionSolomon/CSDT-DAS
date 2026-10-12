import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllSessions } from '../services/sessionService'
import { getMyOptions, claimCourse } from '../services/allocationService'

const msg = (err) => err.response?.data?.message || err.message

function Section({ title, bar, children }) {
    return (
        <div className="card overflow-hidden mb-6">
            <div className={`${bar} text-white px-4 py-2 text-sm font-semibold`}>{title}</div>
            {children}
        </div>
    )
}

function ClaimCourses() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [options, setOptions] = useState(null)
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    useEffect(() => {
        getAllSessions().then(setSessions).catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    async function load() {
        setOptions(await getMyOptions(sessionId, semester))
    }

    async function handleLoad() {
        setError(null); setMessage(null); setBusy(true)
        try { await load() } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function handleClaim(entry, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            await claimCourse(entry.courseId, entry.academicSessionId, entry.semester)
            setMessage(success)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const Row = ({ e, action }) => (
        <li className="px-4 py-3 flex items-center justify-between gap-4">
            <div>
                <p className="font-semibold">{e.courseCode} <span className="font-normal theme-text-muted">— {e.courseTitle}</span></p>
                <p className="text-xs theme-text-muted">{e.creditUnit} unit(s) · {e.levelName}</p>
            </div>
            {action}
        </li>
    )

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const empty = options && !options.confirm.length && !options.mine.length && !options.open.length

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Claim Courses</h2>
            <p className="theme-text-muted text-sm mb-6">
                Confirm the courses listed in your name, or claim a course that has no lecturer yet.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => { setSessionId(e.target.value); setOptions(null) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => { setSemester(e.target.value); setOptions(null) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
                <button onClick={handleLoad} disabled={!sessionId || busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Working…' : 'Show my courses'}
                </button>
            </div>

            {options && options.confirm.length > 0 && (
                <Section title={`Listed in your name: please confirm (${options.confirm.length})`} bar="bg-amber-500">
                    <ul className="divide-y theme-border">
                        {options.confirm.map((e) => (
                            <Row key={e.id} e={e} action={
                                <button disabled={busy} onClick={() => handleClaim(e, `${e.courseCode} confirmed.`)}
                                        className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                                    Confirm
                                </button>
                            } />
                        ))}
                    </ul>
                </Section>
            )}

            {options && options.mine.length > 0 && (
                <Section title={`Your courses (${options.mine.length})`} bar="bg-green-600">
                    <ul className="divide-y theme-border">
                        {options.mine.map((e) => (
                            <Row key={e.id} e={e} action={
                                <div className="flex gap-3 text-sm font-medium" style={{ color: 'var(--accent)' }}>
                                    <Link to="/teaching">Teaching</Link>
                                    <Link to={`/exam-scoresheet?courseId=${e.courseId}&sessionId=${e.academicSessionId}&semester=${e.semester}`}>
                                        Record results
                                    </Link>
                                </div>
                            } />
                        ))}
                    </ul>
                </Section>
            )}

            {options && options.open.length > 0 && (
                <Section title={`Open courses: no lecturer yet (${options.open.length})`} bar="bg-slate-600">
                    <ul className="divide-y theme-border">
                        {options.open.map((e) => (
                            <Row key={e.id} e={e} action={
                                <button disabled={busy} onClick={() => handleClaim(e, `${e.courseCode} is now yours.`)}
                                        className="border theme-border rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                                    Claim
                                </button>
                            } />
                        ))}
                    </ul>
                </Section>
            )}

            {empty && (
                <div className="card p-6 text-sm theme-text-muted">
                    Nothing is listed for this session and semester yet. If a course you teach already has other lecturers on it,
                    ask the H.O.D to add you.
                </div>
            )}
        </div>
    )
}

export default ClaimCourses