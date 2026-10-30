import { useEffect, useState } from 'react'
import {
    listClearances, approveClearance, rejectClearance, resetClearance,
    approveRegistered, getGate, setGate,
} from '../services/clearanceService'
import { getUnlinkedStudents, linkStudentAccount } from '../services/userService'
import { getAllStudents } from '../services/studentService'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleString('en-GB') : '')

const TABS = [['PENDING', 'Waiting'], ['ATTENTION', 'Needs attention'], ['APPROVED', 'Approved'], ['ALL', 'All']]
const matches = (tab, status) =>
    tab === 'ALL' || (tab === 'ATTENTION' ? ['FLAGGED', 'REJECTED'].includes(status) : status === tab)

const BADGE = {
    PENDING: 'bg-amber-100 text-amber-700',
    APPROVED: 'bg-green-100 text-green-700',
    REJECTED: 'bg-red-100 text-red-700',
    FLAGGED: 'bg-red-100 text-red-700',
}

function LinkRow({ account, students, busy, onLink }) {
    const [query, setQuery] = useState('')
    const [picked, setPicked] = useState(null)

    const term = query.trim().toLowerCase()
    const found = term && !picked
        ? students.filter((s) => s.matricNumber.toLowerCase().includes(term) || s.fullName.toLowerCase().includes(term)).slice(0, 6)
        : []

    return (
        <li className="py-3 relative">
            <div className="flex justify-between gap-4 flex-wrap items-start">
                <div>
                    <p className="font-medium">{account.fullName} <span className="font-normal theme-text-muted">· {account.username}</span></p>
                    <p className="text-xs theme-text-muted">{account.email} · signed up {when(account.createdAt)}</p>
                </div>
                <div className="flex gap-2 items-start">
                    <div className="relative">
                        <input value={query} onChange={(e) => { setQuery(e.target.value); setPicked(null) }}
                               placeholder="Find their roster entry (matric or name)"
                               className="border theme-border rounded-lg px-3 py-1.5 text-sm w-72" />
                        {found.length > 0 && (
                            <ul className="absolute z-10 left-0 right-0 mt-1 card divide-y theme-border max-h-56 overflow-y-auto">
                                {found.map((s) => (
                                    <li key={s.id}>
                                        <button type="button" onClick={() => { setPicked(s); setQuery(`${s.matricNumber} — ${s.fullName}`) }}
                                                className="w-full text-left px-3 py-1.5 text-sm hover:bg-black/5">
                                            {s.matricNumber} — {s.fullName} <span className="theme-text-muted">· {s.programmeName} {s.levelName}</span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <button disabled={!picked || busy} onClick={() => onLink(account, picked)}
                            className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                        Link
                    </button>
                </div>
            </div>
        </li>
    )
}

function RegistrationApprovals() {
    const [rows, setRows] = useState([])
    const [gate, setGateState] = useState(false)
    const [unlinked, setUnlinked] = useState([])
    const [roster, setRoster] = useState([])
    const [tab, setTab] = useState('PENDING')
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    async function load() {
        const [list, setting, accounts, students] = await Promise.all([
            listClearances(), getGate(), getUnlinkedStudents(), getAllStudents(),
        ])
        setRows(list)
        setGateState(setting.enabled)
        setUnlinked(accounts)
        setRoster(students)
    }

    useEffect(() => {
        load().catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    async function act(action, success) {
        setError(null); setMessage(null); setBusy(true)
        try {
            await action()
            if (success) setMessage(success)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function toggleGate() {
        const next = !gate
        const ok = window.confirm(next
            ? 'Switch the lock ON? Students without an approved registration will lose access to class materials, online classes, results and analytics.\n\nTip: use "Approve everyone already registered" first.'
            : 'Switch the lock OFF? Every student regains access.')
        if (ok) act(() => setGate(next), next ? 'The lock is now ON.' : 'The lock is now OFF.')
    }

    function approveEveryone() {
        if (!window.confirm('Approve every pending request, and clear every student who has already registered courses this session?')) return
        act(async () => {
            const result = await approveRegistered()
            setMessage(`${result.count} student(s) approved.`)
        })
    }

    function reject(row) {
        const note = window.prompt(`Why is ${row.fullName}'s invoice not accepted?`, 'Invoice code not found on the school portal.')
        if (note === null) return
        act(() => rejectClearance(row.id, note), 'Rejected. The student is now locked until they see you.')
    }

    function link(account, student) {
        const ok = window.confirm(
            `Link the account "${account.username}" to ${student.matricNumber} — ${student.fullName}?\n\n` +
            'Only do this after checking in person that this account belongs to that student.'
        )
        if (ok) act(async () => {
            const result = await linkStudentAccount(account.id, student.id)
            setMessage(result.message)
        })
    }

    const count = (t) => rows.filter((r) => matches(t, r.status)).length
    const shown = rows.filter((r) => matches(tab, r.status))

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Registration Approvals</h2>
            <p className="theme-text-muted text-sm mb-6">
                Students register freely and submit the invoice code from their school fees payment. Compare each code with the school's records,
                then approve or reject. Reused invoice codes are flagged automatically.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            {unlinked.length > 0 && (
                <div className="card p-4 mb-6">
                    <p className="font-semibold mb-1">Accounts waiting to be linked to a student record ({unlinked.length})</p>
                    <p className="text-xs theme-text-muted mb-2">
                        These students signed in but cannot register because their account is not attached to the roster. Check who they are in person,
                        then pick their roster entry and press Link. They must sign out and in again afterwards.
                    </p>
                    <ul className="divide-y theme-border">
                        {unlinked.map((a) => (
                            <LinkRow key={a.id} account={a} students={roster} busy={busy} onLink={link} />
                        ))}
                    </ul>
                </div>
            )}

            <div className="card p-4 mb-6 flex items-center justify-between gap-4 flex-wrap">
                <div>
                    <p className="font-semibold">
                        Access lock:{' '}
                        <span className={gate ? 'text-red-600' : 'text-green-700'}>{gate ? 'ON' : 'OFF'}</span>
                    </p>
                    <p className="text-xs theme-text-muted max-w-xl">
                        When ON, students without an approved registration cannot open class materials, online classes, results or analytics.
                        Before switching it on, approve everyone who has already registered so nobody is locked out by surprise.
                    </p>
                </div>
                <div className="flex gap-3">
                    <button onClick={approveEveryone} disabled={busy}
                            className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                        Approve everyone already registered
                    </button>
                    <button onClick={toggleGate} disabled={busy}
                            className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                        Switch lock {gate ? 'OFF' : 'ON'}
                    </button>
                </div>
            </div>

            <div className="flex flex-wrap gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {TABS.map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)}
                            className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>
                        {label} ({count(key)})
                    </button>
                ))}
            </div>

            {shown.length === 0 ? (
                <div className="card p-6 text-sm theme-text-muted">Nothing here.</div>
            ) : (
                <ul className="space-y-3">
                    {shown.map((r) => (
                        <li key={r.id} className="card p-4">
                            <div className="flex justify-between gap-4 flex-wrap">
                                <div className="min-w-0">
                                    <p className="font-semibold">
                                        {r.fullName} <span className="font-normal theme-text-muted">· {r.matricNumber}</span>
                                        <span className={`ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${BADGE[r.status]}`}>{r.status}</span>
                                    </p>
                                    <p className="text-xs theme-text-muted">{r.programme} · {r.level} · submitted {when(r.submittedAt)}</p>
                                    <p className="text-sm mt-2">
                                        Invoice code: <span className="font-mono font-semibold">{r.invoiceCode}</span>
                                        <span className="theme-text-muted"> · Registration code: </span>
                                        <span className="font-mono">{r.registrationCode}</span>
                                    </p>
                                    <p className="text-xs theme-text-muted mt-1">
                                        Courses ({r.courses.length}): {r.courses.length ? r.courses.join(', ') : 'none registered'}
                                    </p>
                                    {r.sharedWith.length > 0 && (
                                        <p className="text-xs font-medium text-red-600 mt-1">
                                            The same invoice code was also used by: {r.sharedWith.join(', ')}
                                        </p>
                                    )}
                                    {r.note && <p className="text-xs theme-text-muted mt-1">Note: {r.note}</p>}
                                    {r.decidedBy && r.status !== 'PENDING' && (
                                        <p className="text-xs theme-text-muted">Decided by {r.decidedBy} on {when(r.decidedAt)}</p>
                                    )}
                                </div>

                                <div className="flex gap-3 items-start text-sm shrink-0">
                                    {r.status !== 'APPROVED' && (
                                        <button disabled={busy} onClick={() => act(() => approveClearance(r.id), `${r.fullName} approved.`)}
                                                className="accent-gradient text-white rounded-lg px-4 py-1.5 font-medium btn-press disabled:opacity-50">
                                            Approve
                                        </button>
                                    )}
                                    {r.status !== 'REJECTED' && (
                                        <button disabled={busy} onClick={() => reject(r)} className="text-red-600 hover:underline">
                                            Reject
                                        </button>
                                    )}
                                    <button disabled={busy}
                                            onClick={() => window.confirm(`Reopen ${r.fullName}'s request? They will be able to submit again.`)
                                                && act(() => resetClearance(r.id), 'Reopened.')}
                                            className="theme-text-muted hover:underline">
                                        Reopen
                                    </button>
                                </div>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}

export default RegistrationApprovals