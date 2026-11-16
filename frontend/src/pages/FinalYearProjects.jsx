import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { DESIGNATIONS, designationLabel, classLabel, CLASS_STYLE } from '../utils/projects'
import {
    getStaff, saveStaff, getAssignments, getLatestDraft, createDraft, moveDraftItem, publishDraft,
} from '../services/projectService'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleString('en-GB') : '—')
const isFirst = (cls) => cls === 'FIRST'

export default function FinalYearProjects() {
    const { user } = useAuth()
    const isHod = ['HOD', 'ADMIN'].includes(String(user?.role || '').toUpperCase())
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [staff, setStaff] = useState([])
    const [assignments, setAssignments] = useState([])
    const [draft, setDraft] = useState(null)
    const [showSettings, setShowSettings] = useState(false)
    const [showHistory, setShowHistory] = useState(false)
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [message, setMessage] = useState('')

    useEffect(() => {
        getAllSessions().then((rows) => {
            setSessions(rows || [])
            const current = currentSessionOf(rows || [])
            if (current) setSessionId(String(current.id))
        }).catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    const refresh = useCallback(async () => {
        const [s, a, latest] = await Promise.all([getStaff(), getAssignments(sessionId), getLatestDraft(sessionId)])
        // API responses are normalised before rendering so an empty or unexpected
        // response cannot cause .filter/.map crashes in the allocation screen.
        setStaff(Array.isArray(s) ? s : [])
        setAssignments(Array.isArray(a) ? a : [])
        setDraft(latest && typeof latest === 'object' ? {
            ...latest,
            items: Array.isArray(latest.items) ? latest.items : [],
            history: Array.isArray(latest.history) ? latest.history : [],
        } : null)
    }, [sessionId])

    useEffect(() => {
        if (!sessionId) return
        Promise.resolve().then(() => refresh()).catch((e) => setError(msg(e)))
    }, [sessionId, refresh])

    async function run(fn, successText) {
        setBusy(true); setError(''); setMessage('')
        try {
            const result = await fn()
            if (successText) setMessage(typeof successText === 'function' ? successText(result) : successText)
            return result
        } catch (e) {
            setError(msg(e))
            return null
        } finally { setBusy(false) }
    }

    function patch(userId, change) {
        setStaff(rows => rows.map(r => {
            if (r.userId === userId) return { ...r, ...change }
            if (change.coordinator) return { ...r, coordinator: false }
            if (change.assistantCoordinator) return { ...r, assistantCoordinator: false }
            return r
        }))
    }

    async function saveSettings() {
        const result = await run(() => saveStaff(staff.map(r => ({
            userId: r.userId, designation: r.designation, eligible: r.eligible,
            firstClassEligible: r.firstClassEligible, coordinator: r.coordinator,
            assistantCoordinator: r.assistantCoordinator,
        }))), 'Supervisor settings saved.')
        if (result) setStaff(result)
    }

    async function generate() {
        const result = await run(() => createDraft(Number(sessionId)))
        if (result) {
            setDraft(result)
            setMessage(`Draft generated with ${result.items.length} proposed assignments. Nothing has been published.`)
        }
    }

    async function move(item, supervisorId) {
        const result = await run(() => moveDraftItem(draft.draftId, item.itemId, Number(supervisorId),
            'Changed during Project Coordinator review.'))
        if (result) setDraft(result)
    }

    async function publish() {
        if (!window.confirm('Publish this reviewed allocation? It will become the official project supervision schedule visible to students and staff.')) return
        const result = await run(() => publishDraft(draft.draftId))
        if (result) {
            setDraft(result)
            await refresh()
            setMessage('Allocation published. The public schedule is now available to students and staff.')
        }
    }

    const supervisors = useMemo(() => staff.filter(s => s.eligible), [staff])
    const groups = useMemo(() => supervisors.map(s => ({
        ...s, items: draftItems.filter(i => i.supervisorId === s.userId),
    })), [supervisors, draft])
    const draftItems = Array.isArray(draft?.items) ? draft.items : []
    const draftHistory = Array.isArray(draft?.history) ? draft.history : []
    const firstCount = draftItems.filter(i => isFirst(i.classification)).length

    if (loading) return <p className="theme-text-muted">Loading project allocation…</p>

    return (
        <div className="max-w-6xl space-y-5">
            <div>
                <h2 className="text-2xl font-bold mb-1">Final-Year Project Allocation</h2>
                <p className="theme-text-muted text-sm">
                    First Class students are assigned first, round-robin among the supervisors selected by the H.O.D.
                    Upper Second, Lower Second and Third Class students are then distributed across all eligible supervisors
                    to balance total workloads. The Project Coordinator reviews and edits the draft before publication.
                </p>
            </div>

            {error && <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="card p-4 flex flex-wrap gap-4 items-end">
                <div>
                    <label className="block text-sm font-medium mb-1">Academic session</label>
                    <select value={sessionId} onChange={e => setSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <button onClick={generate} disabled={busy || !sessionId}
                        className="accent-gradient text-white rounded-lg px-5 py-2.5 text-sm font-semibold disabled:opacity-50">
                    {busy ? 'Working…' : 'Assign / Generate Draft'}
                </button>
                <span className="text-xs theme-text-muted pb-2">{supervisors.length} eligible supervisors · {assignments.length} published/previous assignments</span>
            </div>

            {draft && (
                <section className="space-y-4">
                    <div className="card p-4">
                        <div className="flex flex-wrap justify-between gap-3 items-center">
                            <div>
                                <h3 className="font-semibold text-lg">Allocation review — {draft.sessionName}</h3>
                                <p className="text-xs theme-text-muted">Draft #{draft.draftId} · {draft.status} · Last updated {when(draft.updatedAt)}</p>
                            </div>
                            <span className={`text-xs rounded-full px-3 py-1 ${draft.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' : 'bg-amber-100 text-amber-800'}`}>
                                {draft.status}
                            </span>
                        </div>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4">
                            {[
                                ['Students', draftItems.length],
                                ['Supervisors used', new Set(draftItems.map(i => i.supervisorId)).size],
                                ['First Class', firstCount],
                                ['Other classifications', draftItems.length - firstCount],
                            ].map(([label, value]) => <div key={label} className="rounded-lg p-3" style={{ background: 'var(--surface-soft)' }}>
                                <p className="text-2xl font-bold">{value}</p><p className="text-xs theme-text-muted">{label}</p>
                            </div>)}
                        </div>
                    </div>

                    {groups.map(g => (
                        <div className="card overflow-hidden" key={g.userId}>
                            <div className="theme-sidebar text-white px-4 py-3 flex justify-between gap-3 flex-wrap">
                                <strong className="text-sm">{g.fullName} <span className="font-normal text-white/70">· {designationLabel(g.designation)}</span></strong>
                                <span className="text-xs text-white/75">{g.items.length} student(s)</span>
                            </div>
                            {g.items.length === 0 ? <p className="p-3 text-sm theme-text-muted">No students assigned in this draft.</p> : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead className="theme-text-muted text-xs uppercase" style={{ background: 'var(--surface-soft)' }}>
                                            <tr><th className="p-3 text-left">Student</th><th className="p-3 text-left">Programme</th><th className="p-3 text-left">Classification</th><th className="p-3 text-left">CGPA</th><th className="p-3 text-left">Change supervisor</th></tr>
                                        </thead>
                                        <tbody className="divide-y theme-border">
                                            {g.items.map(item => (
                                                <tr key={item.itemId}>
                                                    <td className="p-3"><span className="font-medium">{item.fullName}</span><br/><span className="text-xs theme-text-muted">{item.matricNumber}</span></td>
                                                    <td className="p-3">{item.programme}</td>
                                                    <td className="p-3"><span className={`text-xs px-2 py-1 rounded-full ${CLASS_STYLE[item.classification]}`}>{classLabel(item.classification)}</span></td>
                                                    <td className="p-3">{item.cgpa == null ? '—' : Number(item.cgpa).toFixed(2)}</td>
                                                    <td className="p-3">
                                                        <select disabled={busy || draft.status !== 'DRAFT'} value={item.supervisorId}
                                                            onChange={e => move(item, e.target.value)}
                                                            className="border theme-border rounded-lg px-2 py-1.5 text-xs max-w-56">
                                                            {supervisors.filter(s => !isFirst(item.classification) || s.firstClassEligible)
                                                                .map(s => <option key={s.userId} value={s.userId}>{s.fullName}</option>)}
                                                        </select>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    ))}

                    {draft.status === 'DRAFT' && (
                        <div className="flex gap-3 flex-wrap">
                            <button onClick={publish} disabled={busy || draft.items.length === 0}
                                className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold disabled:opacity-50">
                                {busy ? 'Saving…' : 'Publish Approved Allocation'}
                            </button>
                            <span className="self-center text-xs theme-text-muted">Changes are saved as you move students. Publishing makes the schedule official.</span>
                        </div>
                    )}
                    <div className="card p-4">
                        <button onClick={() => setShowHistory(v => !v)} className="font-semibold text-sm">
                            {showHistory ? 'Hide' : 'View'} allocation history ({draftHistory.length})
                        </button>
                        {showHistory && <div className="mt-3 space-y-2">
                            {draftHistory.map((h, i) => <div key={i} className="text-xs border-l-2 theme-border pl-3 py-1">
                                <strong>{h.action.replaceAll('_', ' ')}</strong> · {h.matricNumber || 'Whole draft'} · {h.previousSupervisor || '—'} → {h.newSupervisor || '—'}
                                <div className="theme-text-muted">{h.changedBy} · {when(h.changedAt)} {h.note ? `· ${h.note}` : ''}</div>
                            </div>)}
                        </div>}
                    </div>
                </section>
            )}

            {isHod && (
                <div className="card">
                    <button onClick={() => setShowSettings(v => !v)} className="w-full p-4 text-left font-semibold flex justify-between">
                        <span>H.O.D. supervisor settings</span><span className="text-xs theme-text-muted">{showSettings ? 'Hide' : 'Show'}</span>
                    </button>
                    {showSettings && <div className="px-4 pb-4">
                        <p className="text-xs theme-text-muted mb-3">Select all project supervisors, then separately select the subset eligible to receive First Class students.</p>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead className="theme-text-muted text-xs uppercase"><tr>
                                    <th className="p-2 text-left">Supervises</th><th className="p-2 text-left">First Class eligible</th>
                                    <th className="p-2 text-left">Lecturer</th><th className="p-2 text-left">Designation</th>
                                    <th className="p-2 text-left">Project Coordinator</th><th className="p-2 text-left">Assistant Coordinator</th>
                                </tr></thead>
                                <tbody className="divide-y theme-border">{staff.map(r => <tr key={r.userId}>
                                    <td className="p-2"><input type="checkbox" checked={r.eligible} onChange={e => patch(r.userId, { eligible: e.target.checked, firstClassEligible: e.target.checked ? r.firstClassEligible : false })}/></td>
                                    <td className="p-2"><input type="checkbox" checked={r.firstClassEligible} disabled={!r.eligible}
                                        onChange={e => patch(r.userId, { firstClassEligible: e.target.checked })}/></td>
                                    <td className="p-2 font-medium">{r.fullName}</td>
                                    <td className="p-2"><select value={r.designation} onChange={e => patch(r.userId, { designation: e.target.value })}
                                        className="border theme-border rounded px-2 py-1">{DESIGNATIONS.map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></td>
                                    <td className="p-2"><input type="checkbox" checked={r.coordinator} onChange={e => patch(r.userId, { coordinator: e.target.checked })}/></td>
                                    <td className="p-2"><input type="checkbox" checked={r.assistantCoordinator} onChange={e => patch(r.userId, { assistantCoordinator: e.target.checked })}/></td>
                                </tr>)}</tbody>
                            </table>
                        </div>
                        <button onClick={saveSettings} disabled={busy} className="mt-4 accent-gradient text-white rounded-lg px-5 py-2 text-sm font-semibold">Save supervisor settings</button>
                    </div>}
                </div>
            )}
        </div>
    )
}
