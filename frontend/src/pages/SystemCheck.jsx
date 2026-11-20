import { useEffect, useState } from 'react'
import { AlertTriangle, CheckCircle2, RefreshCw, XCircle } from 'lucide-react'
import { getSystemCheck } from '../services/deployService'

const msg = (err) => err.response?.data?.message || err.message

const TONE = {
    fail: { icon: XCircle, color: '#dc2626', bg: 'rgba(220,38,38,.12)' },
    warn: { icon: AlertTriangle, color: '#d97706', bg: 'rgba(245,158,11,.14)' },
    ok: { icon: CheckCircle2, color: '#16a34a', bg: 'rgba(22,163,74,.12)' },
}

function SystemCheck() {
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    async function load() {
        setBusy(true); setError(null)
        try { setData(await getSystemCheck()) } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    useEffect(() => { load() }, [])

    return (
        <div className="max-w-4xl">
            <div className="flex justify-between items-start gap-3 flex-wrap mb-2">
                <h2 className="text-2xl font-bold">System Check</h2>
                <button onClick={load} disabled={busy} className="flex items-center gap-1.5 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    <RefreshCw size={14} className={busy ? 'animate-spin' : ''} /> Check again
                </button>
            </div>
            <p className="theme-text-muted text-sm mb-6">
                A quick look at your live setup before the department relies on it. It only reads. It never shows a password or a key, only whether it is present.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {!data && !error && <p className="theme-text-muted">Checking…</p>}

            {data && (
                <>
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="card p-4"><p className="text-2xl font-bold text-red-600">{data.fail}</p><p className="text-xs theme-text-muted">to fix now</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold text-amber-600">{data.warn}</p><p className="text-xs theme-text-muted">to look at</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold text-green-600">{data.ok}</p><p className="text-xs theme-text-muted">fine</p></div>
                    </div>

                    <div className="space-y-2 mb-8">
                        {data.checks.map((c) => {
                            const t = TONE[c.status]
                            const Icon = t.icon
                            return (
                                <div key={c.key} className="card p-3.5 flex items-start gap-3">
                                    <span className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style={{ background: t.bg, color: t.color }}><Icon size={18} /></span>
                                    <span className="min-w-0">
                    <span className="block text-sm font-semibold">{c.title}</span>
                    <span className="block text-xs theme-text-muted mt-0.5">{c.detail}</span>
                  </span>
                                </div>
                            )
                        })}
                    </div>

                    <div className="grid md:grid-cols-2 gap-4">
                        <div className="card p-4">
                            <p className="font-semibold mb-2">Totals</p>
                            <ul className="text-sm space-y-1">{data.totals.map((t) => <li key={t.label} className="flex justify-between"><span>{t.label}</span><b>{t.value}</b></li>)}</ul>
                        </div>
                        <div className="card p-4">
                            <p className="font-semibold mb-2">Students recorded under each session</p>
                            {data.sessions.length === 0 ? <p className="text-sm theme-text-muted">No student has a session yet.</p> : (
                                <ul className="text-sm space-y-1">{data.sessions.map((s) => <li key={s.label} className="flex justify-between"><span>{s.label}</span><b>{s.value}</b></li>)}</ul>
                            )}
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}

export default SystemCheck