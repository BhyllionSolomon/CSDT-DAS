import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

const CONDITIONS = [
    ['FUNCTIONAL', 'Functional'], ['NEEDS_ATTENTION', 'Functional but needs attention'],
    ['FAULTY', 'Faulty'], ['UNDER_MAINTENANCE', 'Under maintenance'],
    ['BEYOND_REPAIR', 'Beyond repair'], ['DECOMMISSIONED', 'Decommissioned'],
]
const LABS = [
    ['COMPUTER_SCIENCE', 'Computer Science'], ['SOFTWARE_ENGINEERING', 'Software Engineering'],
    ['CYBERSECURITY', 'Cybersecurity'], ['INFORMATION_TECHNOLOGY', 'Information Technology'],
    ['SHARED', 'Shared facilities'],
]
const CATEGORIES = ['COMPUTER', 'COMPUTER_PERIPHERAL', 'NETWORKING', 'PROJECTOR_DISPLAY', 'FURNITURE', 'ELECTRICAL', 'COOLING_VENTILATION', 'PRINTING', 'OTHER']
const EMPTY = {
    assetTag: '', name: '', category: 'COMPUTER', laboratory: 'COMPUTER_SCIENCE',
    description: '', quantity: 1, conditionStatus: 'FUNCTIONAL', acquiredDate: '',
    roomLocation: '', manufacturer: '', model: '', serialNumber: '', processor: '',
    ram: '', storage: '', operatingSystem: '', networkStatus: '', notes: '', active: true,
}
const label = (items, value) => items.find(x => x[0] === value)?.[1] || value || '—'

