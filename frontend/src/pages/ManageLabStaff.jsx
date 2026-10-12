import { useEffect, useState } from 'react'
import { getAllUsers, createUser, updateUser, setUserPhone, resetUserPassword } from '../services/userService'

const msg = (err) => err.response?.data?.message || err.message
const ROLES = { LAB_OFFICER: 'Lab Officer', TECHNICAL_STAFF: 'Technical Staff' }
const EMPTY = { fullName: '', email: '', phone: '', username: '', password: '', role: 'LAB_OFFICER' }

function ManageLabStaff() {
    const [users, setUsers] = useState([])
    const [form, setForm] = useState(EMPTY)
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    async function load() {
        try { setUsers(await getAllUsers()) } catch (err) { setError(msg(err)) }
    }

    useEffect(() => { load().finally(() => setLoading(false)) }, [])

    const staff = users.filter((u) => ROLES[u.role])
    const set = (name) => (e) => setForm({ ...form, [name]: e.target.value })

    async function act(action, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            await action()
            setMessage(success)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function handleCreate(e) {
        e.preventDefault()
        if (form.password.length < 6) { setError('Choose a temporary password of at least 6 characters.'); return }
        act(async () => {
            const created = await createUser({
                username: form.username.trim(), password: form.password,
                fullName: form.fullName.trim(), email: form.email.trim(), role: form.role,
            })
            if (form.phone.trim()) await setUserPhone(created.id, form.phone.trim())
            setForm(EMPTY)
        }, 'Account created. Give them the username and temporary password; they can change it later with "Forgot your password?".')
    }

    function handleReset(u) {
        const password = window.prompt(`Temporary password for ${u.fullName} (at least 6 characters):`)
        if (password === null) return
        if (password.length < 6) { setError('The password must be at least 6 characters.'); return }
        act(() => resetUserPassword(u.id, password), `Password for ${u.fullName} has been reset. Share it with them privately.`)
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Manage Lab Staff</h2>
            <p className="theme-text-muted text-sm mb-6">
                Lab Officers and Technical Staff work in a separate laboratory area. They cannot see students, courses, results
                or any other academic page, and only you can create their accounts.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <form onSubmit={handleCreate} className="card p-4 mb-6 flex gap-3 items-end flex-wrap">
                <input value={form.fullName} onChange={set('fullName')} placeholder="Full name" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input type="email" value={form.email} onChange={set('email')} placeholder="Email" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input value={form.phone} onChange={set('phone')} placeholder="Phone number"
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input value={form.username} onChange={set('username')} placeholder="Username" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input type="password" value={form.password} onChange={set('password')} placeholder="Temporary password" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <select value={form.role} onChange={set('role')} className="border theme-border rounded-lg px-3 py-2 text-sm">
                    <option value="LAB_OFFICER">Lab Officer</option>
                    <option value="TECHNICAL_STAFF">Technical Staff</option>
                </select>
                <button type="submit" disabled={busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    Add Lab Staff
                </button>
            </form>

            <div className="card overflow-hidden">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Lab staff ({staff.length})</div>
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-2">Name</th>
                        <th className="px-4 py-2">Role</th>
                        <th className="px-4 py-2">Email</th>
                        <th className="px-4 py-2">Phone</th>
                        <th className="px-4 py-2">Status</th>
                        <th className="px-4 py-2"></th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {staff.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-2 font-medium">{u.fullName}</td>
                            <td className="px-4 py-2">{ROLES[u.role]}</td>
                            <td className="px-4 py-2">{u.email}</td>
                            <td className="px-4 py-2">{u.phoneNumber || '—'}</td>
                            <td className="px-4 py-2">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                    {u.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                            </td>
                            <td className="px-4 py-2 text-right whitespace-nowrap">
                                <button disabled={busy} onClick={() => handleReset(u)} className="text-xs font-medium mr-3" style={{ color: 'var(--accent)' }}>
                                    Reset password
                                </button>
                                <button disabled={busy}
                                        onClick={() => act(() => updateUser(u.id, { active: !u.active }), u.active ? 'Account deactivated.' : 'Account activated.')}
                                        className={`text-xs font-medium ${u.active ? 'text-red-600' : ''}`} style={u.active ? undefined : { color: 'var(--accent)' }}>
                                    {u.active ? 'Deactivate' : 'Activate'}
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
                {staff.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No lab staff yet.</p>}
            </div>
        </div>
    )
}

export default ManageLabStaff