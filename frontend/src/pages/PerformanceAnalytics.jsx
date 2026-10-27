import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import { getAllSessions } from '../services/sessionService'
import { getPerformance } from '../services/analyticsService'
import { BarChart, LineChart, HBars, Legend, StatCard, GRADE_COLORS } from '../components/Charts'

const LEVEL_CODES = ['100', '200', '300', '400']
const msg = (err) => err.response?.data?.message || err.message

function Card({ title, children }) {
    return (
        <div className="card p-4">
            <p className="font-semibold mb-3">{title}</p>
            {children}
        </div>
    )
}

function PerformanceAnalytics() {
    const { user } = useAuth()
    const isAdviser = user.role === 'LEVEL_ADVISER'

    const [sessions, setSessions] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [programmeId, setProgrammeId] = useState(isAdviser && user.programmeId ? String(user.programmeId) : '')
    const [levelId, setLevelId] = useState('')
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllSessions(), api.get('/programmes'), api.get('/levels')])
            .then(([s, p, l]) => {
                setSessions(s)
                setProgrammes(p.data)
                setLevels(l.data.filter((x) => LEVEL_CODES.includes(x.code)))
                const latest = [...s].sort((a, b) => String(b.startDate).localeCompare(String(a.startDate)))[0]
                if (latest) setSessionId(String(latest.id))
            })
            .catch((err) => setError(msg(err)))
            .finally(() => setLoading(false))
    }, [])

    async function show() {
        setError(null); setBusy(true)
        try {
            setData(await getPerformance(sessionId, programmeId, levelId))
        } catch (err) {
            setError(msg(err))
        } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const visibleProgrammes = isAdviser ? programmes.filter((p) => String(p.id) === programmeId) : programmes
    const s = data?.summary

    const courseItems = data
        ? [...data.courses]
            .sort((a, b) => a.passRate - b.passRate)
            .map((c) => ({
                label: `${c.code} — ${c.title}`,
                value: c.passRate,
                sub: `Average ${c.average} · ${c.students} student(s)`,
                color: c.passRate < 60 ? '#dc2626' : undefined,
            }))
        : []

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Performance Analytics</h2>
            <p className="theme-text-muted text-sm mb-6">
                How students are doing, by programme, level and session. Only approved results are counted, and "level" is the level of the course.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Programme</label>
                    <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)} disabled={isAdviser}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {!isAdviser && <option value="">All programmes</option>}
                        {visibleProgrammes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Level</label>
                    <select value={levelId} onChange={(e) => setLevelId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">All levels</option>
                        {levels.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                    </select>
                </div>
                <button onClick={show} disabled={!sessionId || busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Loading…' : 'Show analytics'}
                </button>
            </div>

            {data && s.results === 0 && (
                <div className="card p-6 text-sm theme-text-muted">
                    There are no approved results for {data.sessionName}, {data.programmeName}, {data.levelName} yet.
                    Approve results on the Result Verification page and they will appear here.
                </div>
            )}

            {data && s.results > 0 && (
                <div className="space-y-6">
                    <p className="text-sm theme-text-muted">
                        {data.programmeName} · {data.levelName} · {data.sessionName}
                    </p>

                    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                        <StatCard label="Students" value={s.students} />
                        <StatCard label="Results" value={s.results} />
                        <StatCard label="Average score" value={s.averageScore} />
                        <StatCard label="Pass rate" value={`${s.passRate}%`} hint="Score of 40 and above" />
                        <StatCard label="Average GPA" value={s.averageGpa} hint="Out of 5.00" />
                    </div>

                    <div className="grid md:grid-cols-2 gap-6">
                        <Card title="Grade distribution">
                            <BarChart data={data.gradeDistribution.map((g) => ({ label: g.label, value: g.count, color: GRADE_COLORS[g.label] }))} />
                        </Card>
                        <Card title="Students by GPA band">
                            <HBars max={s.students} items={data.gpaBands.map((b) => ({
                                label: b.label, value: b.count, color: b.label.startsWith('Below') ? '#dc2626' : undefined,
                            }))} />
                        </Card>
                    </div>

                    <Card title="Average score and pass rate across sessions">
                        <Legend items={[{ name: 'Average score', color: 'var(--accent)' }, { name: 'Pass rate (%)', color: '#16a34a' }]} />
                        <LineChart
                            labels={data.trend.map((t) => t.session)}
                            series={[
                                { name: 'Average score', color: 'var(--accent)', values: data.trend.map((t) => t.average) },
                                { name: 'Pass rate', color: '#16a34a', values: data.trend.map((t) => t.passRate) },
                            ]}
                        />
                    </Card>

                    <Card title="Pass rate by course (courses needing attention first)">
                        <HBars items={courseItems} unit="%" />
                    </Card>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Course detail</div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr>
                                    <th className="px-4 py-2">Course</th>
                                    <th className="px-4 py-2">Students</th>
                                    <th className="px-4 py-2">Average</th>
                                    <th className="px-4 py-2">Pass rate</th>
                                    {['A', 'B', 'C', 'D', 'E', 'F'].map((g) => <th key={g} className="px-3 py-2">{g}</th>)}
                                </tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {data.courses.map((c) => (
                                    <tr key={c.courseId}>
                                        <td className="px-4 py-2 font-medium">{c.code} <span className="font-normal theme-text-muted">{c.title}</span></td>
                                        <td className="px-4 py-2">{c.students}</td>
                                        <td className="px-4 py-2">{c.average}</td>
                                        <td className="px-4 py-2">{c.passRate}%</td>
                                        {['A', 'B', 'C', 'D', 'E', 'F'].map((g) => <td key={g} className="px-3 py-2">{c.grades[g]}</td>)}
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default PerformanceAnalytics