export default function LabAssets() {
    const { user } = useAuth()
    const canManage = ['HOD', 'ADMIN', 'LAB_OFFICER'].includes(user?.role)
    const [rows, setRows] = useState([])
    const [form, setForm] = useState(EMPTY)
    const [editing, setEditing] = useState(null)
    const [filter, setFilter] = useState('ALL')
    const [search, setSearch] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    async function load() {
        const response = await api.get('/lab/assets')
        setRows(response.data)
    }
    useEffect(() => { load().catch(e => setError(e.response?.data?.message || 'Could not load the asset register.')) }, [])

    const visible = useMemo(() => rows.filter(r =>
        (filter === 'ALL' || r.laboratory === filter) &&
        [r.assetTag, r.name, r.category, r.serialNumber, r.roomLocation].join(' ').toLowerCase().includes(search.toLowerCase())
    ), [rows, filter, search])

    function startEdit(asset) {
        setEditing(asset.id)
        setForm({ ...EMPTY, ...asset, acquiredDate: asset.acquiredDate || '' })
        setError(''); setNotice('')
    }
    function reset() { setEditing(null); setForm(EMPTY) }

    async function save(e) {
        e.preventDefault(); setBusy(true); setError(''); setNotice('')
        try {
            const payload = { ...form, quantity: Number(form.quantity), acquiredDate: form.acquiredDate || null }
            if (editing) await api.put('/lab/assets/' + editing, payload)
            else await api.post('/lab/assets', payload)
            await load(); reset(); setNotice('Asset record saved.')
        } catch (e) { setError(e.response?.data?.message || e.response?.data?.error || 'Unable to save this asset.') }
        finally { setBusy(false) }
    }

    async function condition(id, conditionStatus) {
        setError(''); setNotice('')
        try {
            await api.patch('/lab/assets/' + id + '/condition', { conditionStatus })
            await load(); setNotice('Equipment condition updated.')
        } catch (e) { setError(e.response?.data?.message || 'Could not update condition.') }
    }

    const input = (key, title, type = 'text') => <label className="block text-sm">
        <span className="block font-medium mb-1">{title}</span>
        <input type={type} value={form[key] ?? ''} onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
            className="w-full border theme-border rounded-lg px-3 py-2 bg-transparent" />
    </label>
    const select = (key, title, options) => <label className="block text-sm">
        <span className="block font-medium mb-1">{title}</span>
        <select value={form[key]} onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
            className="w-full border theme-border rounded-lg px-3 py-2 bg-transparent">
            {options.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
    </label>

    return <div className="max-w-6xl space-y-5">
        <div>
            <h2 className="text-2xl font-bold">Laboratory Equipment &amp; Assets</h2>
            <p className="theme-text-muted text-sm mt-1">Department-wide register of computers, peripherals, facilities and equipment.</p>
        </div>
        {error && <div role="alert" className="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{error}</div>}
        {notice && <div role="status" className="rounded-lg bg-green-50 text-green-700 p-3 text-sm">{notice}</div>}

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {[
                ['Registered assets', rows.length],
                ['Units recorded', rows.reduce((n, r) => n + Number(r.quantity || 0), 0)],
                ['Need attention', rows.filter(r => ['NEEDS_ATTENTION','FAULTY','UNDER_MAINTENANCE'].includes(r.conditionStatus)).length],
                ['Beyond repair', rows.filter(r => r.conditionStatus === 'BEYOND_REPAIR').length],
            ].map(([title, value]) => <div className="card p-4" key={title}><p className="text-xs theme-text-muted">{title}</p><p className="text-2xl font-bold mt-1">{value}</p></div>)}
        </div>

        {canManage && <section className="card p-4">
            <div className="flex justify-between items-center gap-3 mb-4">
                <h3 className="font-semibold">{editing ? 'Edit asset' : 'Register new asset'}</h3>
                {editing && <button type="button" onClick={reset} className="text-sm underline">Cancel edit</button>}
            </div>
            <form onSubmit={save} className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {input('assetTag', 'Asset tag / ID')}
                {input('name', 'Equipment name')}
                {select('category', 'Category', CATEGORIES.map(x => [x, x.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())]))}
                {select('laboratory', 'Assigned laboratory', LABS)}
                {input('quantity', 'Quantity', 'number')}
                {select('conditionStatus', 'Condition', CONDITIONS)}
                {input('acquiredDate', 'Acquisition date', 'date')}
                {input('roomLocation', 'Room / location')}
                {input('manufacturer', 'Manufacturer')}
                {input('model', 'Model')}
                {input('serialNumber', 'Serial number')}
                {input('processor', 'Processor / CPU')}
                {input('ram', 'RAM')}
                {input('storage', 'Storage')}
                {input('operatingSystem', 'Operating system')}
                {input('networkStatus', 'Network status')}
                <label className="block text-sm md:col-span-3"><span className="block font-medium mb-1">Description / notes</span>
                    <textarea value={form.notes} onChange={e => setForm(p => ({ ...p, notes: e.target.value }))} rows={2}
                        className="w-full border theme-border rounded-lg px-3 py-2 bg-transparent" /></label>
                <div className="md:col-span-3 flex gap-2"><button disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm disabled:opacity-50">{busy ? 'Saving…' : editing ? 'Update asset' : 'Add asset'}</button>
                    {editing && <button type="button" onClick={reset} className="border theme-border rounded-lg px-4 py-2 text-sm">Clear</button>}</div>
            </form>
        </section>}

        <section className="card overflow-hidden">
            <div className="p-4 border-b theme-border flex flex-col md:flex-row gap-3 md:items-center md:justify-between">
                <h3 className="font-semibold">Asset register ({visible.length})</h3>
                <div className="flex flex-col sm:flex-row gap-2">
                    <input aria-label="Search assets" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search tag, name, serial…" className="border theme-border rounded-lg px-3 py-2 text-sm bg-transparent" />
                    <select aria-label="Filter laboratory" value={filter} onChange={e => setFilter(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm bg-transparent">
                        <option value="ALL">All laboratories</option>{LABS.map(([v,t]) => <option key={v} value={v}>{t}</option>)}
                    </select>
                </div>
            </div>
            {visible.length === 0 ? <p className="p-6 text-sm theme-text-muted">No matching assets recorded.</p> :
                <div className="overflow-x-auto"><table className="w-full text-sm min-w-[900px]">
                    <thead><tr className="theme-sidebar text-white text-left"><th className="p-3">Asset</th><th className="p-3">Laboratory</th><th className="p-3">Category</th><th className="p-3">Qty</th><th className="p-3">Specifications</th><th className="p-3">Condition</th><th className="p-3">Action</th></tr></thead>
                    <tbody>{visible.map(r => <tr key={r.id} className="border-b theme-border align-top">
                        <td className="p-3"><b>{r.name}</b><div className="text-xs theme-text-muted">{r.assetTag} · {r.roomLocation || 'No location'}</div></td>
                        <td className="p-3">{label(LABS, r.laboratory)}</td><td className="p-3">{r.category.replaceAll('_',' ')}</td><td className="p-3">{r.quantity}</td>
                        <td className="p-3 text-xs">{[r.processor, r.ram, r.storage, r.operatingSystem, r.networkStatus].filter(Boolean).join(' · ') || '—'}</td>
                        <td className="p-3"><select value={r.conditionStatus} disabled={user?.role === 'TECHNICAL_STAFF' ? false : !canManage} onChange={e => condition(r.id, e.target.value)} className="border theme-border rounded px-2 py-1 bg-transparent text-xs">{CONDITIONS.map(([v,t]) => <option key={v} value={v}>{t}</option>)}</select></td>
                        <td className="p-3">{canManage && <button onClick={() => startEdit(r)} className="underline text-xs">Edit</button>}</td>
                    </tr>)}</tbody>
                </table></div>}
        </section>
        <p className="text-xs theme-text-muted">Changes are attributed to the signed-in account. Technical staff can update equipment condition but cannot create or edit asset master records.</p>
    </div>
}
