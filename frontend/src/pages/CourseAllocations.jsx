import { useEffect, useMemo, useState } from 'react'
import { Check, Clock, X, Plus } from 'lucide-react'
import api from '../services/api'
import { getAllSessions } from '../services/sessionService'
import { getAllCourses } from '../services/courseService'
import {
    getAllocationsForSession, offerCourse, addLecturer, linkAllocation,
    removeAllocation, previewAllocationDoc, commitAllocationDoc,
} from '../services/allocationService'

const msg = (err) => err.response?.data?.message || err.message
const TEACHING = ['LECTURER', 'ADJUNCT', 'LEVEL_ADVISER']
const LEVEL_CODES = ['100', '200', '300', '400']
const TABS = [
    ['course', 'By Course'],
    ['lecturer', 'By Lecturer'],
    ['upload', 'Upload Document'],
    ['offer', 'Add a Course'],
]

function AddLecturerBox({ accounts, onAdd, onCancel }) {
    const [text, setText] = useState('')
    const [picked, setPicked] = useState(null)

    const term = text.trim().toLowerCase()
    const matches = term && !picked
        ? accounts.filter((a) => a.fullName.toLowerCase().includes(term)).slice(0, 6)
        : []

    return (
        <div className="mt-2 p-3 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>
            <input value={text} onChange={(e) => { setText(e.target.value); setPicked(null) }}
                   placeholder="Lecturer's name" className="w-full border theme-border rounded-lg px-3 py-1.5 text-sm" />
            {matches.length > 0 && (
                <ul className="card divide-y theme-border mt-1 max-h-48 overflow-y-auto">
                    {matches.map((a) => (
                        <li key={a.id}>
                            <button type="button" onClick={() => { setPicked(a); setText(a.fullName) }}
                                    className="w-full text-left px-3 py-1.5 text-sm hover:bg-black/5">
                                {a.fullName} <span className="theme-text-muted">· {a.email}</span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            <p className="text-xs theme-text-muted mt-1">
                {picked
                    ? 'Has an account: added as confirmed.'
                    : term ? 'No account selected: listed by name until they confirm it.' : 'Start typing to search signed-up lecturers.'}
            </p>
            <div className="flex gap-2 mt-2">
                <button type="button" disabled={!text.trim()}
                        onClick={() => onAdd(picked ? { lecturerId: picked.id } : { lecturerName: text.trim() })}
                        className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                    Add
                </button>
                <button type="button" onClick={onCancel} className="text-sm theme-text-muted hover:underline">Cancel</button>
            </div>
        </div>
    )
}

function CourseAllocations() {
    const [sessions, setSessions] = useState([])
    const [allCourses, setAllCourses] = useState([])
    const [accounts, setAccounts] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [loading, setLoading] = useState(true)

    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [entries, setEntries] = useState(null)
    const [tab, setTab] = useState('course')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)
    const [addingFor, setAddingFor] = useState(null)
    const [onlyOpen, setOnlyOpen] = useState(false)
    const [offerId, setOfferId] = useState('')

    const [file, setFile] = useState(null)
    const [preview, setPreview] = useState(null)
    const [createMissing, setCreateMissing] = useState(true)

    useEffect(() => {
        Promise.all([getAllSessions(), getAllCourses(), api.get('/users'), api.get('/programmes'), api.get('/levels')])
            .then(([s, c, u, p, l]) => {
                setSessions(s)
                setAllCourses(c)
                setAccounts(u.data.filter((x) => TEACHING.includes(x.role)))
                setProgrammes(p.data)
                setLevels(l.data.filter((x) => LEVEL_CODES.includes(x.code)))
            })
            .catch((err) => setError(msg(err)))
            .finally(() => setLoading(false))
    }, [])

    async function loadEntries(sid = sessionId, sem = semester) {
        if (!sid) return
        setEntries(await getAllocationsForSession(sid, sem))
    }

    async function handleLoad() {
        setError(null); setMessage(null); setBusy(true)
        try { await loadEntries() } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function run(action, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            await action()
            if (success) setMessage(success)
            await loadEntries()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const courses = useMemo(() => {
        const map = new Map()
        for (const e of entries || []) {
            if (!map.has(e.courseId)) {
                map.set(e.courseId, {
                    courseId: e.courseId, code: e.courseCode, title: e.courseTitle,
                    unit: e.creditUnit, status: e.courseStatus, level: e.levelName, rows: [],
                })
            }
            map.get(e.courseId).rows.push(e)
        }
        return [...map.values()].sort((a, b) => a.code.localeCompare(b.code))
    }, [entries])

    const lecturers = useMemo(() => {
        const map = new Map()
        for (const e of entries || []) {
            if (!e.lecturerId && !e.lecturerName) continue
            const key = e.lecturerId ? `u${e.lecturerId}` : `n${e.lecturerName.toLowerCase()}`
            if (!map.has(key)) map.set(key, { key, name: e.lecturerName, claimed: !!e.lecturerId, rows: [] })
            map.get(key).rows.push(e)
        }
        return [...map.values()].sort((a, b) => a.name.localeCompare(b.name))
    }, [entries])

    const named = (entries || []).filter((e) => e.lecturerId || e.lecturerName)
    const claimed = named.filter((e) => e.claimed).length
    const hasLecturer = (c) => c.rows.some((r) => r.lecturerId || r.lecturerName)
    const openCount = courses.filter((c) => !hasLecturer(c)).length
    const shownCourses = onlyOpen ? courses.filter((c) => !hasLecturer(c)) : courses
    const notListed = allCourses.filter((c) => !courses.some((x) => x.courseId === c.id))

    // ---- document import
    async function handlePreview() {
        if (!file) return
        setError(null); setMessage(null); setBusy(true)
        try {
            const rows = await previewAllocationDoc(file)
            setPreview(rows.map((r) => ({ ...r, include: true })))
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function patchRow(i, patch) {
        setPreview((rows) => rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))
    }

    function patchNew(i, j, patch) {
        setPreview((rows) => rows.map((r, idx) => (idx !== i ? r : {
            ...r, newCourses: r.newCourses.map((nc, k) => (k === j ? { ...nc, ...patch } : nc)),
        })))
    }

    function toggleProgramme(i, j, programmeId) {
        const nc = preview[i].newCourses[j]
        const has = nc.programmeIds.includes(programmeId)
        patchNew(i, j, {
            programmeIds: has ? nc.programmeIds.filter((p) => p !== programmeId) : [...nc.programmeIds, programmeId],
        })
    }

    async function handleCommit() {
        const rows = preview
            .filter((r) => r.include)
            .map((r) => ({
                courseIds: r.matches.map((m) => m.id),
                newCourses: createMissing ? r.newCourses : [],
                lecturers: r.lecturers,
            }))
            .filter((r) => r.courseIds.length > 0 || r.newCourses.length > 0)

        if (rows.length === 0) { setError('Nothing selected to import.'); return }

        const incomplete = rows.flatMap((r) => r.newCourses).filter((nc) => !nc.unit || !nc.levelId)
        if (incomplete.length > 0) {
            setError(`${incomplete.length} new course(s) still need a unit and a level: ${incomplete.map((c) => c.code).join(', ')}`)
            return
        }

        setError(null); setMessage(null); setBusy(true)
        try {
            const result = await commitAllocationDoc({ academicSessionId: Number(sessionId), semester, rows })
            setMessage(
                `Imported. Courses created: ${result.coursesCreated} · open courses added: ${result.courseSlotsOpened} · ` +
                `lecturers listed: ${result.lecturersListed} · already listed: ${result.alreadyListed}.` +
                (result.notes.length ? ` Notes: ${result.notes.join(' ')}` : '')
            )
            setPreview(null); setFile(null)
            await loadEntries()
            setAllCourses(await getAllCourses())
            setTab('course')
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const stat = (label, value) => (
        <div className="card p-4"><p className="text-2xl font-bold">{value}</p><p className="text-xs theme-text-muted">{label}</p></div>
    )

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Course Allocations</h2>
            <p className="theme-text-muted text-sm mb-6">
                Upload the department's allocation document, or build the list by hand. Lecturers listed here confirm their own courses,
                and lecturers with an account can claim any course that has no lecturer yet. A course can have more than one lecturer.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => { setSessionId(e.target.value); setEntries(null); setPreview(null) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => { setSemester(e.target.value); setEntries(null); setPreview(null) }}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
                <button onClick={handleLoad} disabled={!sessionId || busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Working…' : 'Load'}
                </button>
            </div>

            {sessionId && (
                <div className="flex flex-wrap gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                    {TABS.map(([key, label]) => (
                        <button key={key} onClick={() => setTab(key)}
                                className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>
                            {label}
                        </button>
                    ))}
                </div>
            )}

            {entries && (tab === 'course' || tab === 'lecturer') && (
                <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mb-6">
                    {stat('Courses on the list', courses.length)}
                    {stat('Lecturers listed', named.length)}
                    {stat('Confirmed / claimed', claimed)}
                    {stat('Waiting to confirm', named.length - claimed)}
                    {stat('Courses with no lecturer', openCount)}
                </div>
            )}

            {/* ---------------- By course ---------------- */}
            {entries && tab === 'course' && (
                <div className="card overflow-hidden">
                    <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold flex justify-between items-center">
                        <span>Courses ({shownCourses.length})</span>
                        <label className="text-xs font-normal flex items-center gap-2">
                            <input type="checkbox" checked={onlyOpen} onChange={(e) => setOnlyOpen(e.target.checked)} />
                            Only courses with no lecturer
                        </label>
                    </div>

                    {shownCourses.length === 0 ? (
                        <p className="p-6 text-sm theme-text-muted">
                            {courses.length === 0
                                ? 'Nothing is listed for this session and semester yet. Upload the document or add a course.'
                                : 'Every course has a lecturer.'}
                        </p>
                    ) : (
                        <ul className="divide-y theme-border">
                            {shownCourses.map((c) => (
                                <li key={c.courseId} className="px-4 py-3">
                                    <div className="flex justify-between gap-4">
                                        <div>
                                            <p className="font-semibold">{c.code} <span className="font-normal theme-text-muted">— {c.title}</span></p>
                                            <p className="text-xs theme-text-muted">{c.unit} unit(s) · {c.status} · {c.level}</p>
                                        </div>
                                        <button onClick={() => setAddingFor(addingFor === c.courseId ? null : c.courseId)}
                                                className="text-xs font-medium flex items-center gap-1 shrink-0" style={{ color: 'var(--accent)' }}>
                                            <Plus size={13} /> Add lecturer
                                        </button>
                                    </div>

                                    <div className="flex flex-wrap gap-2 mt-2">
                                        {c.rows.filter((r) => r.lecturerId || r.lecturerName).map((r) => (
                                            <span key={r.id}
                                                  className={`inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full font-medium ${
                                                      r.claimed ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                        {r.claimed ? <Check size={12} /> : <Clock size={12} />}
                                                {r.lecturerName}
                                                {!r.claimed && (
                                                    <select defaultValue="" disabled={busy}
                                                            onChange={(e) => e.target.value && run(() => linkAllocation(r.id, Number(e.target.value)), 'Linked to the account.')}
                                                            className="bg-transparent text-[11px] border-0 outline-none max-w-[110px]">
                                                        <option value="">link account…</option>
                                                        {accounts.map((a) => <option key={a.id} value={a.id} className="text-black">{a.fullName}</option>)}
                                                    </select>
                                                )}
                                                <button title="Remove" disabled={busy}
                                                        onClick={() => window.confirm(`Remove ${r.lecturerName} from ${c.code}?`) && run(() => removeAllocation(r.id), 'Removed.')}>
                          <X size={12} />
                        </button>
                      </span>
                                        ))}
                                        {!hasLecturer(c) && <span className="text-xs italic theme-text-muted">No lecturer yet: open to claim</span>}
                                    </div>

                                    {addingFor === c.courseId && (
                                        <AddLecturerBox accounts={accounts} onCancel={() => setAddingFor(null)}
                                                        onAdd={(who) => {
                                                            setAddingFor(null)
                                                            run(() => addLecturer({ courseId: c.courseId, academicSessionId: Number(sessionId), semester, ...who }), 'Lecturer added.')
                                                        }} />
                                    )}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            {/* ---------------- By lecturer ---------------- */}
            {entries && tab === 'lecturer' && (
                <div className="grid grid-cols-2 gap-4">
                    {lecturers.length === 0 && <p className="theme-text-muted text-sm">No lecturers are listed yet.</p>}
                    {lecturers.map((l) => (
                        <div key={l.key} className="card overflow-hidden">
                            <div className={`px-4 py-2 text-sm font-semibold flex justify-between ${l.claimed ? 'bg-green-600 text-white' : 'bg-amber-500 text-white'}`}>
                                <span>{l.name}</span>
                                <span className="text-xs font-normal">{l.claimed ? 'Confirmed' : 'Waiting to confirm'}</span>
                            </div>
                            <ul className="divide-y theme-border">
                                {l.rows.map((r) => (
                                    <li key={r.id} className="px-4 py-2 flex justify-between items-start gap-3">
                                        <div>
                                            <p className="text-sm font-semibold">{r.courseCode}</p>
                                            <p className="text-xs theme-text-muted">{r.courseTitle} · {r.creditUnit} unit(s) · {r.levelName}</p>
                                        </div>
                                        <div className="flex items-center gap-2 shrink-0">
                                            {!l.claimed && (
                                                <select defaultValue="" disabled={busy}
                                                        onChange={(e) => e.target.value && run(() => linkAllocation(r.id, Number(e.target.value)), 'Linked to the account.')}
                                                        className="border theme-border rounded px-1 py-0.5 text-[11px] max-w-[120px]">
                                                    <option value="">link account…</option>
                                                    {accounts.map((a) => <option key={a.id} value={a.id}>{a.fullName}</option>)}
                                                </select>
                                            )}
                                            <button title="Remove" disabled={busy}
                                                    onClick={() => window.confirm(`Remove ${l.name} from ${r.courseCode}?`) && run(() => removeAllocation(r.id), 'Removed.')}>
                                                <X size={14} />
                                            </button>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>
            )}

            {/* ---------------- Upload document ---------------- */}
            {sessionId && tab === 'upload' && (
                <div className="card p-4">
                    <h3 className="font-semibold mb-1">Upload allocation document</h3>
                    <p className="text-xs theme-text-muted mb-3">
                        Choose the department's allocation document (Word). You'll see a preview to review and correct before anything is saved.
                    </p>

                    <div className="flex gap-3 items-center flex-wrap">
                        <input type="file" accept=".docx"
                               onChange={(e) => { setFile(e.target.files?.[0] || null); setPreview(null) }}
                               className="text-sm" />
                        <button onClick={handlePreview} disabled={!file || busy}
                                className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {busy && !preview ? 'Reading…' : 'Preview'}
                        </button>
                    </div>

                    {preview && (
                        <div className="mt-5">
                            <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
                                <p className="text-sm font-medium">
                                    {preview.length} row(s) found · {preview.filter((r) => r.include).length} selected
                                </p>
                                <label className="text-sm flex items-center gap-2">
                                    <input type="checkbox" checked={createMissing} onChange={(e) => setCreateMissing(e.target.checked)} />
                                    Create courses that don't exist yet
                                </label>
                            </div>

                            <ul className="divide-y theme-border border theme-border rounded-lg">
                                {preview.map((r, i) => (
                                    <li key={i} className={`p-3 ${r.include ? '' : 'opacity-50'}`}>
                                        <label className="flex items-start gap-3">
                                            <input type="checkbox" checked={r.include} className="mt-1"
                                                   onChange={(e) => patchRow(i, { include: e.target.checked })} />
                                            <div className="flex-1 min-w-0">
                                                <div className="flex flex-wrap gap-1.5">
                                                    {r.matches.map((m) => (
                                                        <span key={m.id} className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-medium">
                              {m.code}
                            </span>
                                                    ))}
                                                    {r.matches.length === 0 && r.newCourses.length === 0 && (
                                                        <span className="text-xs italic theme-text-muted">No course recognised in this row</span>
                                                    )}
                                                </div>
                                                <p className="text-xs theme-text-muted mt-1">
                                                    Lecturers: {r.lecturers.length ? r.lecturers.join(', ') : 'none listed'}
                                                </p>
                                                {r.warnings.map((w, k) => (
                                                    <p key={k} className="text-xs text-amber-700 mt-1">{w}</p>
                                                ))}
                                            </div>
                                        </label>

                                        {createMissing && r.include && r.newCourses.map((nc, j) => (
                                            <div key={j} className="mt-3 ml-7 p-3 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                                <p className="text-xs font-semibold mb-2">New course to create</p>
                                                <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
                                                    <input value={nc.code || ''} placeholder="Code"
                                                           onChange={(e) => patchNew(i, j, { code: e.target.value })}
                                                           className="border theme-border rounded-lg px-2 py-1 text-sm" />
                                                    <input value={nc.title || ''} placeholder="Title"
                                                           onChange={(e) => patchNew(i, j, { title: e.target.value })}
                                                           className="border theme-border rounded-lg px-2 py-1 text-sm md:col-span-2" />
                                                    <input type="number" min="1" value={nc.unit ?? ''} placeholder="Units"
                                                           onChange={(e) => patchNew(i, j, { unit: e.target.value === '' ? null : Number(e.target.value) })}
                                                           className="border theme-border rounded-lg px-2 py-1 text-sm" />
                                                </div>
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 mt-2">
                                                    <select value={nc.levelId ?? ''}
                                                            onChange={(e) => patchNew(i, j, { levelId: e.target.value ? Number(e.target.value) : null })}
                                                            className="border theme-border rounded-lg px-2 py-1 text-sm">
                                                        <option value="">Level…</option>
                                                        {levels.map((lv) => <option key={lv.id} value={lv.id}>{lv.name}</option>)}
                                                    </select>
                                                    <select value={nc.status || 'C'}
                                                            onChange={(e) => patchNew(i, j, { status: e.target.value })}
                                                            className="border theme-border rounded-lg px-2 py-1 text-sm">
                                                        <option value="C">C: Compulsory</option>
                                                        <option value="R">R: Required</option>
                                                        <option value="E">E: Elective</option>
                                                    </select>
                                                </div>
                                                <div className="flex flex-wrap gap-3 mt-2">
                                                    {programmes.map((p) => (
                                                        <label key={p.id} className="text-xs flex items-center gap-1.5">
                                                            <input type="checkbox" checked={(nc.programmeIds || []).includes(p.id)}
                                                                   onChange={() => toggleProgramme(i, j, p.id)} />
                                                            {p.name}
                                                        </label>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                    </li>
                                ))}
                            </ul>

                            <div className="flex gap-2 mt-4">
                                <button onClick={handleCommit} disabled={busy}
                                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                    {busy ? 'Importing…' : 'Import selected rows'}
                                </button>
                                <button onClick={() => setPreview(null)} disabled={busy}
                                        className="text-sm theme-text-muted hover:underline">
                                    Discard preview
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* ---------------- Add a course ---------------- */}
            {sessionId && tab === 'offer' && (
                <div className="card p-4">
                    <h3 className="font-semibold mb-1">Add a course to this list</h3>
                    <p className="text-xs theme-text-muted mb-3">
                        Puts an existing course on the list with no lecturer, so a lecturer with an account can claim it.
                        {' '}{notListed.length} course(s) not yet listed for this semester. Create new courses on the Create Course page first.
                    </p>

                    <div className="flex gap-3 items-end flex-wrap">
                        <select value={offerId} onChange={(e) => setOfferId(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm min-w-[260px]">
                            <option value="">Select a course…</option>
                            {notListed.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}
                        </select>
                        <button disabled={!offerId || busy}
                                onClick={() => run(async () => {
                                    await offerCourse(Number(offerId), Number(sessionId), semester)
                                    setOfferId('')
                                }, 'Course added to the list.')}
                                className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            Add
                        </button>
                    </div>

                    {notListed.length === 0 && <p className="text-sm theme-text-muted mt-3">Every course is already on the list.</p>}
                </div>
            )}

            {!sessionId && <p className="text-sm theme-text-muted">Select a session and semester, then press Load to begin.</p>}
        </div>
    )
}

export default CourseAllocations