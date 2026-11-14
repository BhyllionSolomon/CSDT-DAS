import { useEffect, useMemo, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { getSecurityLog } from '../services/securityLogService'

const msg = (err) => err.response?.data?.message || err.message
const DAY = 24 * 60 * 60 * 1000

const tone = (action) => {
    const a = String(action || '').toUpperCase()
    if (a.includes('DENIED') || a.includes('FAIL') || a.includes('LOCK') || a.includes('BLOCK')) return 'bg-red-100 text-red-700'
    if (a.includes('RESET') || a.includes('CHANGE')) return 'bg-amber-100 text-amber-700'
    return 'bg-slate-200 text-slate-600'
}

function SecurityLog() {
    const [events, setEvents] = useState(null)
    const [action, setAction] = useState('ALL')
    const [query, setQuery] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    async function load() {
        setBusy(true); setError(null)
        try { setEvents(await getSecurityLog()) } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    useEffect(() => { load() }, [])

    const actions = useMemo(() => (events ? [...new Set(events.map((e) => e.action))].sort() : []), [events])

    const stats = useMemo(() => {
        if (!events) return null
        const recent = events.filter((e) => Date.now() - new Date(e.at).getTime() < DAY)
        return {
            total: recent.length,
            denied: recent.filter((e) => String(e.action).toUpperCase().includes('DENIED')).length,
            failed: recent.filter((e) => String(e.action).toUpperCase().includes('FAIL')).length,
        }
    }, [events])

    const shown = (events || []).filter((e) =>
        (action === 'ALL' || e.action === action) &&
        (!query.trim() || `${e.username} ${e.ip} ${e.detail} ${e.role}`.toLowerCase().includes(query.trim().toLowerCase())))

    return (
        <div className="max-w-5xl">
            <div className="flex justify-between items-start gap-3 flex-wrap mb-2">
                <h2 className="text-2xl font-bold">Security Log</h2>
                <button onClick={load} disabled={busy} className="flex items-center gap-1.5 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    <RefreshCw size={14} className={busy ? 'animate-spin' : ''} /> Refresh
                </button>
            </div>
            <p className="theme-text-muted text-sm mb-6">
                A record of blocked actions and account events, so you can see who tried to do what. The most recent 500 events are shown.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {stats && (
                <div className="grid grid-cols-3 gap-3 mb-6">
                    <div className="card p-4"><p className="text-2xl font-bold">{stats.total}</p><p className="text-xs theme-text-muted">events in the last 24 hours</p></div>
                    <div className="card p-4"><p className={`text-2xl font-bold ${stats.denied > 0 ? 'text-red-600' : ''}`}>{stats.denied}</p><p className="text-xs theme-text-muted">blocked actions</p></div>
                    <div className="card p-4"><p className={`text-2xl font-bold ${stats.failed > 0 ? 'text-amber-600' : ''}`}>{stats.failed}</p><p className="text-xs theme-text-muted">failed sign-ins</p></div>
                </div>
            )}

            {events && (
                <>
                    <div className="card p-4 mb-4 flex gap-3 flex-wrap">
                        <select value={action} onChange={(e) => setAction(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="ALL">All event types</option>
                            {actions.map((a) => <option key={a} value={a}>{a}</option>)}
                        </select>
                        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search person, address or details"
                               className="border theme-border rounded-lg px-3 py-2 text-sm w-72" />
                    </div>

                    {shown.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">{events.length === 0 ? 'Nothing has been recorded yet.' : 'No event matches your search.'}</div>
                    ) : (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{shown.length} event{shown.length === 1 ? '' : 's'}</div>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                    <tr>
                                        <th className="px-4 py-2">When</th><th className="px-4 py-2">Who</th><th className="px-4 py-2">Address</th>
                                        <th className="px-4 py-2">Event</th><th className="px-4 py-2">Details</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y theme-border">
                                    {shown.map((e, i) => (
                                        <tr key={i}>
                                            <td className="px-4 py-2 whitespace-nowrap">{new Date(e.at).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'medium' })}</td>
                                            <td className="px-4 py-2">{e.username || '—'} <span className="text-xs theme-text-muted">{e.role}</span></td>
                                            <td className="px-4 py-2 font-mono text-xs">{e.ip || '—'}</td>
                                            <td className="px-4 py-2"><span className={`text-xs px-2 py-0.5 rounded-full font-medium ${tone(e.action)}`}>{e.action}</span></td>
                                            <td className="px-4 py-2 text-xs theme-text-muted break-words">{e.detail}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default SecurityLog