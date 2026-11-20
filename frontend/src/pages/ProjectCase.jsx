import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Trash2 } from 'lucide-react'
import {
    getCase, saveStage, addMeeting, deleteMeeting, saveAssessment, schedulePresentation, setOutcome, addFeedback,
    errorOf, STATUS_LABEL, STATUS_COLOR, OUTCOME_LABEL, OUTCOME_TONE, dateTime, day,
} from '../services/projectSuiteService'
import { Donut, Ring } from '../components/ProjectCharts'

const today = () => new Date().toISOString().slice(0, 10)
const field = 'border theme-border rounded-lg px-3 py-2 text-sm'

function StageRow({ s, busy, onSave }) {
    const [note, setNote] = useState(s.note || '')
    useEffect(() => setNote(s.note || ''), [s.note])
    return (
        <li className="px-4 py-3 flex items-center gap-3 flex-wrap">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: STATUS_COLOR[s.status] }} />
            <span className="w-60 text-sm font-medium">{s.label}</span>
            <select value={s.status} disabled={busy} onChange={(e) => onSave(s.key, e.target.value, note)} className={field}>
                {Object.entries(STATUS_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <input value={note} maxLength={300} onChange={(e) => setNote(e.target.value)} onBlur={() => note !== (s.note || '') && onSave(s.key, s.status, note)}
                   placeholder="Note for the student (optional)" className={`${field} flex-1 min-w-[180px]`} />
        </li>
    )
}

