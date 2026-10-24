import { useEffect, useState } from 'react'
import { Printer } from 'lucide-react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { DAYS, TIME_OPTIONS, inTwoWeeks } from '../utils/timetable'
import {
    getAdminStatus, openWindow, updateDeadline, reopenWindow, autoFillWindow, publishWindow,
    listRooms, addRoom, setRoomActive, addEntry, deleteEntry,
} from '../services/timetableService'
import TimetableGrid, { printLandscape } from '../components/TimetableGrid'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleString('en-GB') : '')

const STATUS_STYLE = {
    OPEN: 'bg-amber-100 text-amber-700',
    CLOSED: 'bg-blue-100 text-blue-700',
    PUBLISHED: 'bg-green-100 text-green-700',
}
const STATUS_TEXT = {
    OPEN: 'Open: lecturers are choosing their times',
    CLOSED: 'Closed: waiting for your approval',
    PUBLISHED: 'Published: students can see it',
}

function TimetableAdmin() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [status, setStatus] = useState(null)
    const [rooms, setRooms] = useState([])
    const [deadline, setDeadline] = useState(inTwoWeeks())
    const [selected, setSelected] = useState(null)
    const [onlyWaiting, setOnlyWaiting] = useState(false)
    const [form, setForm] = useState({ courseId: '', kind: 'LECTURE', day: 'MON', start: '08:00', end: '10:00', roomId: '', note: '' })
    const [newRoom, setNewRoom] = useState({ name: '', type: 'LECTURE' })
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then((s) => {
                setSessions(s)
                const current = currentSessionOf(s)
                if (current) setSessionId(String(current.id))
            })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    async function load() {
        if (!sessionId) return
        const [s, r] = await Promise.all([getAdminStatus(sessionId, semester), listRooms()])
        setStatus(s)
        setRooms(r)
        setSelected(null)
    }

    useEffect(() => {
        if (!sessionId) return
        setError(null); setMessage(null)
        load().catch((e) => setError(msg(e)))
    }, [sessionId, semester])

    async function act(action, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            const result = await action()
            if (success) setMessage(typeof success === 'function' ? success(result) : success)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const w = status?.window
    const exists = w && w.status !== 'NONE'
    const waiting = status ? status.courses.filter((c) => !c.responded) : []

    function fillMissing() {
        if (!window.confirm('Place every course that has no slot yet? Lecturers will no longer be able to change their choices unless you reopen it.')) return
        act(() => autoFillWindow(w.id), (r) =>
            `Placed ${r.coursesPlaced} course(s) in ${r.sessionsAdded} session(s).` +
            (r.unplaced.length ? ` Could not place: ${r.unplaced.join(', ')}.` : ''))
    }

    function publish() {
        const missing = status.totalCourses - status.respondedCourses
        const force = missing > 0
        if (force && !window.confirm(`${missing} course(s) still have no slot. Publish anyway?`)) return
        if (!force && !window.confirm('Approve and publish this timetable? Students and lecturers will see it.')) return
        act(() => publishWindow(w.id, force), (r) => r.message)
    }

    function reopen() {
        const date = window.prompt('Reopen for lecturers until (YYYY-MM-DD):', inTwoWeeks())
        if (date) act(() => reopenWindow(w.id, date), 'Reopened for lecturers.')
    }

    function changeDeadline() {
        const date = window.prompt('New deadline (YYYY-MM-DD):', (w.deadline || '').slice(0, 10))
        if (date) act(() => updateDeadline(w.id, date), 'Deadline updated.')
    }

    function addManual(e) {
        e.preventDefault()
        act(() => addEntry({
            sessionId: Number(sessionId), semester, courseId: Number(form.courseId), kind: form.kind, day: form.day,
            startTime: form.start, endTime: form.end, roomId: Number(form.roomId), note: form.note,
        }), 'Session added.')
    }

    function removeSelected() {
        if (window.confirm(`Remove ${selected.courseCode} on ${selected.day} ${selected.startTime}–${selected.endTime}?`)) {
            act(() => deleteEntry(selected.id), 'Session removed.')
        }
    }

    function createRoom(e) {
        e.preventDefault()
        act(async () => { await addRoom(newRoom.name, newRoom.type); setNewRoom({ name: '', type: 'LECTURE' }) }, 'Room added.')
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const roomChoices = rooms.filter((r) => r.active && (form.kind === 'PRACTICAL' ? r.type === 'LAB' : true))

    return (
        <div>
            <div className="no-print">
                <h2 className="text-2xl font-bold mb-2">Timetable</h2>
                <p className="theme-text-muted text-sm mb-6">
                    Open a window for a semester. Lecturers choose days, times and rooms for the courses they claimed. At the deadline CSDT places
                    any course that is still missing, then you review and approve the complete timetable for students to see.
                </p>

                {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

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

                {status && !exists && (
                    <div className="card p-5 mb-6 max-w-xl">
                        <p className="font-semibold mb-1">Open the timetable window</p>
                        <p className="text-xs theme-text-muted mb-3">
                            Every lecturer who claimed a course can then choose when and where to teach it, until the deadline.
                        </p>
                        <div className="flex gap-3 items-end flex-wrap">
                            <div>
                                <label className="block text-sm font-medium mb-1">Deadline</label>
                                <input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)}
                                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                            </div>
                            <button disabled={busy} onClick={() => act(() => openWindow(Number(sessionId), semester, deadline), 'The window is open.')}
                                    className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                Open window
                            </button>
                        </div>
                    </div>
                )}

                {exists && (
                    <>
                        <div className="card p-4 mb-6">
                            <div className="flex justify-between gap-4 flex-wrap">
                                <div>
                                    <p className="font-semibold">
                                        {w.sessionName} · {w.semester === 'FIRST' ? 'First' : 'Second'} semester
                                        <span className={`ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[w.status]}`}>{w.status}</span>
                                    </p>
                                    <p className="text-sm theme-text-muted">{STATUS_TEXT[w.status]}</p>
                                    <p className="text-xs theme-text-muted mt-1">
                                        Deadline {when(w.deadline)}{w.publishedAt ? ` · published ${when(w.publishedAt)}` : ''}
                                    </p>
                                    <p className="text-sm mt-2">
                                        <span className="font-semibold">{status.respondedCourses}</span> of {status.totalCourses} courses have a slot
                                        {waiting.length > 0 && <span className="theme-text-muted"> · {waiting.length} still waiting</span>}
                                    </p>
                                </div>

                                <div className="flex gap-2 flex-wrap items-start">
                                    {w.status === 'OPEN' && (
                                        <button onClick={changeDeadline} disabled={busy} className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                            Change deadline
                                        </button>
                                    )}
                                    {w.status !== 'PUBLISHED' && (
                                        <button onClick={fillMissing} disabled={busy || waiting.length === 0}
                                                className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                            Fill missing now
                                        </button>
                                    )}
                                    {w.status !== 'OPEN' && (
                                        <button onClick={reopen} disabled={busy} className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                            Reopen for lecturers
                                        </button>
                                    )}
                                    {w.status !== 'PUBLISHED' && (
                                        <button onClick={publish} disabled={busy}
                                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                            Approve &amp; publish
                                        </button>
                                    )}
                                    <button onClick={printLandscape} className="flex items-center gap-2 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                                        <Printer size={15} /> Print
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div className="card overflow-hidden mb-6">
                            <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold flex justify-between items-center">
                                <span>Courses ({status.totalCourses})</span>
                                <label className="text-xs font-normal flex items-center gap-2">
                                    <input type="checkbox" checked={onlyWaiting} onChange={(e) => setOnlyWaiting(e.target.checked)} />
                                    Only courses still waiting
                                </label>
                            </div>
                            <div className="max-h-72 overflow-y-auto">
                                <table className="w-full text-sm text-left">
                                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs sticky top-0">
                                    <tr>
                                        <th className="px-4 py-2">Course</th>
                                        <th className="px-4 py-2">Level</th>
                                        <th className="px-4 py-2">Lecturer(s)</th>
                                        <th className="px-4 py-2">Sessions</th>
                                        <th className="px-4 py-2">Status</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y theme-border">
                                    {status.courses.filter((c) => !onlyWaiting || !c.responded).map((c) => (
                                        <tr key={c.courseId}>
                                            <td className="px-4 py-2 font-medium">{c.code} <span className="font-normal theme-text-muted">{c.title}</span></td>
                                            <td className="px-4 py-2">{c.level}</td>
                                            <td className="px-4 py-2">{c.lecturers.length ? c.lecturers.join(', ') : <span className="theme-text-muted">none yet</span>}</td>
                                            <td className="px-4 py-2">{c.sessions}</td>
                                            <td className="px-4 py-2">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${c.responded ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                            {c.responded ? 'Has a slot' : 'Waiting'}
                          </span>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <form onSubmit={addManual} className="card p-4 mb-6">
                            <p className="font-semibold mb-3">Add or adjust a session yourself</p>
                            <div className="flex gap-3 flex-wrap items-end">
                                <select value={form.courseId} onChange={(e) => setForm({ ...form, courseId: e.target.value })} required
                                        className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="">Course…</option>
                                    {status.courses.map((c) => <option key={c.courseId} value={c.courseId}>{c.code} — {c.title}</option>)}
                                </select>
                                <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value, roomId: '' })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="LECTURE">Lecture</option>
                                    <option value="PRACTICAL">Practical (lab)</option>
                                </select>
                                <select value={form.day} onChange={(e) => setForm({ ...form, day: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    {DAYS.map(([code, name]) => <option key={code} value={code}>{name}</option>)}
                                </select>
                                <select value={form.start} onChange={(e) => setForm({ ...form, start: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    {TIME_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
                                </select>
                                <span className="text-sm theme-text-muted">to</span>
                                <select value={form.end} onChange={(e) => setForm({ ...form, end: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    {TIME_OPTIONS.map((t) => <option key={t} value={t}>{t}</option>)}
                                </select>
                                <select value={form.roomId} onChange={(e) => setForm({ ...form, roomId: e.target.value })} required className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="">Room…</option>
                                    {roomChoices.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                                </select>
                                <input value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="Note (optional)"
                                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                                <button type="submit" disabled={busy}
                                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                    Add
                                </button>
                            </div>
                        </form>
                    </>
                )}
            </div>

            {exists && (
                <>
                    <TimetableGrid entries={status.entries} sessionName={w.sessionName} semester={w.semester}
                                   onSelect={setSelected} selectedId={selected?.id} />

                    {selected && (
                        <div className="no-print card p-4 mt-4 flex justify-between gap-4 flex-wrap items-center">
                            <p className="text-sm">
                                <span className="font-semibold">{selected.courseCode}</span> {selected.courseTitle} · {selected.kind === 'PRACTICAL' ? 'Practical' : 'Lecture'} ·
                                {' '}{selected.day} {selected.startTime}–{selected.endTime} · {selected.room}
                                {selected.lecturer ? ` · ${selected.lecturer}` : ''} · placed by {selected.source.toLowerCase()}
                            </p>
                            <button onClick={removeSelected} disabled={busy} className="text-sm text-red-600 hover:underline">Remove this session</button>
                        </div>
                    )}
                </>
            )}

            {rooms.length > 0 && (
                <div className="no-print card p-4 mt-6">
                    <p className="font-semibold mb-3">Rooms and labs</p>
                    <ul className="flex flex-wrap gap-2 mb-4">
                        {rooms.map((r) => (
                            <li key={r.id} className={`text-xs px-3 py-1 rounded-full border theme-border flex items-center gap-2 ${r.active ? '' : 'opacity-50'}`}>
                                {r.name} <span className="theme-text-muted">{r.type === 'LAB' ? 'lab' : 'room'}</span>
                                <button onClick={() => act(() => setRoomActive(r.id, !r.active))} disabled={busy} className="underline" style={{ color: 'var(--accent)' }}>
                                    {r.active ? 'turn off' : 'turn on'}
                                </button>
                            </li>
                        ))}
                    </ul>
                    <form onSubmit={createRoom} className="flex gap-2 flex-wrap">
                        <input value={newRoom.name} onChange={(e) => setNewRoom({ ...newRoom, name: e.target.value })} required placeholder="New room or lab name"
                               className="border theme-border rounded-lg px-3 py-1.5 text-sm" />
                        <select value={newRoom.type} onChange={(e) => setNewRoom({ ...newRoom, type: e.target.value })} className="border theme-border rounded-lg px-3 py-1.5 text-sm">
                            <option value="LECTURE">Lecture room</option>
                            <option value="LAB">Lab</option>
                        </select>
                        <button type="submit" disabled={busy} className="border theme-border rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">Add room</button>
                    </form>
                </div>
            )}
        </div>
    )
}

export default TimetableAdmin