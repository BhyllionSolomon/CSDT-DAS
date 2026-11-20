import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { getMyProject, errorOf, STATUS_LABEL, STATUS_COLOR, OUTCOME_LABEL, OUTCOME_TONE, dateTime, day } from '../services/projectSuiteService'
import { Donut, Columns, Ring } from '../components/ProjectCharts'

function MyProject() {
    const [data, setData] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => { getMyProject().then(setData).catch((e) => setError(errorOf(e))) }, [])

    const charts = useMemo(() => {
        if (!data) return null
        const count = (st) => data.stages.filter((s) => s.status === st).length
        const attended = data.meetings.filter((m) => m.attended).length
        return {
            done: count('APPROVED'),
            columns: Object.keys(STATUS_LABEL).map((k) => ({ label: STATUS_LABEL[k], value: count(k), color: STATUS_COLOR[k] })),
            meetings: [{ label: 'Attended', value: attended, color: '#16a34a' }, { label: 'Missed', value: data.meetings.length - attended, color: '#ef4444' }],
        }
    }, [data])

    if (error) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm max-w-xl">{error}</div>
    if (!data) return <p className="theme-text-muted">Loading…</p>

    if (!data.assigned) {
        return (
            <div className="max-w-xl">
                <h2 className="text-2xl font-bold mb-2">My Project</h2>
                <div className="card p-6 text-sm theme-text-muted">You have not been assigned a project supervisor yet. The H.O.D. assigns supervisors to final-year students once, at the start of the project year.</div>
            </div>
        )
    }

    const next = data.presentations.find((p) => p.outcome === 'PENDING' && p.scheduledAt)
    const represent = data.presentations.length > 0 && data.presentations[data.presentations.length - 1].outcome === 'REPRESENT'

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-1">My Project</h2>
            <p className="theme-text-muted text-sm mb-6">Supervisor: <b>{data.supervisor}</b> · {data.session}</p>

            {next && <div className="mb-4 px-4 py-3 rounded-lg bg-blue-50 text-blue-800 text-sm">Your next presentation: <b>{dateTime(next.scheduledAt)}</b>{next.venue ? ` at ${next.venue}` : ''}.</div>}
            {represent && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">You have been asked to re-present. Make the corrections below. Your new date will appear here as soon as it is set.</div>}

            <div className="grid md:grid-cols-3 gap-4 mb-6">
                <div className="card p-4 flex flex-col items-center"><p className="font-semibold mb-2 self-start">Progress</p><Ring value={charts.done} total={data.stages.length} label="stages approved" /></div>
                <div className="card p-4"><p className="font-semibold mb-2">Stages by status</p><Columns height={90} data={charts.columns} /></div>
                <div className="card p-4"><p className="font-semibold mb-2">Meetings</p>{data.meetings.length === 0 ? <p className="text-sm theme-text-muted">No meeting yet.</p> : <Donut size={100} thickness={16} sub="meetings" data={charts.meetings} />}</div>
            </div>

            <div className="card overflow-hidden mb-6">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Where your project stands</div>
                <ul className="divide-y theme-border">
                    {data.stages.map((s) => (
                        <li key={s.key} className="px-4 py-2.5 flex items-center gap-3 text-sm flex-wrap">
                            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: STATUS_COLOR[s.status] }} />
                            <span className="flex-1 min-w-[160px]">{s.label}</span>
                            <span className="text-xs font-medium" style={{ color: STATUS_COLOR[s.status] }}>{STATUS_LABEL[s.status]}</span>
                            {s.note && <span className="w-full text-xs theme-text-muted pl-5">{s.note}</span>}
                        </li>
                    ))}
                </ul>
            </div>

            <div className="card overflow-hidden mb-6">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Meetings with your supervisor</div>
                <ul className="divide-y theme-border">
                    {data.meetings.length === 0 && <li className="p-6 text-sm theme-text-muted">No meeting has been recorded yet. Arrange one with your supervisor.</li>}
                    {data.meetings.map((m) => (
                        <li key={m.id} className="px-4 py-3 text-sm">
                            <div className="flex items-center gap-3 flex-wrap">
                                <b>{day(m.date)}</b><span className="theme-text-muted">{m.mode.replace('_', ' ').toLowerCase()}</span>
                                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${m.attended ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{m.attended ? 'Attended' : 'Missed'}</span>
                            </div>
                            {m.summary && <p className="mt-1">{m.summary}</p>}
                            {m.nextSteps && <p className="mt-1 text-amber-700">To do: {m.nextSteps}</p>}
                        </li>
                    ))}
                </ul>
            </div>

            {data.presentations.length > 0 && (
                <div className="space-y-4 mb-6">
                    {data.presentations.map((p) => (
                        <div key={p.id} className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 flex justify-between gap-3 flex-wrap text-sm">
                                <span className="font-semibold">Presentation {p.round}{p.round > 1 ? ' (re-presentation)' : ''}</span>
                                <span className="text-white/70">{p.scheduledAt ? dateTime(p.scheduledAt) : 'No date yet'}{p.venue ? ` · ${p.venue}` : ''}</span>
                            </div>
                            <div className="p-4">
                                <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${OUTCOME_TONE[p.outcome]}`}>{OUTCOME_LABEL[p.outcome]}</span>
                                {p.outcomeNote && <p className="text-sm mt-2">{p.outcomeNote}</p>}
                                <p className="text-sm font-semibold mt-4 mb-2">What you must correct</p>
                                {p.feedback.length === 0 ? <p className="text-sm theme-text-muted">No feedback has been written yet.</p> : (
                                    <ul className="space-y-2">{p.feedback.map((f, i) => <li key={i} className="text-sm px-3 py-2 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}><b>{f.assessor}:</b> {f.comments}</li>)}</ul>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <div className="card p-4 flex items-start gap-3">
                <ShieldCheck size={20} className="shrink-0 mt-0.5" style={{ color: 'var(--accent)' }} />
                <div className="text-sm">
                    <p className="font-semibold">Share your experience, anonymously</p>
                    <p className="theme-text-muted mt-0.5">Tell the department about your supervision. It protects every student. <Link to="/project-survey" className="underline font-medium" style={{ color: 'var(--accent)' }}>Open the form</Link></p>
                </div>
            </div>
        </div>
    )
}

export default MyProject