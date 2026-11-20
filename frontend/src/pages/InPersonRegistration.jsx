import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllStudents } from '../services/studentService'
import { getAllSessions } from '../services/sessionService'
import { currentSessionOf } from '../utils/registrationHelpers'
import { getStudentForm, assistedClear } from '../services/clearanceService'
import {
    listDirectEntry, setDirectEntry, listGrants, grantExtra, revokeGrant,
} from '../services/registrationRulesService'
import RegistrationPlanner from '../components/RegistrationPlanner'

const msg = (err) => err.response?.data?.message || err.message

function StudentPicker({ students, onPick, placeholder }) {
    const [query, setQuery] = useState('')
    const term = query.trim().toLowerCase()
    const found = term
        ? students.filter((s) => s.matricNumber.toLowerCase().includes(term) || s.fullName.toLowerCase().includes(term)).slice(0, 8)
        : []

    return (
        <div className="relative">
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={placeholder}
                   className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
            {found.length > 0 && (
                <ul className="absolute z-10 left-0 right-0 mt-1 card divide-y theme-border max-h-64 overflow-y-auto">
                    {found.map((s) => (
                        <li key={s.id}>
                            <button type="button" onClick={() => { onPick(s); setQuery('') }} className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                {s.matricNumber} — {s.fullName} <span className="theme-text-muted">· {s.programmeName} {s.levelName}</span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}

function InPersonRegistration() {
    const { user } = useAuth()
    const isHod = user.role === 'HOD' || user.role === 'ADMIN'

    const [tab, setTab] = useState('register')
    const [students, setStudents] = useState([])
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [busy, setBusy] = useState(false)

    // register tab
    const [student, setStudent] = useState(null)
    const [form, setForm] = useState(null)
    const [invoice, setInvoice] = useState('')

    // direct entry tab
    const [direct, setDirect] = useState([])
    const [grants, setGrants] = useState([])
    const [entryLevel, setEntryLevel] = useState('200')
    const [grantForm, setGrantForm] = useState({ units: '', note: '' })
    const [grantFor, setGrantFor] = useState(null)

    useEffect(() => {
        Promise.all([getAllStudents(), getAllSessions()])
            .then(([st, se]) => {
                setStudents(st); setSessions(se)
                const current = currentSessionOf(se)
                if (current) setSessionId(String(current.id))
            })
            .catch((e) => setError(msg(e)))
    }, [])

    async function loadDirect() {
        if (!sessionId) return
        const [d, g] = await Promise.all([listDirectEntry(), listGrants(sessionId)])
        setDirect(d); setGrants(g)
    }

    useEffect(() => {
        if (tab === 'direct') loadDirect().catch((e) => setError(msg(e)))
    }, [tab, sessionId])

    async function loadForm(id = student?.id) {
        if (!isHod || !id) return
        try { setForm(await getStudentForm(id)) } catch (e) { setForm(null) }
    }

    async function act(action, success) {
        setError(null); setNotice(null); setBusy(true)
        try {
            await action()
            if (success) setNotice(success)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function pickStudent(s) {
        setStudent(s); setForm(null); setInvoice(''); setError(null); setNotice(null)
        loadForm(s.id)
    }

    function clearStudent(e) {
        e.preventDefault()
        act(async () => { await assistedClear(student.id, invoice); setInvoice(''); await loadForm() },
            'Invoice recorded and the student is cleared.')
    }

    function markStudent(s) {
        act(async () => { await setDirectEntry(s.id, Number(entryLevel)); await loadDirect() },
            `${s.fullName} is marked as a direct-entry student (${entryLevel} level).`)
    }

    function unmark(d) {
        if (!window.confirm(`Remove the direct-entry mark from ${d.fullName}? Their lower-level courses will close.`)) return
        act(async () => { await setDirectEntry(d.studentId, 0); await loadDirect() }, 'Direct-entry mark removed.')
    }

    function submitGrant(e) {
        e.preventDefault()
        act(async () => {
            await grantExtra({
                studentId: grantFor.studentId, sessionId: Number(sessionId), semester,
                units: Number(grantForm.units), note: grantForm.note,
            })
            setGrantFor(null); setGrantForm({ units: '', note: '' })
            await loadDirect()
        }, 'Extra units approved.')
    }

    const tabs = [['register', 'Register a student'], ['direct', 'Direct Entry & Extra Units']]
    const semesterText = semester === 'FIRST' ? 'first' : 'second'

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Register in Person</h2>
            <p className="theme-text-muted text-sm mb-6">
                Register courses for a student who cannot do it themselves, and manage direct-entry students and their extra units.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="flex gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {tabs.map(([key, label]) => (
                    <button key={key} onClick={() => { setTab(key); setError(null); setNotice(null) }}
                            className={`px-4 py-2 text-sm font-medium rounded-lg btn-press ${tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>
                        {label}
                    </button>
                ))}
            </div>

            <div className="card p-4 mb-6 flex gap-4 flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={sessionId} onChange={(e) => setSessionId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => setSemester(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
            </div>

            {tab === 'register' && (
                <>
                    <div className="card p-4 mb-6">
                        <label className="block text-sm font-medium mb-1">Find student</label>
                        <StudentPicker students={students} onPick={pickStudent} placeholder="Matric number or name" />
                        {student && <p className="text-sm mt-2">{student.fullName} · {student.programmeName} · {student.levelName}</p>}
                    </div>

                    {student && isHod && form && (
                        <div className="card p-4 mb-6 text-sm">
                            <p className="font-semibold">Payment status: {form.state === 'NONE' ? 'Not submitted' : form.state}</p>
                            {form.registrationCode && <p>Registration code <span className="font-mono">{form.registrationCode}</span> · invoice <span className="font-mono">{form.invoiceCode}</span></p>}
                            <p className="text-xs theme-text-muted mt-1">
                                Registered this session: {form.courses.length ? form.courses.map((c) => c.code).join(', ') : 'none yet'}
                            </p>
                        </div>
                    )}

                    {student && (
                        <RegistrationPlanner staff studentId={student.id} sessionId={sessionId} semester={semester} onRegistered={() => loadForm()} />
                    )}

                    {student && isHod && (
                        <form onSubmit={clearStudent} className="card p-4 mt-6">
                            <p className="font-semibold mb-1">Record the invoice and clear the student</p>
                            <p className="text-xs theme-text-muted mb-3">Check the invoice against the school's records first. This approves their registration.</p>
                            <div className="flex gap-3 flex-wrap">
                                <input value={invoice} onChange={(e) => setInvoice(e.target.value)} required placeholder="Invoice code"
                                       className="border theme-border rounded-lg px-3 py-2 text-sm w-72" />
                                <button type="submit" disabled={busy || (form && form.courses.length === 0)}
                                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                    Clear student
                                </button>
                            </div>
                        </form>
                    )}
                </>
            )}

            {tab === 'direct' && (
                <div className="space-y-6">
                    <div className="card p-4">
                        <p className="font-semibold mb-1">Mark a direct-entry student</p>
                        <p className="text-xs theme-text-muted mb-3">
                            A direct-entry student starts at 200 or 300 level. Their lower-level courses stay open to them, with the extra units you approve, until they have cleared them.
                        </p>
                        <div className="flex gap-3 items-start flex-wrap">
                            <div className="w-80"><StudentPicker students={students} onPick={markStudent} placeholder="Find the student to mark" /></div>
                            <select value={entryLevel} onChange={(e) => setEntryLevel(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="200">Entered at 200 level</option>
                                <option value="300">Entered at 300 level</option>
                            </select>
                        </div>
                        <p className="text-xs theme-text-muted mt-2">Picking a student marks them straight away.</p>
                    </div>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Direct-entry students ({direct.length})</div>
                        {direct.length === 0 ? (
                            <p className="p-6 text-sm theme-text-muted">No direct-entry student is marked yet.</p>
                        ) : (
                            <ul className="divide-y theme-border">
                                {direct.map((d) => {
                                    const g = grants.find((x) => x.studentId === d.studentId && x.semester === semester)
                                    return (
                                        <li key={d.studentId} className="px-4 py-3">
                                            <div className="flex justify-between gap-4 flex-wrap items-center">
                                                <div>
                                                    <p className="font-medium">{d.matricNumber} <span className="font-normal">· {d.fullName}</span></p>
                                                    <p className="text-xs theme-text-muted">{d.programme} · now {d.level} · entered at {d.entryLevel} level</p>
                                                </div>
                                                <div className="flex gap-3 items-center text-sm">
                                                    {g ? (
                                                        <span className="text-green-700">
                              {g.units} extra unit(s) approved for the {semesterText} semester
                              <button onClick={() => act(async () => { await revokeGrant(g.id); await loadDirect() }, 'Extra units withdrawn.')}
                                      className="ml-2 text-xs text-red-600 hover:underline">Withdraw</button>
                            </span>
                                                    ) : (
                                                        <button onClick={() => { setGrantFor(d); setGrantForm({ units: '', note: '' }) }}
                                                                className="font-medium" style={{ color: 'var(--accent)' }}>Approve extra units</button>
                                                    )}
                                                    <button onClick={() => unmark(d)} className="text-xs text-red-600 hover:underline">Remove mark</button>
                                                </div>
                                            </div>

                                            {grantFor?.studentId === d.studentId && (
                                                <form onSubmit={submitGrant} className="mt-3 p-3 rounded-lg flex gap-3 items-end flex-wrap" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                                    <div>
                                                        <label className="block text-xs font-medium mb-1">Extra units ({semesterText} semester)</label>
                                                        <input type="number" min="1" max="24" required value={grantForm.units}
                                                               onChange={(e) => setGrantForm({ ...grantForm, units: e.target.value })}
                                                               className="border theme-border rounded-lg px-3 py-1.5 text-sm w-28" />
                                                    </div>
                                                    <div>
                                                        <label className="block text-xs font-medium mb-1">Note (optional)</label>
                                                        <input value={grantForm.note} onChange={(e) => setGrantForm({ ...grantForm, note: e.target.value })}
                                                               className="border theme-border rounded-lg px-3 py-1.5 text-sm w-64" />
                                                    </div>
                                                    <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">Approve</button>
                                                    <button type="button" onClick={() => setGrantFor(null)} className="text-sm theme-text-muted hover:underline">Cancel</button>
                                                </form>
                                            )}
                                        </li>
                                    )
                                })}
                            </ul>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}

export default InPersonRegistration