function Presentation({ p, canCoordinate, busy, run }) {
    const [feedback, setFeedback] = useState('')
    const [outcome, setOutcomeValue] = useState('')
    const [note, setNote] = useState(p.outcomeNote || '')

    return (
        <div className="card overflow-hidden">
            <div className="theme-sidebar text-white px-4 py-2 flex justify-between gap-3 flex-wrap text-sm">
                <span className="font-semibold">Presentation {p.round}{p.round > 1 ? ' (re-presentation)' : ''}</span>
                <span className="text-white/70">{p.scheduledAt ? dateTime(p.scheduledAt) : 'No date yet'}{p.venue ? ` · ${p.venue}` : ''}</span>
            </div>
            <div className="p-4 space-y-4">
                <div className="flex items-center gap-3 flex-wrap">
                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${OUTCOME_TONE[p.outcome]}`}>{OUTCOME_LABEL[p.outcome]}</span>
                    {p.outcomeNote && <span className="text-sm theme-text-muted">{p.outcomeNote}</span>}
                </div>

                <div>
                    <p className="text-sm font-semibold mb-2">What the assessors say the student must correct</p>
                    {p.feedback.length === 0 ? <p className="text-sm theme-text-muted">No feedback has been written yet.</p> : (
                        <ul className="space-y-2">{p.feedback.map((f, i) => (
                            <li key={i} className="text-sm px-3 py-2 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}><b>{f.assessor}:</b> {f.comments}</li>
                        ))}</ul>
                    )}
                    <div className="mt-3 flex gap-2 flex-wrap">
                        <textarea value={feedback} onChange={(e) => setFeedback(e.target.value)} rows={2} maxLength={1500} placeholder="Add your feedback: what must be corrected before the next presentation" className={`${field} flex-1 min-w-[240px]`} />
                        <button disabled={busy || !feedback.trim()} onClick={() => run(async () => { await addFeedback(p.id, { comments: feedback }); setFeedback('') }, 'Your feedback is saved.')}
                                className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50 self-start">Save feedback</button>
                    </div>
                </div>

                {canCoordinate && (
                    <div className="pt-3 border-t theme-border">
                        <p className="text-sm font-semibold mb-2">Record the outcome</p>
                        <div className="flex gap-2 flex-wrap items-center">
                            <select value={outcome} onChange={(e) => setOutcomeValue(e.target.value)} className={field}>
                                <option value="">Choose…</option>
                                <option value="PASSED">Passed</option>
                                <option value="CORRECTIONS_REQUIRED">Corrections required</option>
                                <option value="REPRESENT">Re-present</option>
                            </select>
                            <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} placeholder="Note (optional)" className={`${field} flex-1 min-w-[200px]`} />
                            <button disabled={busy || !outcome} onClick={() => run(() => setOutcome(p.id, { outcome, note }), 'Outcome recorded.')}
                                    className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">Save outcome</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}

function ProjectCase() {
    const { id } = useParams()
    const [data, setData] = useState(null)
    const [tab, setTab] = useState('progress')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [meeting, setMeeting] = useState({ date: today(), mode: 'IN_PERSON', attended: true, summary: '', nextSteps: '' })
    const [form, setForm] = useState(null)
    const [when, setWhen] = useState({ scheduledAt: '', venue: '' })

    async function load() {
        const d = await getCase(id)
        setData(d)
        setForm((f) => f || {
            problemStatement: d.assessment?.problemStatement || '', objectives: d.assessment?.objectives || '', publishable: d.assessment?.publishable || '',
            similarity: d.assessment?.similarity ?? '', ai: d.assessment?.ai ?? '', checkedOn: d.assessment?.checkedOn || '',
            diligence: d.assessment?.diligence || '', communication: d.assessment?.communication || '', initiative: d.assessment?.initiative || '', comment: d.assessment?.comment || '',
        })
    }

    useEffect(() => { load().catch((e) => setError(errorOf(e))) }, [id])

    async function run(action, success) {
        setBusy(true); setError(null); setNotice(null)
        try { await action(); await load(); if (success) setNotice(success) } catch (err) { setError(errorOf(err)) } finally { setBusy(false) }
    }

    if (!data) return error ? <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm max-w-xl">{error}</div> : <p className="theme-text-muted">Loading…</p>

    const s = data.summary
    const attended = data.meetings.filter((m) => m.attended).length
    const tabs = [['progress', 'Progress'], ['meetings', `Meetings (${data.meetings.length})`], ['assessment', 'Assessment'], ['presentations', 'Presentations']]
    const nums = (k) => (e) => setForm({ ...form, [k]: e.target.value })
    const latest = data.presentations[data.presentations.length - 1]
    const canSchedule = data.canCoordinate && (!latest || latest.outcome === 'PENDING' || latest.outcome === 'REPRESENT')

    return (
        <div className="max-w-4xl">
            <Link to="/project-cases" className="inline-flex items-center gap-1 text-sm theme-text-muted mb-3"><ArrowLeft size={14} /> All students</Link>
            <h2 className="text-2xl font-bold">{s.student}</h2>
            <p className="theme-text-muted text-sm mb-4">{s.matric} · {s.programme} · {s.classification} · Supervisor: {data.supervisor} · {data.session}</p>

            {s.alerts.length > 0 && (
                <div className={`mb-4 px-4 py-3 rounded-lg text-sm ${s.severity === 'HIGH' ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-700'}`}>
                    <ul className="space-y-1">{s.alerts.map((a, i) => <li key={i}>• {a}</li>)}</ul>
                </div>
            )}
            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="flex gap-1 mb-6 p-1 rounded-xl w-fit flex-wrap" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {tabs.map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)} className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>{label}</button>
                ))}
            </div>

            {tab === 'progress' && (
                <div className="space-y-4">
                    <div className="card p-4 flex items-center gap-6 flex-wrap">
                        <Ring value={s.stagesDone} total={s.stagesTotal} label="stages approved" />
                        <p className="text-sm theme-text-muted max-w-sm">Mark each stage as the student moves through it. The student sees these statuses and your notes.</p>
                    </div>
                    <div className="card overflow-hidden"><ul className="divide-y theme-border">
                        {data.stages.map((st) => <StageRow key={st.key} s={st} busy={busy} onSave={(key, status, note) => run(() => saveStage(id, { stage: key, status, note }), 'Saved.')} />)}
                    </ul></div>
                </div>
            )}

            {tab === 'meetings' && (
                <div className="space-y-4">
                    <div className="card p-4 flex gap-6 flex-wrap items-center">
                        <Donut size={110} thickness={18} sub="meetings" data={[
                            { label: 'Attended', value: attended, color: '#16a34a' },
                            { label: 'Missed', value: data.meetings.length - attended, color: '#ef4444' },
                        ]} />
                    </div>

                    <form onSubmit={(e) => { e.preventDefault(); run(() => addMeeting(id, meeting), meeting.attended ? 'Meeting recorded.' : 'Recorded as a missed meeting.').then(() => setMeeting({ ...meeting, summary: '', nextSteps: '' })) }} className="card p-4 space-y-3">
                        <p className="font-semibold">Record a meeting</p>
                        <div className="flex gap-3 flex-wrap items-center">
                            <input type="date" max={today()} value={meeting.date} onChange={(e) => setMeeting({ ...meeting, date: e.target.value })} required className={field} />
                            <select value={meeting.mode} onChange={(e) => setMeeting({ ...meeting, mode: e.target.value })} className={field}>
                                <option value="IN_PERSON">In person</option><option value="ONLINE">Online</option><option value="PHONE">Phone</option>
                            </select>
                            <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={meeting.attended} onChange={(e) => setMeeting({ ...meeting, attended: e.target.checked })} /> The student came</label>
                        </div>
                        <textarea value={meeting.summary} onChange={(e) => setMeeting({ ...meeting, summary: e.target.value })} rows={2} maxLength={600} placeholder="What was discussed" className={`${field} w-full`} />
                        <textarea value={meeting.nextSteps} onChange={(e) => setMeeting({ ...meeting, nextSteps: e.target.value })} rows={2} maxLength={400} placeholder="What the student must do next" className={`${field} w-full`} />
                        <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Save meeting</button>
                        {!meeting.attended && <p className="text-xs text-amber-700">Missed meetings count toward the alert to the H.O.D. and coordinators.</p>}
                    </form>

                    <div className="card overflow-hidden"><ul className="divide-y theme-border">
                        {data.meetings.length === 0 && <li className="p-6 text-sm theme-text-muted">No meeting has been recorded yet.</li>}
                        {data.meetings.map((m) => (
                            <li key={m.id} className="px-4 py-3 text-sm">
                                <div className="flex items-center gap-3 flex-wrap">
                                    <b>{day(m.date)}</b><span className="theme-text-muted">{m.mode.replace('_', ' ').toLowerCase()}</span>
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${m.attended ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{m.attended ? 'Attended' : 'Missed'}</span>
                                    <button onClick={() => window.confirm('Remove this meeting?') && run(() => deleteMeeting(m.id), 'Meeting removed.')} className="ml-auto text-red-500"><Trash2 size={14} /></button>
                                </div>
                                {m.summary && <p className="mt-1">{m.summary}</p>}
                                {m.nextSteps && <p className="mt-1 theme-text-muted">Next: {m.nextSteps}</p>}
                            </li>
                        ))}
                    </ul></div>
                </div>
            )}

            {tab === 'assessment' && form && (
                <form onSubmit={(e) => { e.preventDefault(); run(() => saveAssessment(id, {
                    ...form, similarity: form.similarity === '' ? null : Number(form.similarity), ai: form.ai === '' ? null : Number(form.ai),
                    diligence: form.diligence ? Number(form.diligence) : null, communication: form.communication ? Number(form.communication) : null, initiative: form.initiative ? Number(form.initiative) : null,
                }), 'Assessment saved.') }} className="card p-4 space-y-4">
                    <p className="text-xs theme-text-muted">Only you, the H.O.D. and the coordinators can see this. The student cannot.</p>
                    <div className="grid sm:grid-cols-3 gap-3">
                        {[['problemStatement', 'Convincing problem statement?'], ['objectives', 'Well-stated objectives?']].map(([k, label]) => (
                            <div key={k}><label className="block text-xs font-medium mb-1">{label}</label>
                                <select value={form[k]} onChange={nums(k)} className={`${field} w-full`}><option value="">Not assessed</option><option value="YES">Yes</option><option value="PARTLY">Partly</option><option value="NO">No</option></select></div>
                        ))}
                        <div><label className="block text-xs font-medium mb-1">Publishable?</label>
                            <select value={form.publishable} onChange={nums('publishable')} className={`${field} w-full`}><option value="">Not assessed</option><option value="YES">Yes</option><option value="WITH_REVISION">With revision</option><option value="NO">No</option></select></div>
                    </div>
                    <div className="grid sm:grid-cols-3 gap-3">
                        <div><label className="block text-xs font-medium mb-1">Turnitin similarity (%)</label><input type="number" min="0" max="100" step="0.01" value={form.similarity} onChange={nums('similarity')} className={`${field} w-full`} /></div>
                        <div><label className="block text-xs font-medium mb-1">AI-detection (%)</label><input type="number" min="0" max="100" step="0.01" value={form.ai} onChange={nums('ai')} className={`${field} w-full`} /></div>
                        <div><label className="block text-xs font-medium mb-1">Report checked on</label><input type="date" max={today()} value={form.checkedOn} onChange={nums('checkedOn')} className={`${field} w-full`} /></div>
                    </div>
                    <div className="grid sm:grid-cols-3 gap-3">
                        {[['diligence', 'Diligence'], ['communication', 'Communication'], ['initiative', 'Initiative']].map(([k, label]) => (
                            <div key={k}><label className="block text-xs font-medium mb-1">{label} (1 to 5)</label>
                                <select value={form[k]} onChange={nums(k)} className={`${field} w-full`}><option value="">Not rated</option>{[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}</select></div>
                        ))}
                    </div>
                    <textarea value={form.comment} onChange={nums('comment')} rows={3} maxLength={600} placeholder="Your comment on this project" className={`${field} w-full`} />
                    <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-medium btn-press disabled:opacity-50">Save assessment</button>
                </form>
            )}

            {tab === 'presentations' && (
                <div className="space-y-4">
                    {canSchedule && (
                        <form onSubmit={(e) => { e.preventDefault(); run(() => schedulePresentation(id, when), 'Presentation scheduled.') }} className="card p-4">
                            <p className="font-semibold mb-1">{latest?.outcome === 'REPRESENT' ? 'Schedule the re-presentation' : latest ? 'Change the date' : 'Schedule the presentation'}</p>
                            <div className="flex gap-3 flex-wrap items-center">
                                <input type="datetime-local" value={when.scheduledAt} onChange={(e) => setWhen({ ...when, scheduledAt: e.target.value })} required className={field} />
                                <input value={when.venue} onChange={(e) => setWhen({ ...when, venue: e.target.value })} maxLength={100} placeholder="Venue" className={field} />
                                <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Save date</button>
                            </div>
                        </form>
                    )}
                    {data.presentations.length === 0 && <div className="card p-6 text-sm theme-text-muted">No presentation has been scheduled yet.{data.canCoordinate ? '' : ' The H.O.D. or a Project Coordinator schedules it.'}</div>}
                    {data.presentations.map((p) => <Presentation key={p.id} p={p} canCoordinate={data.canCoordinate} busy={busy} run={run} />)}
                </div>
            )}
        </div>
    )
}

export default ProjectCase