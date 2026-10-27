import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, UploadCloud } from 'lucide-react'
import api from '../services/api'
import { getAllSessions } from '../services/sessionService'
import { importAllocationDoc } from '../services/allocationService'

const msg = (err) => err.response?.data?.message || err.message

function BulkUploadAllocation() {
    const [sessions, setSessions] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('')
    const [file, setFile] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [result, setResult] = useState(null)
    const [picks, setPicks] = useState({})
    const [saved, setSaved] = useState({})

    useEffect(() => {
        Promise.all([getAllSessions(), api.get('/programmes')])
            .then(([s, p]) => { setSessions(s); setProgrammes(p.data) })
            .catch((err) => setError(msg(err)))
    }, [])

    async function handleUpload() {
        setError(null); setResult(null); setPicks({}); setSaved({}); setBusy(true)
        try {
            setResult(await importAllocationDoc(file, sessionId, semester))
        } catch (err) {
            setError(msg(err))
        } finally { setBusy(false) }
    }

    function toggle(courseId, programmeId) {
        setPicks((p) => {
            const current = p[courseId] || []
            return {
                ...p,
                [courseId]: current.includes(programmeId) ? current.filter((x) => x !== programmeId) : [...current, programmeId],
            }
        })
    }

    async function saveProgrammes(course) {
        setError(null)
        try {
            await api.put(`/courses/${course.id}/programmes`, { programmeIds: picks[course.id] || [] })
            setSaved((s) => ({ ...s, [course.id]: true }))
        } catch (err) { setError(msg(err)) }
    }

    const stats = result && [
        ['Courses created', result.coursesCreated],
        ['Open courses (no lecturer)', result.courseSlotsOpened],
        ['Lecturers listed', result.lecturersListed],
        ['Already listed', result.alreadyListed],
    ]

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Bulk Upload Course Allocation</h2>
            <p className="theme-text-muted text-sm mb-6">
                Upload the department's allocation document (Word). CSDT reads each course (code, title, unit, status) and the lecturers named
                against it, creates any courses that are not in the system yet, and sets up the allocations. Lecturers then confirm their own
                courses on Claim Courses. Phone numbers come from the document if it has them; otherwise from the lecturer's account.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6">
                <div className="flex gap-4 items-end flex-wrap">
                    <div>
                        <label className="block text-sm font-medium mb-1">Session</label>
                        <select value={sessionId} onChange={(e) => setSessionId(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Read from the document</option>
                            {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Semester</label>
                        <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Read from the document</option>
                            <option value="FIRST">First</option>
                            <option value="SECOND">Second</option>
                        </select>
                    </div>
                    <input type="file" accept=".docx" onChange={(e) => setFile(e.target.files?.[0] || null)} className="text-sm" />
                    <button onClick={handleUpload} disabled={!file || busy}
                            className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50 flex items-center gap-2">
                        <UploadCloud size={16} /> {busy ? 'Working…' : 'Upload & Create Allocations'}
                    </button>
                </div>
            </div>

            {result && (
                <div className="space-y-4">
                    <div className="card p-4">
                        <p className="font-semibold">
                            {result.semester === 'FIRST' ? 'First' : 'Second'} semester, {result.sessionName}
                            <span className="font-normal theme-text-muted"> · {result.rowsRead} course rows read</span>
                        </p>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
                            {stats.map(([label, value]) => (
                                <div key={label} className="rounded-lg p-3" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                    <p className="text-2xl font-bold">{value}</p>
                                    <p className="text-xs theme-text-muted">{label}</p>
                                </div>
                            ))}
                        </div>
                        <Link to="/course-allocations" className="inline-block mt-3 text-sm font-medium" style={{ color: 'var(--accent)' }}>
                            View the allocations →
                        </Link>
                    </div>

                    {result.notes.length > 0 && (
                        <div className="card p-4">
                            <p className="font-semibold mb-2">Needs your attention ({result.notes.length})</p>
                            <ul className="space-y-1 text-sm text-amber-700">
                                {result.notes.map((n, i) => <li key={i}>• {n}</li>)}
                            </ul>
                        </div>
                    )}

                    {result.needProgramme.length > 0 && (
                        <div className="card p-4">
                            <p className="font-semibold mb-1">Choose the programmes ({result.needProgramme.length})</p>
                            <p className="text-xs theme-text-muted mb-3">
                                These courses were created, but their programme could not be worked out from the code. Tick the programmes they belong to,
                                otherwise students will not see them when registering.
                            </p>
                            <ul className="divide-y theme-border">
                                {result.needProgramme.map((c) => (
                                    <li key={c.id} className="py-3 flex items-center justify-between gap-4 flex-wrap">
                                        <div>
                                            <p className="font-medium">{c.code}</p>
                                            <p className="text-xs theme-text-muted">{c.title}</p>
                                        </div>
                                        <div className="flex items-center gap-3 flex-wrap">
                                            {programmes.map((p) => (
                                                <label key={p.id} className="text-xs flex items-center gap-1.5">
                                                    <input type="checkbox" checked={(picks[c.id] || []).includes(p.id)}
                                                           onChange={() => toggle(c.id, p.id)} />
                                                    {p.name}
                                                </label>
                                            ))}
                                            {saved[c.id] ? (
                                                <span className="text-xs text-green-700 flex items-center gap-1"><Check size={13} /> Saved</span>
                                            ) : (
                                                <button onClick={() => saveProgrammes(c)} disabled={!(picks[c.id] || []).length}
                                                        className="accent-gradient text-white rounded-lg px-3 py-1 text-xs font-medium btn-press disabled:opacity-50">
                                                    Save
                                                </button>
                                            )}
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default BulkUploadAllocation