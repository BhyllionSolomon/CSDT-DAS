import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getSurveyResults, setSurveyWindow, errorOf } from '../services/projectSuiteService'
import { Donut, HBars } from '../components/ProjectCharts'

function ProjectSurveyAdmin() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        getAllSessions().then((s) => {
            setSessions(s)
            const current = currentSessionOf(s)
            if (current) setSessionId(String(current.id))
        }).catch((e) => setError(errorOf(e)))
    }, [])

    async function load(id = sessionId) {
        if (!id) return
        setData(await getSurveyResults(id))
    }

    useEffect(() => { setData(null); setError(null); load().catch((e) => setError(errorOf(e))) }, [sessionId])

    async function toggle(open) {
        const question = open ? 'Open the integrity form to students?' : 'Close the form? Students can no longer submit.'
        if (!window.confirm(question)) return
        setBusy(true); setError(null); setNotice(null)
        try { setNotice((await setSurveyWindow(Number(sessionId), open)).message); await load() } catch (err) { setError(errorOf(err)) } finally { setBusy(false) }
    }

    const total = data ? data.supervisors.reduce((t, s) => t + s.responses, 0) : 0
    const money = data ? data.supervisors.reduce((t, s) => t + s.moneyReports, 0) : 0
    const conduct = data ? data.supervisors.reduce((t, s) => t + s.conductReports, 0) : 0
    const integrity = data ? data.supervisors.reduce((t, s) => t + s.integrityReports, 0) : 0
    const visible = data ? data.supervisors.filter((s) => s.visible) : []

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Integrity Form Results</h2>
            <p className="theme-text-muted text-sm mb-6">The anonymous form final-year students fill in about their supervision. Only you can see these results.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                {data && (
                    <div className="flex items-center gap-3 pb-1">
                        <span className={`text-xs px-3 py-1 rounded-full font-medium ${data.open ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600'}`}>{data.open ? 'Open to students' : 'Closed'}</span>
                        <button onClick={() => toggle(!data.open)} disabled={busy} className={`rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50 ${data.open ? 'border theme-border' : 'accent-gradient text-white'}`}>
                            {data.open ? 'Close the form' : 'Open the form'}
                        </button>
                    </div>
                )}
            </div>

            {!data && !error && <p className="theme-text-muted">Loading…</p>}

            {data && total === 0 && <div className="card p-6 text-sm theme-text-muted">No student has submitted the form for this session yet.</div>}

            {data && total > 0 && (
                <>
                    <div className="grid md:grid-cols-2 gap-4 mb-6">
                        <div className="card p-4">
                            <p className="font-semibold mb-3">Serious concerns reported ({data.concerns.length})</p>
                            <Donut sub="reports" data={[
                                { label: 'Money collected', value: money, color: '#ef4444' },
                                { label: 'Inappropriate conduct', value: conduct, color: '#f59e0b' },
                                { label: 'Report written by others or pressure', value: integrity, color: '#8b5cf6' },
                            ]} />
                        </div>
                        <div className="card p-4">
                            <p className="font-semibold mb-3">Overall supervisor rating (out of 5)</p>
                            {visible.length === 0 ? <p className="text-sm theme-text-muted">Ratings appear once at least {data.minimum} students have answered for a supervisor.</p>
                                : <HBars max={5} data={visible.map((s) => ({ label: s.name, value: s.overall }))} />}
                        </div>
                    </div>

                    <div className="card overflow-hidden mb-6">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">By supervisor</div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr><th className="px-4 py-2">Supervisor</th><th className="px-4 py-2">Responses</th><th className="px-4 py-2">Professional</th><th className="px-4 py-2">Helpful</th><th className="px-4 py-2">Available</th><th className="px-4 py-2">Money</th><th className="px-4 py-2">Conduct</th><th className="px-4 py-2">Integrity</th></tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {data.supervisors.map((s) => (
                                    <tr key={s.supervisorId}>
                                        <td className="px-4 py-2 font-medium">{s.name}</td><td className="px-4 py-2">{s.responses}</td>
                                        <td className="px-4 py-2">{s.visible ? s.professional : '—'}</td><td className="px-4 py-2">{s.visible ? s.helpful : '—'}</td><td className="px-4 py-2">{s.visible ? s.available : '—'}</td>
                                        <td className={`px-4 py-2 ${s.moneyReports ? 'text-red-600 font-semibold' : ''}`}>{s.moneyReports}</td>
                                        <td className={`px-4 py-2 ${s.conductReports ? 'text-red-600 font-semibold' : ''}`}>{s.conductReports}</td>
                                        <td className={`px-4 py-2 ${s.integrityReports ? 'text-red-600 font-semibold' : ''}`}>{s.integrityReports}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <p className="px-4 py-3 text-xs theme-text-muted">A dash means fewer than {data.minimum} students answered, so the average is hidden to protect them. Concerns are always shown.</p>
                    </div>

                    {data.concerns.length > 0 && (
                        <div className="card overflow-hidden mb-6" style={{ borderColor: '#fca5a5' }}>
                            <div className="bg-red-600 text-white px-4 py-2 text-sm font-semibold">Serious concerns (shuffled, separate from the other answers)</div>
                            <ul className="divide-y theme-border">
                                {data.concerns.map((c, i) => (
                                    <li key={i} className="px-4 py-3 text-sm"><b>{c.supervisor}</b> <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 ml-1">{c.type}</span><p className="mt-1">{c.details}</p></li>
                                ))}
                            </ul>
                        </div>
                    )}

                    {[['What the projects achieved', data.impact], ['Acknowledgments', data.acknowledgements], ['Other comments', data.comments]].map(([title, list]) => list.length > 0 && (
                        <div key={title} className="card p-4 mb-4">
                            <p className="font-semibold mb-2">{title}</p>
                            <ul className="space-y-2">{list.map((t, i) => <li key={i} className="text-sm px-3 py-2 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>{t}</li>)}</ul>
                        </div>
                    ))}
                </>
            )}
        </div>
    )
}

export default ProjectSurveyAdmin