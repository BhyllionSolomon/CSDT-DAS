import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAlerts, getCases, schedulePresentation, errorOf, OUTCOME_LABEL, OUTCOME_TONE } from '../services/projectSuiteService'
import { Donut, HBars } from '../components/ProjectCharts'

const field = 'border theme-border rounded-lg px-3 py-2 text-sm'

function ProjectAlerts() {
    const [tab, setTab] = useState('alerts')
    const [alerts, setAlerts] = useState(null)
    const [cases, setCases] = useState(null)
    const [open, setOpen] = useState(null)
    const [when, setWhen] = useState({ scheduledAt: '', venue: '' })
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function load() {
        const [a, c] = await Promise.all([getAlerts(), getCases(null, true)])
        setAlerts(a); setCases(c)
    }

    useEffect(() => { load().catch((e) => setError(errorOf(e))) }, [])

    const chart = useMemo(() => {
        const list = cases || []
        const outcomes = ['NONE', 'PENDING', 'PASSED', 'CORRECTIONS_REQUIRED', 'REPRESENT']
        return {
            severity: [
                { label: 'Needs action', value: list.filter((c) => c.severity === 'HIGH').length, color: '#ef4444' },
                { label: 'Watch', value: list.filter((c) => c.severity === 'MEDIUM').length, color: '#f59e0b' },
                { label: 'On track', value: list.filter((c) => c.severity === 'NONE').length, color: '#16a34a' },
            ],
            outcomes: outcomes.map((o) => ({ label: OUTCOME_LABEL[o], value: list.filter((c) => c.latestOutcome === o).length })),
        }
    }, [cases])

    async function schedule(id) {
        setBusy(true); setError(null); setNotice(null)
        try {
            const result = await schedulePresentation(id, when)
            setNotice(result.message); setOpen(null); setWhen({ scheduledAt: '', venue: '' })
            await load()
        } catch (err) { setError(errorOf(err)) } finally { setBusy(false) }
    }

    if (error && !alerts) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm max-w-xl">{error}</div>
    if (!alerts || !cases) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Project Alerts</h2>
            <p className="theme-text-muted text-sm mb-6">For the H.O.D. and the Project Coordinators: students who are not showing up or not progressing, and the presentation timetable.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="grid md:grid-cols-2 gap-4 mb-6">
                <div className="card p-4"><p className="font-semibold mb-3">All project students</p><Donut sub="students" data={chart.severity} /></div>
                <div className="card p-4"><p className="font-semibold mb-3">Presentations</p><HBars data={chart.outcomes} max={Math.max(1, cases.length)} /></div>
            </div>

            <div className="flex gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {[['alerts', `Needs attention (${alerts.length})`], ['presentations', 'Presentations']].map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)} className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>{label}</button>
                ))}
            </div>

            {tab === 'alerts' && (alerts.length === 0 ? (
                <div className="card p-6 text-sm text-green-700">Nobody needs attention right now.</div>
            ) : (
                <div className="space-y-3">
                    {alerts.map((c) => (
                        <div key={c.id} className="card p-4" style={c.severity === 'HIGH' ? { borderColor: '#fca5a5' } : undefined}>
                            <div className="flex justify-between gap-3 flex-wrap items-start">
                                <div>
                                    <p className="font-semibold">{c.student} <span className="font-normal theme-text-muted">· {c.matric} · {c.programme}</span></p>
                                    <ul className="mt-2 space-y-1 text-sm">{c.alerts.map((a, i) => <li key={i} className={c.severity === 'HIGH' ? 'text-red-700' : 'text-amber-700'}>• {a}</li>)}</ul>
                                </div>
                                <Link to={`/project-cases/${c.id}`} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>Open the case</Link>
                            </div>
                        </div>
                    ))}
                </div>
            ))}

            {tab === 'presentations' && (
                <div className="card overflow-hidden">
                    <ul className="divide-y theme-border">
                        {cases.map((c) => (
                            <li key={c.id} className="px-4 py-3">
                                <div className="flex items-center gap-3 flex-wrap text-sm">
                                    <span className="font-medium flex-1 min-w-[180px]">{c.student} <span className="font-normal theme-text-muted">· {c.matric}</span></span>
                                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${OUTCOME_TONE[c.latestOutcome]}`}>{OUTCOME_LABEL[c.latestOutcome]}</span>
                                    {(c.latestOutcome === 'NONE' || c.latestOutcome === 'PENDING' || c.latestOutcome === 'REPRESENT') && (
                                        <button onClick={() => { setOpen(open === c.id ? null : c.id); setWhen({ scheduledAt: '', venue: '' }) }} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>
                                            {c.latestOutcome === 'REPRESENT' ? 'Schedule re-presentation' : c.latestOutcome === 'PENDING' ? 'Change date' : 'Schedule'}
                                        </button>
                                    )}
                                    <Link to={`/project-cases/${c.id}`} className="text-xs theme-text-muted underline">Outcome and feedback</Link>
                                </div>
                                {open === c.id && (
                                    <div className="mt-3 flex gap-3 flex-wrap items-center">
                                        <input type="datetime-local" value={when.scheduledAt} onChange={(e) => setWhen({ ...when, scheduledAt: e.target.value })} className={field} />
                                        <input value={when.venue} onChange={(e) => setWhen({ ...when, venue: e.target.value })} placeholder="Venue" maxLength={100} className={field} />
                                        <button onClick={() => schedule(c.id)} disabled={busy || !when.scheduledAt} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Save</button>
                                    </div>
                                )}
                            </li>
                        ))}
                        {cases.length === 0 && <li className="p-6 text-sm theme-text-muted">No student has been assigned a supervisor yet.</li>}
                    </ul>
                </div>
            )}
        </div>
    )
}

export default ProjectAlerts