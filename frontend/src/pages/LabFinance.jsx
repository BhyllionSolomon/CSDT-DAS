import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getFinance, getManuals, getImpact, naira } from '../services/labService'

const msg = (err) => err.response?.data?.message || err.message

function Card({ label, value, tone }) {
    return (
        <div className="card p-4">
            <p className={`text-xl font-bold ${tone || ''}`}>{value}</p>
            <p className="text-xs theme-text-muted">{label}</p>
        </div>
    )
}

function GroupCard({ title, g, bar }) {
    return (
        <div className="card overflow-hidden">
            <div className={`${bar} text-white px-4 py-2 text-sm font-semibold`}>{title}</div>
            <div className="p-4 grid grid-cols-3 gap-3 text-center">
                <div><p className="text-xl font-bold">{g.students}</p><p className="text-xs theme-text-muted">students</p></div>
                <div><p className="text-xl font-bold">{g.average != null ? g.average : '—'}</p><p className="text-xs theme-text-muted">average score</p></div>
                <div><p className="text-xl font-bold">{g.passRate != null ? `${g.passRate}%` : '—'}</p><p className="text-xs theme-text-muted">pass rate</p></div>
            </div>
        </div>
    )
}

function LabFinance() {
    const { user } = useAuth()
    const isHod = user.role === 'HOD' || user.role === 'ADMIN'

    const [data, setData] = useState(null)
    const [manuals, setManuals] = useState([])
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [manualId, setManualId] = useState('')
    const [impact, setImpact] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        getFinance().then(setData).catch((e) => setError(msg(e)))
        if (isHod) {
            getManuals().then((m) => setManuals(m.filter((x) => x.courseId))).catch(() => {})
            getAllSessions().then((s) => {
                setSessions(s)
                const current = currentSessionOf(s)
                if (current) setSessionId(String(current.id))
            })
        }
    }, [])

    async function runImpact() {
        setError(null); setImpact(null); setBusy(true)
        try { setImpact(await getImpact(sessionId, manualId)) } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const peak = data ? Math.max(1, ...data.months.map((m) => Math.max(Number(m.revenue), Number(m.spent)))) : 1

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Manual Finance</h2>
            <p className="theme-text-muted text-sm mb-6">What the lab manuals have cost, earned and left in stock.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {data && (
                <>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-6">
                        <Card label="Revenue from sales" value={naira(data.revenue)} />
                        <Card label="Spent on stock" value={naira(data.spent)} />
                        <Card label="Net cash (revenue − spent)" value={naira(data.net)} tone={Number(data.net) < 0 ? 'text-red-600' : 'text-green-600'} />
                        <Card label="Profit on copies sold" value={naira(data.grossProfit)} tone={Number(data.grossProfit) < 0 ? 'text-red-600' : ''} />
                        <Card label="Value of stock on hand" value={naira(data.stockValue)} />
                        <Card label="Copies sold / in stock" value={`${data.copiesSold} / ${data.copiesInStock}`} />
                    </div>

                    <div className="card p-4 mb-6">
                        <div className="flex justify-between items-center mb-4">
                            <p className="font-semibold">Money in and out, by month</p>
                            <div className="flex gap-4 text-xs theme-text-muted">
                                <span className="flex items-center gap-1"><span className="w-3 h-3 rounded accent-gradient inline-block" /> Revenue</span>
                                <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-slate-400 inline-block" /> Spent on stock</span>
                            </div>
                        </div>
                        {data.months.length === 0 ? (
                            <p className="text-sm theme-text-muted">Nothing has been recorded yet.</p>
                        ) : (
                            <div className="flex items-end gap-4 overflow-x-auto" style={{ height: 180 }}>
                                {data.months.map((m) => (
                                    <div key={m.month} className="flex flex-col items-center shrink-0" style={{ width: 56 }}>
                                        <div className="flex items-end gap-1 flex-1 w-full justify-center">
                                            <div title={naira(m.revenue)} className="w-5 rounded-t accent-gradient" style={{ height: `${(Number(m.revenue) / peak) * 100}%`, minHeight: Number(m.revenue) > 0 ? 3 : 0 }} />
                                            <div title={naira(m.spent)} className="w-5 rounded-t bg-slate-400" style={{ height: `${(Number(m.spent) / peak) * 100}%`, minHeight: Number(m.spent) > 0 ? 3 : 0 }} />
                                        </div>
                                        <span className="text-[10px] theme-text-muted mt-1">{m.month}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="card overflow-hidden mb-8">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">By manual</div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr>
                                    <th className="px-4 py-2">Manual</th><th className="px-4 py-2">Added</th><th className="px-4 py-2">Sold</th><th className="px-4 py-2">Stock</th>
                                    <th className="px-4 py-2">Revenue</th><th className="px-4 py-2">Spent</th><th className="px-4 py-2">Profit</th><th className="px-4 py-2">Stock value</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {data.manuals.map((m) => (
                                    <tr key={m.manualId}>
                                        <td className="px-4 py-2 font-medium">{m.title}</td>
                                        <td className="px-4 py-2">{m.purchased}</td>
                                        <td className="px-4 py-2">{m.sold}</td>
                                        <td className="px-4 py-2">{m.stock}</td>
                                        <td className="px-4 py-2">{naira(m.revenue)}</td>
                                        <td className="px-4 py-2">{naira(m.spent)}</td>
                                        <td className={`px-4 py-2 ${Number(m.grossProfit) < 0 ? 'text-red-600' : ''}`}>{naira(m.grossProfit)}</td>
                                        <td className="px-4 py-2">{naira(m.stockValue)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                        <p className="px-4 py-3 text-xs theme-text-muted">
                            Profit is the sales revenue minus the average cost of the copies sold. Net cash also counts copies still in stock as money already spent.
                        </p>
                    </div>
                </>
            )}

            {isHod && (
                <div className="card p-4">
                    <p className="font-semibold mb-1">Do the manuals help scores?</p>
                    <p className="text-xs theme-text-muted mb-4">
                        Compares students who bought a manual with those who did not, using the approved scores in that manual's course.
                        Only the H.O.D sees this, and it needs at least 5 students in each group.
                    </p>
                    <div className="flex gap-3 flex-wrap items-end mb-4">
                        <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                        </select>
                        <select value={manualId} onChange={(e) => setManualId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Manual…</option>
                            {manuals.map((m) => <option key={m.id} value={m.id}>{m.title} ({m.courseCode})</option>)}
                        </select>
                        <button onClick={runImpact} disabled={busy || !manualId || !sessionId}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {busy ? 'Working…' : 'Compare'}
                        </button>
                    </div>

                    {manuals.length === 0 && <p className="text-xs text-amber-700">No manual is linked to a course yet. Link one on the Manual Sales &amp; Stock page.</p>}

                    {impact && (
                        <div className="space-y-4">
                            <p className="text-sm font-medium">{impact.manualTitle} · {impact.courseCode} · {impact.sessionName}</p>
                            <div className="grid md:grid-cols-2 gap-4">
                                <GroupCard title="Bought the manual" g={impact.buyers} bar="accent-gradient" />
                                <GroupCard title="Did not buy" g={impact.nonBuyers} bar="bg-slate-500" />
                            </div>
                            {impact.enoughData && (
                                <p className="text-sm">
                                    Average score difference: <span className="font-semibold">{impact.averageDifference > 0 ? '+' : ''}{impact.averageDifference}</span> marks for those who bought.
                                </p>
                            )}
                            <p className="text-xs theme-text-muted">{impact.note}</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default LabFinance