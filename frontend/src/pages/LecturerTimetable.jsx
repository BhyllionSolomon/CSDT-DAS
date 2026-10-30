import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { DAYS, TIME_OPTIONS } from '../utils/timetable'
import { getMineTimetable, listRooms, addEntry, deleteEntry } from '../services/timetableService'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '')
const dayName = (code) => (DAYS.find(([c]) => c === code) || [code, code])[1]

function AddSession({ course, rooms, sessionId, semester, onAdded }) {
    const [f, setF] = useState({ kind: 'LECTURE', day: 'MON', start: '08:00', end: '10:00', roomId: '', note: '' })
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    const choices = rooms.filter((r) => r.active && (f.kind === 'PRACTICAL' ? r.type === 'LAB' : true))

    async function submit(e) {
        e.preventDefault()
        setError(null); setBusy(true)
        try {
            await addEntry({
                sessionId: Number(sessionId), semester, courseId: course.courseId, kind: f.kind, day: f.day,
                startTime: f.start, endTime: f.end, roomId: Number(f.roomId), note: f.note,
            })
            await onAdded()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    return (
        <form onSubmit={submit} className="mt-3 p-3 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>
            {error && <div className="mb-2 px-3 py-2 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            <div className="flex gap-2 flex-wrap items-end">
                <select value={f.kind} onChange={(e) => setF({ ...f, kind: e.target.value, roomId: '' })} className="border theme-border rounded-lg px-2 py-1.5 text-sm">
                    <option value="LECTURE">Lecture</option>
                    <option value="PRACTICAL">Practical (lab)</option>
                </select>
                <select value={f.day} onChange={(e) => setF({ ...f, day: e.target.value })} className="border theme-border rounded-lg px-2 py-1.5 text-sm">
                    {DAYS.map(([code, name]) => <option key={code} value={code}>{name}</option>)}
                </select>
                <select value={f.start} onChange={(e) => setF({ ...f, start: e.target.value })} className="border theme-border rounded-lg px-2 py-1.5 text-sm">
                    {TIME_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
                <span className="text-sm theme-text-muted">to</span>
                <select value={f.end} onChange={(e) => setF({ ...f, end: e.target.value })} className="border theme-border rounded-lg px-2 py-1.5 text-sm">
                    {TIME_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
                <select value={f.roomId} onChange={(e) => setF({ ...f, roomId: e.target.value })} required className="border theme-border rounded-lg px-2 py-1.5 text-sm">
                    <option value="">Room…</option>
                    {choices.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
                <input value={f.note} onChange={(e) => setF({ ...f, note: e.target.value })} placeholder="Note (optional)"
                       className="border theme-border rounded-lg px-2 py-1.5 text-sm w-40" />
                <button type="submit" disabled={busy}
                        className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Checking…' : 'Add session'}
                </button>
            </div>
            <p className="text-xs theme-text-muted mt-2">
                A practical must use a lab. The system refuses a double-booked room, a clash in your own timetable, two courses the same students take at the same time, and the 1 to 2 pm break.
            </p>
        </form>
    )
}

function LecturerTimetable() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [data, setData] = useState(null)
    const [rooms, setRooms] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllSessions(), listRooms()])
            .then(([s, r]) => {
                setSessions(s); setRooms(r)
                const current = currentSessionOf(s)
                if (current) setSessionId(String(current.id))
            })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    async function load() {
        if (!sessionId) return
        setData(await getMineTimetable(sessionId, semester))
    }

    useEffect(() => {
        setError(null)
        load().catch((e) => setError(msg(e)))
    }, [sessionId, semester])

    async function remove(entry) {
        if (!window.confirm(`Remove ${entry.courseCode} on ${entry.day} ${entry.startTime}–${entry.endTime}?`)) return
        try { await deleteEntry(entry.id); await load() } catch (err) { setError(msg(err)) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const w = data?.window

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">My Timetable Choices</h2>
            <p className="theme-text-muted text-sm mb-6">
                Choose the days, times and rooms for the courses you claimed. Courses without a choice by the deadline are placed for you automatically.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => setSemester(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
            </div>

            {w && w.status === 'NONE' && (
                <div className="card p-6 text-sm theme-text-muted">The H.O.D has not opened the timetable for this semester yet.</div>
            )}

            {w && w.status === 'OPEN' && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">
                    Choose your times before <strong>{when(w.deadline)}</strong>. After that, any course without a choice is placed automatically.
                </div>
            )}
            {w && w.status === 'CLOSED' && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-blue-50 text-blue-700 text-sm">
                    The deadline has passed and the H.O.D is reviewing the timetable. To change something, please contact the H.O.D.
                </div>
            )}
            {w && w.status === 'PUBLISHED' && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    The timetable is published. <Link to="/timetable" className="underline font-medium">View the department timetable</Link>
                </div>
            )}

            {data && w.status !== 'NONE' && data.courses.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">
                    You have not claimed any course for this semester yet. Claim your courses first, then come back here.
                </div>
            )}

            <div className="space-y-4">
                {data && w.status !== 'NONE' && data.courses.map((c) => (
                    <div key={c.courseId} className="card p-4">
                        <div className="flex justify-between gap-4 flex-wrap">
                            <div>
                                <p className="font-semibold">{c.code} <span className="font-normal theme-text-muted">— {c.title}</span></p>
                                <p className="text-xs theme-text-muted">{c.unit} unit(s) · {c.level}</p>
                            </div>
                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium h-fit ${c.entries.length ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                {c.entries.length ? `${c.entries.length} session(s)` : 'No choice yet'}
              </span>
                        </div>

                        {c.entries.length > 0 && (
                            <ul className="divide-y theme-border mt-3">
                                {c.entries.map((e) => (
                                    <li key={e.id} className="py-1.5 flex items-center gap-3 text-sm">
                                        <span className="font-medium w-24">{dayName(e.day)}</span>
                                        <span className="w-28">{e.startTime}–{e.endTime}</span>
                                        <span className="flex-1">{e.room} · {e.kind === 'PRACTICAL' ? 'Practical' : 'Lecture'}{e.note ? ` · ${e.note}` : ''}</span>
                                        <span className="text-xs theme-text-muted">{e.source === 'AUTO' ? 'placed automatically' : ''}</span>
                                        {data.canEdit && (
                                            <button onClick={() => remove(e)} className="text-xs text-red-600 hover:underline">Remove</button>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}

                        {data.canEdit && (
                            <AddSession course={c} rooms={rooms} sessionId={sessionId} semester={semester} onAdded={load} />
                        )}
                    </div>
                ))}
            </div>
        </div>
    )
}

export default LecturerTimetable