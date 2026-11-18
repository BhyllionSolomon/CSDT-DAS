import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCases, errorOf, OUTCOME_LABEL, OUTCOME_TONE } from '../services/projectSuiteService'
import { Donut } from '../components/ProjectCharts'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'

const SEVERITY = {
    HIGH: ['Needs action', 'bg-red-100 text-red-700'],
    MEDIUM: ['Watch', 'bg-amber-100 text-amber-700'],
    NONE: ['On track', 'bg-green-100 text-green-700'],
}

function ProjectCases() {
    const [all, setAll] = useState(true)
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [cases, setCases] = useState(null)
    const [query, setQuery] = useState('')
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions().then(rows => { setSessions(rows || []); const current = currentSessionOf(rows || []); if (current) setSessionId(String(current.id)) }).catch(e => setError(errorOf(e)))
    }, [])

    useEffect(() => {
        if (!sessionId) return
        setCases(null); setError(null)
        getCases(Number(sessionId), all).then(setCases).catch((e) => setError(errorOf(e)))
    }, [all, sessionId])

    const counts = useMemo(() => {
        const list = cases || []
        return { high: list.filter((c) => c.severity === 'HIGH').length, medium: list.filter((c) => c.severity === 'MEDIUM').length, none: list.filter((c) => c.severity === 'NONE').length }
    }, [cases])

    const shown = (cases || []).filter((c) => !query.trim() || `${c.student} ${c.matric}`.toLowerCase().includes(query.trim().toLowerCase()))

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">All Project Cases</h2>
            <p className="theme-text-muted text-sm mb-6">
                The final-year students you supervise: their progress, your meetings with them, and anyone who needs attention.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-4 flex-wrap items-center">
                <label className="text-sm">Academic session<select value={sessionId} onChange={e => setSessionId(e.target.value)} className="block border theme-border rounded-lg px-3 py-2 mt-1">{sessions.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
                <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search name or matric number" className="border theme-border rounded-lg px-3 py-2 text-sm w-64" />
                <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={all} onChange={(e) => setAll(e.target.checked)} /> Show every student (H.O.D. and coordinators)</label>
            </div>

            {!cases && !error && <p className="theme-text-muted">Loading…</p>}

            {cases && cases.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">No student has been assigned to you for this session yet. The H.O.D. assigns supervisors once, at the start of the project year.</div>
            )}

            {cases && cases.length > 0 && (
                <>
                    <div className="card p-4 mb-6">
                        <p className="font-semibold mb-3">Where your students stand</p>
                        <Donut sub="students" data={[
                            { label: 'Needs action', value: counts.high, color: '#ef4444' },
                            { label: 'Watch', value: counts.medium, color: '#f59e0b' },
                            { label: 'On track', value: counts.none, color: '#16a34a' },
                        ]} />
                    </div>

                    <div className="grid md:grid-cols-2 gap-4">
                        {shown.map((c) => (
                            <Link key={c.id} to={`/project-cases/${c.id}`} className="card card-hover p-4 block">
                                <div className="flex justify-between gap-3 items-start">
                                    <div className="min-w-0">
                                        <p className="font-semibold truncate">{c.student}</p>
                                        <p className="text-xs theme-text-muted">{c.matric} · {c.programme} · {c.classification}</p>
                                    </div>
                                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${SEVERITY[c.severity][1]}`}>{SEVERITY[c.severity][0]}</span>
                                </div>

                                <div className="mt-3">
                                    <div className="flex justify-between text-xs theme-text-muted mb-1"><span>{c.stagesDone} of {c.stagesTotal} stages approved</span></div>
                                    <div className="h-2 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                        <div className="h-full accent-gradient" style={{ width: `${(c.stagesDone / c.stagesTotal) * 100}%` }} />
                                    </div>
                                </div>

                                <div className="flex gap-2 flex-wrap mt-3 text-xs">
                                    <span className="px-2 py-0.5 rounded-full bg-slate-200 text-slate-600">{c.meetings} meeting(s)</span>
                                    {c.missed > 0 && <span className="px-2 py-0.5 rounded-full bg-red-100 text-red-700">{c.missed} missed</span>}
                                    <span className={`px-2 py-0.5 rounded-full ${OUTCOME_TONE[c.latestOutcome]}`}>{OUTCOME_LABEL[c.latestOutcome]}</span>
                                </div>
                                {c.alerts.length > 0 && <p className="text-xs text-amber-700 mt-2">{c.alerts[0]}</p>}
                            </Link>
                        ))}
                    </div>
                </>
            )}
        </div>
    )
}

export default ProjectCases