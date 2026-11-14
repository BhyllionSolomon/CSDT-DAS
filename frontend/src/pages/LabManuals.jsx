import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import {
    getManuals, saveManual, addPurchase, searchLabStudents, sellManual, getSales, voidSale, listLabCourses, naira,
} from '../services/labService'

const msg = (err) => err.response?.data?.message || err.message
const today = () => new Date().toISOString().slice(0, 10)
const BLANK_MANUAL = { id: null, title: '', courseId: '', unitPrice: '', active: true }

function LabManuals() {
    const { user } = useAuth()
    const canFinance = ['HOD', 'ADMIN', 'LAB_OFFICER'].includes(user.role)

    const [tab, setTab] = useState('sell')
    const [manuals, setManuals] = useState([])
    const [sales, setSales] = useState([])
    const [courses, setCourses] = useState([])
    const [query, setQuery] = useState('')
    const [hits, setHits] = useState([])
    const [student, setStudent] = useState(null)
    const [manualId, setManualId] = useState('')
    const [form, setForm] = useState(BLANK_MANUAL)
    const [buy, setBuy] = useState({ manualId: '', quantity: '', unitCost: '', date: today(), note: '' })
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function refresh() {
        const [m, s] = await Promise.all([getManuals(), getSales()])
        setManuals(m); setSales(s)
    }

    useEffect(() => {
        refresh().catch((e) => setError(msg(e)))
        if (canFinance) listLabCourses().then(setCourses).catch(() => {})
    }, [])

    useEffect(() => {
        const q = query.trim()
        if (q.length < 2) { setHits([]); return }
        const timer = setTimeout(() => searchLabStudents(q).then(setHits).catch(() => setHits([])), 300)
        return () => clearTimeout(timer)
    }, [query])

    async function act(action, success) {
        setError(null); setNotice(null); setBusy(true)
        try {
            const result = await action()
            setNotice(typeof success === 'function' ? success(result) : success || result?.message)
            await refresh()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const sellable = manuals.filter((m) => m.active && m.stock > 0)

    function sell(e) {
        e.preventDefault()
        if (!student) { setError('Choose a student first.'); return }
        if (!manualId) { setError('Choose a manual.'); return }
        act(async () => {
            const r = await sellManual(Number(manualId), student.id)
            setStudent(null); setQuery(''); setHits([])
            return r
        })
    }

    function void_(s) {
        if (!window.confirm(`Void the sale of ${s.manualTitle} to ${s.studentName}? The copy goes back into stock.`)) return
        act(() => voidSale(s.id))
    }

    function submitManual(e) {
        e.preventDefault()
        act(async () => {
            await saveManual({
                id: form.id, title: form.title, courseId: form.courseId ? Number(form.courseId) : null,
                unitPrice: Number(form.unitPrice), active: form.active,
            })
            setForm(BLANK_MANUAL)
        }, 'Manual saved.')
    }

    function submitPurchase(e) {
        e.preventDefault()
        act(async () => {
            const r = await addPurchase(Number(buy.manualId), {
                quantity: Number(buy.quantity), unitCost: Number(buy.unitCost), date: buy.date, note: buy.note,
            })
            setBuy({ manualId: buy.manualId, quantity: '', unitCost: '', date: today(), note: '' })
            return r
        })
    }

    const tabs = [['sell', 'Sell a manual'], ['stock', 'Stock & manuals']]

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Manual Sales &amp; Stock</h2>
            <p className="theme-text-muted text-sm mb-6">Record lab manual sales to students and keep track of the copies in stock.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="flex gap-1 mb-6 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {tabs.map(([key, label]) => (
                    <button key={key} onClick={() => { setTab(key); setError(null); setNotice(null) }}
                            className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>
                        {label}
                    </button>
                ))}
            </div>

            {tab === 'sell' && (
                <>
                    <form onSubmit={sell} className="card p-4 mb-6 space-y-4">
                        <div className="relative">
                            <label className="block text-sm font-medium mb-1">Student</label>
                            {student ? (
                                <div className="flex justify-between items-center border theme-border rounded-lg px-3 py-2 text-sm">
                                    <span>{student.matricNumber} · {student.fullName} <span className="theme-text-muted">· {student.programme} {student.level}</span></span>
                                    <button type="button" onClick={() => setStudent(null)} className="text-xs text-red-600 hover:underline">Change</button>
                                </div>
                            ) : (
                                <>
                                    <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Matric number or name"
                                           className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                                    {hits.length > 0 && (
                                        <ul className="absolute z-10 left-0 right-0 mt-1 card divide-y theme-border max-h-64 overflow-y-auto">
                                            {hits.map((h) => (
                                                <li key={h.id}>
                                                    <button type="button" onClick={() => { setStudent(h); setHits([]) }} className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                                        {h.matricNumber} · {h.fullName} <span className="theme-text-muted">· {h.programme} {h.level}</span>
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium mb-1">Manual</label>
                            <select value={manualId} onChange={(e) => setManualId(e.target.value)} className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Choose a manual…</option>
                                {sellable.map((m) => <option key={m.id} value={m.id}>{m.title}{m.courseCode ? ` (${m.courseCode})` : ''} — {naira(m.unitPrice)} · {m.stock} in stock</option>)}
                            </select>
                            {manuals.length > 0 && sellable.length === 0 && <p className="text-xs text-amber-700 mt-1">No manual is in stock right now.</p>}
                        </div>

                        <button type="submit" disabled={busy}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {busy ? 'Saving…' : 'Record sale'}
                        </button>
                    </form>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Recent sales</div>
                        {sales.length === 0 ? (
                            <p className="p-6 text-sm theme-text-muted">No sale has been recorded yet.</p>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                    <tr>
                                        <th className="px-4 py-2">When</th><th className="px-4 py-2">Student</th><th className="px-4 py-2">Manual</th>
                                        <th className="px-4 py-2">Price</th><th className="px-4 py-2">By</th><th className="px-4 py-2"></th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y theme-border">
                                    {sales.map((s) => (
                                        <tr key={s.id} className={s.voided ? 'opacity-50 line-through' : ''}>
                                            <td className="px-4 py-2">{new Date(s.soldAt).toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' })}</td>
                                            <td className="px-4 py-2">{s.matricNumber} <span className="theme-text-muted">· {s.studentName}</span></td>
                                            <td className="px-4 py-2">{s.manualTitle}</td>
                                            <td className="px-4 py-2">{naira(s.unitPrice)}</td>
                                            <td className="px-4 py-2 theme-text-muted">{s.recordedBy}</td>
                                            <td className="px-4 py-2 text-right">
                                                {canFinance && !s.voided && <button onClick={() => void_(s)} disabled={busy} className="text-xs text-red-600 hover:underline no-underline">Void</button>}
                                                {s.voided && <span className="text-xs">voided</span>}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </>
            )}

            {tab === 'stock' && (
                <div className="space-y-6">
                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Manuals ({manuals.length})</div>
                        {manuals.length === 0 ? (
                            <p className="p-6 text-sm theme-text-muted">No manual has been added yet.{canFinance ? ' Add the first one below.' : ''}</p>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                    <tr>
                                        <th className="px-4 py-2">Manual</th><th className="px-4 py-2">Course</th><th className="px-4 py-2">Price</th>
                                        <th className="px-4 py-2">Added</th><th className="px-4 py-2">Sold</th><th className="px-4 py-2">In stock</th>
                                        {canFinance && <th className="px-4 py-2">Avg cost</th>}
                                        <th className="px-4 py-2"></th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y theme-border">
                                    {manuals.map((m) => (
                                        <tr key={m.id} className={m.active ? '' : 'opacity-50'}>
                                            <td className="px-4 py-2 font-medium">{m.title}</td>
                                            <td className="px-4 py-2">{m.courseCode || '—'}</td>
                                            <td className="px-4 py-2">{naira(m.unitPrice)}</td>
                                            <td className="px-4 py-2">{m.purchased}</td>
                                            <td className="px-4 py-2">{m.sold}</td>
                                            <td className={`px-4 py-2 font-semibold ${m.stock <= 5 ? 'text-amber-600' : ''}`}>{m.stock}</td>
                                            {canFinance && <td className="px-4 py-2">{naira(m.averageCost)}</td>}
                                            <td className="px-4 py-2 text-right">
                                                {canFinance && (
                                                    <button onClick={() => setForm({ id: m.id, title: m.title, courseId: m.courseId || '', unitPrice: m.unitPrice, active: m.active })}
                                                            className="text-xs font-medium" style={{ color: 'var(--accent)' }}>Edit</button>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>

                    {canFinance && (
                        <>
                            <form onSubmit={submitManual} className="card p-4">
                                <p className="font-semibold mb-3">{form.id ? 'Edit manual' : 'Add a manual'}</p>
                                <div className="flex gap-3 flex-wrap items-end">
                                    <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required placeholder="Title"
                                           className="border theme-border rounded-lg px-3 py-2 text-sm w-64" />
                                    <select value={form.courseId} onChange={(e) => setForm({ ...form, courseId: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="">Course (optional)</option>
                                        {courses.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}
                                    </select>
                                    <input type="number" min="1" step="0.01" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} required placeholder="Selling price ₦"
                                           className="border theme-border rounded-lg px-3 py-2 text-sm w-40" />
                                    <label className="text-sm flex items-center gap-2 pb-2">
                                        <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> On sale
                                    </label>
                                    <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Save</button>
                                    {form.id && <button type="button" onClick={() => setForm(BLANK_MANUAL)} className="text-sm theme-text-muted hover:underline">Cancel</button>}
                                </div>
                                <p className="text-xs theme-text-muted mt-2">Linking a manual to its course lets the H.O.D compare scores of students who bought it.</p>
                            </form>

                            <form onSubmit={submitPurchase} className="card p-4">
                                <p className="font-semibold mb-3">Add copies to stock</p>
                                <div className="flex gap-3 flex-wrap items-end">
                                    <select value={buy.manualId} onChange={(e) => setBuy({ ...buy, manualId: e.target.value })} required className="border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="">Manual…</option>
                                        {manuals.map((m) => <option key={m.id} value={m.id}>{m.title}</option>)}
                                    </select>
                                    <input type="number" min="1" value={buy.quantity} onChange={(e) => setBuy({ ...buy, quantity: e.target.value })} required placeholder="Copies"
                                           className="border theme-border rounded-lg px-3 py-2 text-sm w-28" />
                                    <input type="number" min="1" step="0.01" value={buy.unitCost} onChange={(e) => setBuy({ ...buy, unitCost: e.target.value })} required placeholder="Cost per copy ₦"
                                           className="border theme-border rounded-lg px-3 py-2 text-sm w-44" />
                                    <input type="date" max={today()} value={buy.date} onChange={(e) => setBuy({ ...buy, date: e.target.value })}
                                           className="border theme-border rounded-lg px-3 py-2 text-sm" />
                                    <input value={buy.note} onChange={(e) => setBuy({ ...buy, note: e.target.value })} placeholder="Note (optional)"
                                           className="border theme-border rounded-lg px-3 py-2 text-sm w-48" />
                                    <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Add to stock</button>
                                </div>
                            </form>
                        </>
                    )}
                </div>
            )}
        </div>
    )
}

export default LabManuals