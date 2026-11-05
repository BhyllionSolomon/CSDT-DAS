import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { DESIGNATIONS, designationLabel, classLabel, CLASS_STYLE } from '../utils/projects'
import {
    getStaff, saveStaff, getProposal, confirmAllocation, getAssignments, removeAssignment,
} from '../services/projectService'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '')

function FinalYearProjects() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [staff, setStaff] = useState([])
    const [assignments, setAssignments] = useState([])
    const [proposal, setProposal] = useState(null)
    const [showSettings, setShowSettings] = useState(false)
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

    async function refresh() {
        const [s, a] = await Promise.all([getStaff(), getAssignments(sessionId)])
        setStaff(s)
        setAssignments(a)
    }

    useEffect(() => {
        if (!sessionId) return
        setProposal(null); setError(null)
        refresh().catch((e) => setError(msg(e)))
    }, [sessionId])

    async function act(action, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            const result = await action()
            if (success) setMessage(typeof success === 'function' ? success(result) : success)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function patch(userId, change) {
        setStaff((rows) => rows.map((r) => {
            if (r.userId === userId) return { ...r, ...change }
            // only one Project Coordinator and one Assistant Project Coordinator
            if (change.coordinator) return { ...r, coordinator: false }
            if (change.assistantCoordinator) return { ...r, assistantCoordinator: false }
            return r
        }))
    }

    function handleSaveSettings() {
        act(async () => {
            const rows = await saveStaff(staff.map((r) => ({
                userId: r.userId, designation: r.designation, eligible: r.eligible,
                coordinator: r.coordinator, assistantCoordinator: r.assistantCoordinator,
            })))
            setStaff(rows)
        }, 'Supervisor settings saved.')
    }

    function handlePropose() {
        setProposal(null)
        act(async () => { setProposal(await getProposal(sessionId)) })
    }

    function handleConfirm() {
        const pairs = proposal.groups.flatMap((g) => g.students.map((s) => ({ studentId: s.studentId, supervisorId: g.supervisorId })))
        if (pairs.length === 0) { setError('There are no students to assign.'); return }

        if (!window.confirm(`Assign ${pairs.length} student(s) to ${proposal.supervisors} supervisor(s) for ${proposal.sessionName}?\n\nEveryone is saved together, or nobody is.`)) return

        act(async () => {
            const result = await confirmAllocation(Number(sessionId), pairs)
            setProposal(null)
            await refresh()
            return result
        }, (r) => r.message)
    }

    function handleRemove(a) {
        if (!window.confirm(`Remove ${a.fullName} from ${a.supervisorName}? They will be unassigned and can be allocated again.`)) return
        act(async () => { await removeAssignment(a.id); await refresh() }, 'Assignment removed.')
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const eligibleCount = staff.filter((s) => s.eligible).length

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Final-Year Projects</h2>
            <p className="theme-text-muted text-sm mb-6">
                Allocate every unassigned 400 level student to a project supervisor in one step. First Class and Second Class Upper students are
                shared out first, one for each supervisor, and the rest are spread as evenly as possible. You review the allocation before anything is saved.
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
                <button onClick={handlePropose} disabled={busy || !sessionId}
                        className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                    {busy && !proposal ? 'Working…' : 'Assign Students to Supervisors'}
                </button>
                <p className="text-xs theme-text-muted pb-2">{eligibleCount} supervisor(s) ticked · {assignments.length} student(s) already assigned</p>
            </div>

            {proposal && (
                <div className="space-y-4 mb-8">
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                        {[
                            ['Students to assign', proposal.eligibleStudents],
                            ['Supervisors', proposal.supervisors],
                            ['First Class', proposal.firstClass],
                            ['Second Class Upper', proposal.upperSecond],
                            ['Others', proposal.others],
                        ].map(([label, value]) => (
                            <div key={label} className="card p-4">
                                <p className="text-2xl font-bold">{value}</p>
                                <p className="text-xs theme-text-muted">{label}</p>
                            </div>
                        ))}
                    </div>

                    {proposal.notes.map((n, i) => (
                        <div key={i} className="px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">{n}</div>
                    ))}

                    {proposal.groups.map((g) => (
                        <div key={g.supervisorId} className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-4 flex-wrap">
                <span className="font-semibold text-sm">
                  {g.fullName}
                    <span className="font-normal text-white/60"> · {designationLabel(g.designation)}</span>
                    {g.coordinator && <span className="ml-2 text-[10px] uppercase bg-white/15 px-1.5 py-0.5 rounded">Project Coordinator</span>}
                    {g.assistantCoordinator && <span className="ml-2 text-[10px] uppercase bg-white/15 px-1.5 py-0.5 rounded">Assistant Coordinator</span>}
                </span>
                                <span className="text-xs text-white/70">
                  {g.proposed} new{g.existing > 0 ? ` · ${g.existing} already assigned` : ''} · {g.firstClass} First · {g.upperSecond} Upper · {g.others} other
                </span>
                            </div>
                            {g.students.length === 0 ? (
                                <p className="px-4 py-3 text-sm theme-text-muted">No student for this supervisor in this round.</p>
                            ) : (
                                <ul className="divide-y theme-border">
                                    {g.students.map((s) => (
                                        <li key={s.studentId} className="px-4 py-2 flex items-center gap-3 text-sm">
                                            <span className="w-40">{s.matricNumber}</span>
                                            <span className="flex-1">{s.fullName} <span className="theme-text-muted">· {s.programme}</span></span>
                                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${CLASS_STYLE[s.classification]}`}>{classLabel(s.classification)}</span>
                                            <span className="w-12 text-right theme-text-muted">{s.cgpa != null ? Number(s.cgpa).toFixed(2) : '—'}</span>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    ))}

                    <div className="flex gap-3">
                        <button onClick={handleConfirm} disabled={busy || proposal.eligibleStudents === 0}
                                className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                            {busy ? 'Saving…' : 'Confirm allocation'}
                        </button>
                        <button onClick={() => setProposal(null)} disabled={busy} className="text-sm theme-text-muted hover:underline">Discard</button>
                    </div>
                </div>
            )}

            <div className="card mb-8">
                <button onClick={() => setShowSettings(!showSettings)}
                        className="w-full text-left px-4 py-3 font-semibold flex justify-between items-center">
                    <span>Supervisor settings</span>
                    <span className="text-xs font-normal theme-text-muted">{showSettings ? 'Hide' : 'Show'}</span>
                </button>

                {showSettings && (
                    <div className="px-4 pb-4">
                        <p className="text-xs theme-text-muted mb-3">
                            Tick the lecturers who supervise final-year projects and set each one's designation. The Head of Department, senior lecturers and the two
                            coordinators are first in line for any extra students. Your own lecturer account can carry the Head of Department designation.
                        </p>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr>
                                    <th className="px-3 py-2">Supervises</th>
                                    <th className="px-3 py-2">Name</th>
                                    <th className="px-3 py-2">Account</th>
                                    <th className="px-3 py-2">Designation</th>
                                    <th className="px-3 py-2">Project Coordinator</th>
                                    <th className="px-3 py-2">Assistant Coordinator</th>
                                    <th className="px-3 py-2">This session</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {staff.map((r) => (
                                    <tr key={r.userId}>
                                        <td className="px-3 py-2"><input type="checkbox" checked={r.eligible} onChange={(e) => patch(r.userId, { eligible: e.target.checked })} /></td>
                                        <td className="px-3 py-2 font-medium">{r.fullName}</td>
                                        <td className="px-3 py-2 text-xs theme-text-muted">{r.role}</td>
                                        <td className="px-3 py-2">
                                            <select value={r.designation} onChange={(e) => patch(r.userId, { designation: e.target.value })}
                                                    className="border theme-border rounded-lg px-2 py-1 text-sm">
                                                {DESIGNATIONS.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                                            </select>
                                        </td>
                                        <td className="px-3 py-2"><input type="checkbox" checked={r.coordinator} onChange={(e) => patch(r.userId, { coordinator: e.target.checked })} /></td>
                                        <td className="px-3 py-2"><input type="checkbox" checked={r.assistantCoordinator} onChange={(e) => patch(r.userId, { assistantCoordinator: e.target.checked })} /></td>
                                        <td className="px-3 py-2">{r.currentLoad}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <button onClick={handleSaveSettings} disabled={busy}
                                className="mt-4 accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            Save settings
                        </button>
                    </div>
                )}
            </div>

            <div className="card overflow-hidden">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Current assignments ({assignments.length})</div>
                {assignments.length === 0 ? (
                    <p className="p-6 text-sm theme-text-muted">No student has been assigned for this session yet.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                            <tr>
                                <th className="px-4 py-2">Student</th>
                                <th className="px-4 py-2">Class</th>
                                <th className="px-4 py-2">Supervisor</th>
                                <th className="px-4 py-2">Assigned</th>
                                <th className="px-4 py-2"></th>
                            </tr>
                            </thead>
                            <tbody className="divide-y theme-border">
                            {assignments.map((a) => (
                                <tr key={a.id}>
                                    <td className="px-4 py-2">{a.matricNumber} <span className="theme-text-muted">· {a.fullName}</span></td>
                                    <td className="px-4 py-2">
                                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${CLASS_STYLE[a.classification]}`}>{classLabel(a.classification)}</span>
                                    </td>
                                    <td className="px-4 py-2">{a.supervisorName} <span className="theme-text-muted">· {designationLabel(a.designation)}</span></td>
                                    <td className="px-4 py-2 theme-text-muted">{when(a.assignedAt)}</td>
                                    <td className="px-4 py-2 text-right">
                                        <button onClick={() => handleRemove(a)} disabled={busy} className="text-xs text-red-600 hover:underline">Remove</button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    )
}

export default FinalYearProjects