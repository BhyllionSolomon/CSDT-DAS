import { useEffect, useState } from 'react'
import { getAllUsers, createUser } from '../services/userService'
import { getAllProgrammes } from '../services/programmeService'

function ManageAdvisers() {
    const [users, setUsers] = useState([])
    const [programmes, setProgrammes] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [programmeId, setProgrammeId] = useState('')
    const [submitting, setSubmitting] = useState(false)

    function load() {
        setLoading(true)
        setError(null)
        Promise.all([getAllUsers(), getAllProgrammes()])
            .then(([u, p]) => {
                setUsers(Array.isArray(u) ? u : [])
                setProgrammes(Array.isArray(p) ? p : [])
            })
            .catch((e) => setError(e.response?.data?.message || e.message))
            .finally(() => setLoading(false))
    }
    useEffect(load, [])

    const advisers = users.filter((u) => u.role === 'LEVEL_ADVISER')

    async function handleCreate(e) {
        e.preventDefault()
        setError(null)

        if (!programmeId) {
            setError('Please select a programme before adding an adviser.')
            return
        }

        setSubmitting(true)
        try {
            await createUser({
                username, password, fullName, email,
                role: 'LEVEL_ADVISER',
                programmeId: Number(programmeId),
            })
            setUsername(''); setPassword(''); setFullName(''); setEmail(''); setProgrammeId('')
            load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSubmitting(false) }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-6">Manage Level Advisers</h2>
            {error && <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>}

            {programmes.length === 0 && (
                <div className="mb-4 px-4 py-3 rounded-md bg-amber-50 text-amber-800 text-sm">
                    No programmes were found. Confirm <code>GET /api/programmes</code> returns data before adding an adviser.
                </div>
            )}

            <form onSubmit={handleCreate} className="bg-white rounded-lg shadow p-4 mb-6 flex gap-3 items-end flex-wrap">
                <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" type="email" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Temp password" type="password" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)} required
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                    <option value="">Assign programme…</option>
                    {programmes.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.code})</option>)}
                </select>
                <button type="submit" disabled={submitting}
                        className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                    {submitting ? 'Creating…' : 'Add Adviser'}
                </button>
            </form>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                    <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Programme</th><th className="px-4 py-3">Email</th></tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                    {advisers.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-3 font-medium">{u.fullName}</td>
                            <td className="px-4 py-3">{u.programmeName || '—'}</td>
                            <td className="px-4 py-3">{u.email}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {advisers.length === 0 && (
                    <p className="text-center text-slate-400 py-8">No level advisers yet.</p>
                )}
            </div>
        </div>
    )
}
export default ManageAdvisers