import { useEffect, useState } from 'react'
import api from '../services/api'
import { assignAdviser, removeAdviser, getAdviserHistory } from '../services/adviserService'

const LEVEL_CODES = ['100', '200', '300', '400']
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '—')

function ManageAdvisers() {
    const [users, setUsers] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [history, setHistory] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    const [query, setQuery] = useState('')
    const [picked, setPicked] = useState(null)
    const [phone, setPhone] = useState('')
    const [programmeId, setProgrammeId] = useState('')
    const [levelId, setLevelId] = useState('')
    const [submitting, setSubmitting] = useState(false)

    async function load() {
        try {
            const [u, p, l, h] = await Promise.all([
                api.get('/users'), api.get('/programmes'), api.get('/levels'), getAdviserHistory(),
            ])
            setUsers(u.data); setProgrammes(p.data); setLevels(l.data); setHistory(h)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        }
    }

    useEffect(() => { load().finally(() => setLoading(false)) }, [])

    const pool = users.filter((u) => ['LECTURER', 'ADJUNCT', 'LEVEL_ADVISER'].includes(u.role))
    const term = query.trim().toLowerCase()
    const matches = term && !picked ? pool.filter((u) => u.fullName.toLowerCase().includes(term)).slice(0, 8) : []
    const advisers = users.filter((u) => u.role === 'LEVEL_ADVISER')

    function pick(u) {
        setPicked(u); setQuery(u.fullName); setPhone(u.phoneNumber || '')
        setProgrammeId(u.programmeId ? String(u.programmeId) : '')
        setLevelId(u.levelId ? String(u.levelId) : '')
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    function clear() { setPicked(null); setQuery(''); setPhone(''); setProgrammeId(''); setLevelId('') }

    async function handleAssign(e) {
        e.preventDefault()
        setError(null); setMessage(null)
        if (!picked) { setError('Search for a lecturer and select them first.'); return }
        if (!programmeId || !levelId) { setError('Choose a programme and a level.'); return }

        setSubmitting(true)
        try {
            if (phone.trim() && phone.trim() !== (picked.phoneNumber || '')) {
                await api.put(`/users/${picked.id}/phone`, { phoneNumber: phone.trim() })
            }
            const result = await assignAdviser(picked.id, Number(programmeId), Number(levelId))
            setMessage(result.message)
            clear()
            await load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSubmitting(false) }
    }

    async function handleRemove(u) {
        const reason = window.prompt(
            `Remove ${u.fullName} as Level Adviser?\nOptional reason (kept in the archive):`, ''
        )
        if (reason === null) return
        setError(null); setMessage(null)
        try {
            const result = await removeAdviser(u.id, reason)
            setMessage(result.message)
            await load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Manage Level Advisers</h2>
            <p className="theme-text-muted text-sm mb-6">
                Type a lecturer's name (they must have signed up and been activated), choose the programme and level, and
                assign. Assigning the same level again replaces the current adviser. Nothing is ever deleted: every change is archived below.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <form onSubmit={handleAssign} className="card p-5 mb-6 space-y-4 max-w-2xl">
                <div className="relative">
                    <label className="block text-sm font-medium mb-1">Lecturer's name</label>
                    <input value={query} onChange={(e) => { setQuery(e.target.value); if (picked) setPicked(null) }}
                           placeholder="Start typing, e.g. Obong" className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                    {matches.length > 0 && (
                        <ul className="absolute z-10 mt-1 w-full card divide-y theme-border max-h-56 overflow-y-auto">
                            {matches.map((u) => (
                                <li key={u.id}>
                                    <button type="button" onClick={() => pick(u)} className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                        {u.fullName} <span className="theme-text-muted">· {u.email}</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                    {term && !picked && matches.length === 0 && (
                        <p className="text-xs theme-text-muted mt-1">No lecturer found with that name. Have they signed up and been activated?</p>
                    )}
                </div>

                {picked && (
                    <div className="rounded-lg p-4 text-sm space-y-2" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <div className="flex justify-between">
                            <p className="font-semibold">{picked.fullName}</p>
                            <button type="button" onClick={clear} className="text-xs theme-text-muted hover:underline">Change</button>
                        </div>
                        <p><span className="theme-text-muted">Email:</span> {picked.email}</p>
                        <div>
                            <label className="theme-text-muted text-xs">Phone number</label>
                            <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Enter phone number"
                                   className="w-full border theme-border rounded-lg px-3 py-1.5 text-sm mt-1" />
                        </div>
                    </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium mb-1">Programme</label>
                        <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)}
                                className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Select…</option>
                            {programmes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Level</label>
                        <select value={levelId} onChange={(e) => setLevelId(e.target.value)}
                                className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Select…</option>
                            {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                        </select>
                    </div>
                </div>

                <button type="submit" disabled={submitting}
                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {submitting ? 'Saving…' : 'Assign as Level Adviser'}
                </button>
            </form>

            <div className="card overflow-hidden mb-8">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Current Level Advisers ({advisers.length})</div>
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-2">Name</th><th className="px-4 py-2">Programme</th><th className="px-4 py-2">Level</th>
                        <th className="px-4 py-2">Email</th><th className="px-4 py-2">Phone</th><th className="px-4 py-2"></th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {advisers.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-2 font-medium">{u.fullName}</td>
                            <td className="px-4 py-2">{u.programmeName || '—'}</td>
                            <td className="px-4 py-2">{u.levelName || <span className="text-amber-600">not set</span>}</td>
                            <td className="px-4 py-2">{u.email}</td>
                            <td className="px-4 py-2">{u.phoneNumber || '—'}</td>
                            <td className="px-4 py-2 text-right whitespace-nowrap">
                                <button onClick={() => pick(u)} className="text-xs font-medium mr-3" style={{ color: 'var(--accent)' }}>Edit</button>
                                <button onClick={() => handleRemove(u)} className="text-xs text-red-600 hover:underline">Remove</button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {advisers.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No level advisers yet.</p>}
            </div>

            <div className="card overflow-hidden">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Archive — all assignments ({history.length})</div>
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-2">Name</th><th className="px-4 py-2">Programme</th><th className="px-4 py-2">Level</th>
                        <th className="px-4 py-2">From</th><th className="px-4 py-2">To</th><th className="px-4 py-2">Assigned by</th><th className="px-4 py-2">Ended</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {history.map((h) => (
                        <tr key={h.id}>
                            <td className="px-4 py-2 font-medium">{h.fullName}</td>
                            <td className="px-4 py-2">{h.programmeName}</td>
                            <td className="px-4 py-2">{h.levelName}</td>
                            <td className="px-4 py-2">{when(h.startedAt)}</td>
                            <td className="px-4 py-2">
                                {h.endedAt ? when(h.endedAt) : <span className="text-green-700 font-medium">Current</span>}
                            </td>
                            <td className="px-4 py-2">{h.assignedBy || '—'}</td>
                            <td className="px-4 py-2 theme-text-muted">{h.endedAt ? `${h.endedBy || ''}${h.endReason ? ` · ${h.endReason}` : ''}` : ''}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {history.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No history yet.</p>}
            </div>
        </div>
    )
}

export default ManageAdvisers