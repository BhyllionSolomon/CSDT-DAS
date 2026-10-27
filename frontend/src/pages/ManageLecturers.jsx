import { useEffect, useState } from 'react'
import { getAllUsers, createUser, updateUser, setUserPhone } from '../services/userService'

const msg = (err) => err.response?.data?.message || err.message
const ROLE_LABEL = { LECTURER: 'Lecturer', ADJUNCT: 'Adjunct', LEVEL_ADVISER: 'Level Adviser' }
const EMPTY = { fullName: '', email: '', username: '', role: 'LECTURER' }

// A password nobody ever sees. The person sets their own through "Forgot your password?".
function unseenPassword() {
    const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    const bytes = new Uint8Array(32)
    crypto.getRandomValues(bytes)
    return Array.from(bytes, (b) => alphabet[b % alphabet.length]).join('') + 'aA1!'
}

function ManageLecturers() {
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

    const lecturers = users.filter((u) => ROLE_LABEL[u.role])
    const inactive = lecturers.filter((u) => !u.active).length
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
        act(async () => {
            await createUser({
                username: form.username.trim(), password: unseenPassword(),
                fullName: form.fullName.trim(), email: form.email.trim(), role: form.role,
            })
            setForm(EMPTY)
        }, 'Account created. Ask them to open the sign-in page, choose "Forgot your password?", enter their username, and set their own password with the code emailed to them. Nobody else can see or set it.')
    }

    function handlePhone(u) {
        const phone = window.prompt(`Phone number for ${u.fullName}:`, u.phoneNumber || '')
        if (phone === null) return
        act(() => setUserPhone(u.id, phone.trim()), 'Phone number saved.')
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Manage Lecturers</h2>
            <p className="theme-text-muted text-sm mb-6">
                New lecturer sign-ups stay inactive until you activate them. You can also add accounts and activate or deactivate them.
                Passwords belong to their owners: nobody, including you, can see or set them.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}
            {inactive > 0 && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">
                    {inactive} account(s) are inactive. Activate the ones you recognise so they can sign in.
                </div>
            )}

            <form onSubmit={handleCreate} className="card p-4 mb-6 flex gap-3 items-end flex-wrap">
                <input value={form.fullName} onChange={set('fullName')} placeholder="Full name" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input type="email" value={form.email} onChange={set('email')} placeholder="Their own email" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <input value={form.username} onChange={set('username')} placeholder="Username" required
                       className="border theme-border rounded-lg px-3 py-2 text-sm" />
                <select value={form.role} onChange={set('role')} className="border theme-border rounded-lg px-3 py-2 text-sm">
                    <option value="LECTURER">Lecturer</option>
                    <option value="ADJUNCT">Adjunct</option>
                </select>
                <button type="submit" disabled={busy}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    Add Lecturer
                </button>
            </form>

            <div className="card overflow-hidden">
                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Lecturers ({lecturers.length})</div>
                <table className="w-full text-sm text-left">
                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                    <tr>
                        <th className="px-4 py-2">Name</th>
                        <th className="px-4 py-2">Email</th>
                        <th className="px-4 py-2">Phone</th>
                        <th className="px-4 py-2">Role</th>
                        <th className="px-4 py-2">Status</th>
                        <th className="px-4 py-2"></th>
                    </tr>
                    </thead>
                    <tbody className="divide-y theme-border">
                    {lecturers.map((u) => (
                        <tr key={u.id}>
                            <td className="px-4 py-2 font-medium">{u.fullName}</td>
                            <td className="px-4 py-2">{u.email}</td>
                            <td className="px-4 py-2">
                                {u.phoneNumber || '—'}{' '}
                                <button onClick={() => handlePhone(u)} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>edit</button>
                            </td>
                            <td className="px-4 py-2">{ROLE_LABEL[u.role]}</td>
                            <td className="px-4 py-2">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                    {u.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                            </td>
                            <td className="px-4 py-2 text-right whitespace-nowrap">
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
                {lecturers.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No lecturers yet.</p>}
            </div>
        </div>
    )
}

export default ManageLecturers