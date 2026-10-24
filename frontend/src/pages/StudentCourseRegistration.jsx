import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import { getOutstandingCourses, registerMultiple } from '../services/registrationService'
import { carryOversFor, currentSessionOf } from '../utils/registrationHelpers'

const msg = (err) => err.response?.data?.message || err.message

function StudentCourseRegistration() {
    const { user } = useAuth()

    const [courses, setCourses] = useState([])
    const [sessions, setSessions] = useState([])
    const [outstanding, setOutstanding] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [picked, setPicked] = useState({})
    const [required, setRequired] = useState(null)
    const [registered, setRegistered] = useState([])
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        if (!user.studentId) { setLoading(false); return }
        Promise.all([getAllCourses(), getAllSessions(), getOutstandingCourses(user.studentId)])
            .then(([c, s, o]) => {
                setCourses(c); setSessions(s); setOutstanding(o)
                const current = currentSessionOf(s)
                if (current) setSessionId(String(current.id))
            })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [user.studentId])

    useEffect(() => {
        if (!user.studentId || !user.programmeId || !user.levelId) return
        api.get(`/unit-limits/programme/${user.programmeId}/level/${user.levelId}/semester/${semester}`)
            .then((res) => setRequired(res.data?.requiredUnits ?? res.data?.required ?? null))
            .catch(() => setRequired(null))
    }, [user.studentId, user.programmeId, user.levelId, semester])

    async function loadRegistered() {
        if (!sessionId) return
        try {
            const res = await api.get(`/course-registrations/student/${user.studentId}/session/${sessionId}`)
            setRegistered(
                res.data
                    .filter((r) => String(r.status).toUpperCase() === 'REGISTERED')
                    .map((r) => ({ id: r.courseId ?? r.course?.id, semester: String(r.semester || '').toUpperCase() }))
            )
        } catch (e) { setRegistered([]) }
    }

    useEffect(() => { loadRegistered() }, [sessionId, user.studentId])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    if (!user.studentId) {
        return (
            <div className="card p-6 max-w-xl">
                <h2 className="text-xl font-bold mb-2">Course Registration</h2>
                <p className="text-sm theme-text-muted">
                    Your account isn't linked to a student record yet. If you have just been added to the department's roster,
                    sign up with your matric number. Otherwise contact your Level Adviser or the H.O.D.
                </p>
            </div>
        )
    }

    const registeredIds = new Set(registered.filter((r) => r.semester === semester).map((r) => r.id))
    const carry = carryOversFor(outstanding, semester)
    const carryIds = new Set(carry.map((c) => c.id))

    const offered = courses.filter((c) =>
        c.active !== false && c.levelId === user.levelId && c.semester === semester &&
        c.programmes?.some((p) => p.id === user.programmeId) && !carryIds.has(c.id))

    const chosen = new Map()
    carry.forEach((c) => chosen.set(c.id, c.unit))
    offered.forEach((c) => {
        if (picked[c.id] || registeredIds.has(c.id)) chosen.set(c.id, c.creditUnit || 0)
    })

    const ids = [...chosen.keys()]
    const total = [...chosen.values()].reduce((a, b) => a + b, 0)
    const matches = required !== null && total === required

    async function register() {
        setError(null); setNotice(null); setBusy(true)
        try {
            await registerMultiple(user.studentId, Number(sessionId), semester, ids)
            setNotice('Your courses are registered. Next, open Registration & Payment and enter your invoice code.')
            setPicked({})
            await loadRegistered()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-1">Course Registration</h2>
            <p className="theme-text-muted text-sm mb-6">
                {user.fullName} · {user.programmeName} · {user.levelName}
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    {notice}{' '}
                    <Link to="/my-registration" className="underline font-medium">Registration &amp; Payment</Link>
                </div>
            )}

            <div className="card p-4 mb-6 flex gap-4 flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => { setSemester(e.target.value); setPicked({}) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
            </div>

            {carry.length > 0 && (
                <div className="card overflow-hidden mb-6">
                    <div className="bg-amber-500 text-white px-4 py-2 text-sm font-semibold">
                        Carry-over courses: you failed these, so they come first ({carry.length})
                    </div>
                    <ul className="divide-y theme-border">
                        {carry.map((c) => (
                            <li key={c.id} className="px-4 py-2 flex items-center gap-3 text-sm">
                                <input type="checkbox" checked disabled />
                                <span className="font-medium">{c.code}</span>
                                <span className="flex-1">{c.title}</span>
                                <span className="theme-text-muted">{c.unit} unit(s)</span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="card overflow-hidden mb-6">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">
                    {user.levelName} {semester === 'FIRST' ? 'first' : 'second'} semester courses
                </div>
                {offered.length === 0 ? (
                    <p className="p-4 text-sm theme-text-muted">
                        No courses have been set up for your programme and level this semester yet. Please check with the H.O.D.
                    </p>
                ) : (
                    <ul className="divide-y theme-border">
                        {offered.map((c) => {
                            const done = registeredIds.has(c.id)
                            return (
                                <li key={c.id} className="px-4 py-2">
                                    <label className="flex items-center gap-3 text-sm">
                                        <input type="checkbox" checked={done || !!picked[c.id]} disabled={done}
                                               onChange={(e) => setPicked({ ...picked, [c.id]: e.target.checked })} />
                                        <span className="font-medium">{c.code}</span>
                                        <span className="flex-1">{c.title}</span>
                                        <span className="theme-text-muted">{c.creditUnit} unit(s) · {c.status || 'C'}</span>
                                        {done && <span className="text-xs text-green-700 font-medium">Registered</span>}
                                    </label>
                                </li>
                            )
                        })}
                    </ul>
                )}
            </div>

            <div className="flex items-center gap-4 flex-wrap">
                <button onClick={register} disabled={busy || !sessionId || ids.length === 0}
                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Please wait…' : 'Register courses'}
                </button>
                <p className={`text-sm ${matches ? 'text-green-700' : 'theme-text-muted'}`}>
                    Selected: {total} unit(s) · Required: {required ?? 'not set yet'}
                </p>
            </div>
        </div>
    )
}

export default StudentCourseRegistration