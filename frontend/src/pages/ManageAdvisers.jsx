import { useEffect, useState } from 'react'
import api from '../services/api'

const LEVEL_CODES = ['100', '200', '300', '400']

function ManageAdvisers() {
    const [users, setUsers] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    const [query, setQuery] = useState('')
    const [picked, setPicked] = useState(null)
    const [phone, setPhone] = useState('')
    const [programmeId, setProgrammeId] = useState('')
    const [levelId, setLevelId] = useState('')
    const [submitting, setSubmitting] = useState(false)

    function load() {
        return Promise.all([api.get('/users'), api.get('/programmes'), api.get('/levels')])
            .then(([u, p, l]) => { setUsers(u.data); setProgrammes(p.data); setLevels(l.data) })
            .catch((err) => setError(err.response?.data?.message || err.message))
    }

    useEffect(() => { load().finally(() => setLoading(false)) }, [])

    const pool = users.filter((u) => ['LECTURER', 'ADJUNCT', 'LEVEL_ADVISER'].includes(u.role))
    const term = query.trim().toLowerCase()
    const matches = term && !picked
        ? pool.filter((u) => u.fullName.toLowerCase().includes(term)).slice(0, 8)
        : []
    const advisers = users.filter((u) => u.role === 'LEVEL_ADVISER')

    function pick(u) {
        setPicked(u)
        setQuery(u.fullName)
        setPhone(u.phoneNumber || '')
        setProgrammeId(u.programmeId ? String(u.programmeId) : '')
        setLevelId(u.levelId ? String(u.levelId) : '')
    }

    function clearPick() {
        setPicked(null); setQuery(''); setPhone(''); setProgrammeId(''); setLevelId('')
    }

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
            await api.put(`/users/${picked.id}/assign-level-adviser`, {
                programmeId: Number(programmeId),
                levelId: Number(levelId),
            })

            const prog = programmes.find((p) => String(p.id) === programmeId)
            const lvl = levels.find((l) => String(l.id) === levelId)
            setMessage(`${picked.fullName} is now the Level Adviser for ${lvl?.name} ${prog?.name}.`)
            clearPick()
            await load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSubmitting(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Manage Level Advisers</h2>
            <p className="theme-text-muted text-sm mb-6">
                Type a lecturer's name (they must have signed up first), choose the programme and level,
                and assign. An account advises one programme and level at a time; assigning again moves them.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <form onSubmit={handleAssign} className="card p-5 mb-6 space-y-4 max-w-2xl">
                <div className="relative">
                    <label className="block text-sm font-medium mb-1">Lecturer's name</label>
                    <input
                        value={query}
                        onChange={(e) => { setQuery(e.target.value); if (picked) setPicked(null) }}
                        placeholder="Start typing, e.g. Obong"
                        className="w-full border theme-border rounded-lg px-3 py-2 text-sm"
                    />
                    {matches.length > 0 && (
                        <ul className="absolute z-10 mt-1 w-full card divide-y theme-border max-h-56 overflow-y-auto">
                            {matches.map((u) => (
                                <li key={u.id}>
                                    <button type="button" onClick={() => pick(u)}
                                            className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                        {u.fullName} <span className="theme-text-muted">· {u.email}</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                    {term && !picked && matches.length === 0 && (
                        <p className="text-xs theme-text-muted mt-1">No lecturer found with that name. Have they signed up yet?</p>
                    )}
                </div>

                {picked && (
                    <div className="rounded-lg p-4 text-sm space-y-2" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <div className="flex justify-between">
                            <p className="font-semibold">{picked.fullName}</p>
                            <button type="button" onClick={clearPick} className="text-xs theme-text-muted hover:underline">Change</button>
                        </div>
                        <p><span className="theme-text-muted">Email:</span> {picked.email}</p>
                        <div>
                            <label className="theme-text-muted text-xs">Phone number</label>
                            <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Enter phone number"
                                   className="w-full border theme-border rounded-lg px-3 py-1.5 text-sm mt-1" />
                        </div>
                        <p className="text-xs theme-text-muted">Current role: {picked.role}</p>
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
                    {submitting ? 'Assigning…' : 'Assign as Level Adviser'}
                </button>
            </form>

            <div className="card overflow-hidden">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Current Level Advisers ({advisers.length})</div>
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-2">Name</th>
                        <th className="px-4 py-2">Programme</th>
                        <th className="px-4 py-2">Level</th>
                        <th className="px-4 py-2">Email</th>
                        <th className="px-4 py-2">Phone</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {advisers.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-2 font-medium">{u.fullName}</td>
                            <td className="px-4 py-2">{u.programmeName || '—'}</td>
                            <td className="px-4 py-2">{u.levelName || '—'}</td>
                            <td className="px-4 py-2">{u.email}</td>
                            <td className="px-4 py-2">{u.phoneNumber || '—'}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {advisers.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No level advisers yet.</p>}
            </div>
        </div>
    )
}

export default ManageAdvisers