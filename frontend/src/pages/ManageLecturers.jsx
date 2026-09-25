import { useEffect, useState } from 'react'
import { getAllUsers, createUser, updateUser } from '../services/userService'

function ManageLecturers() {
    const [users, setUsers] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [role, setRole] = useState('LECTURER')
    const [submitting, setSubmitting] = useState(false)

    function load() {
        setLoading(true)
        getAllUsers().then(setUsers).catch((e) => setError(e.message)).finally(() => setLoading(false))
    }
    useEffect(load, [])

    const lecturers = users.filter((u) => u.role === 'LECTURER' || u.role === 'ADJUNCT')

    async function handleCreate(e) {
        e.preventDefault()
        setError(null)
        setSubmitting(true)
        try {
            await createUser({ username, password, fullName, email, role })
            setUsername(''); setPassword(''); setFullName(''); setEmail('')
            load()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSubmitting(false) }
    }

    async function toggleActive(u) {
        await updateUser(u.id, { active: !u.active })
        load()
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-6">Manage Lecturers</h2>
            {error && <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>}

            <form onSubmit={handleCreate} className="bg-white rounded-lg shadow p-4 mb-6 flex gap-3 items-end flex-wrap">
                <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" type="email" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Temp password" type="password" required
                       className="border border-slate-300 rounded-md px-3 py-2 text-sm" />
                <select value={role} onChange={(e) => setRole(e.target.value)} className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                    <option value="LECTURER">Lecturer</option>
                    <option value="ADJUNCT">Adjunct</option>
                </select>
                <button type="submit" disabled={submitting}
                        className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                    {submitting ? 'Creating…' : 'Add Lecturer'}
                </button>
            </form>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                    <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Role</th><th className="px-4 py-3">Status</th><th className="px-4 py-3"></th></tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                    {lecturers.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-3 font-medium">{u.fullName}</td>
                            <td className="px-4 py-3">{u.email}</td>
                            <td className="px-4 py-3">{u.role}</td>
                            <td className="px-4 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-600'}`}>
                    {u.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                            </td>
                            <td className="px-4 py-3 text-right">
                                <button onClick={() => toggleActive(u)} className="text-xs text-blue-600 hover:underline">
                                    {u.active ? 'Deactivate' : 'Activate'}
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}
export default ManageLecturers