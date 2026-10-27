import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllStudents } from '../services/studentService'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import { getOutstandingCourses, registerMultiple } from '../services/registrationService'
import { getStudentForm, assistedClear } from '../services/clearanceService'
import { carryOversFor, currentSessionOf } from '../utils/registrationHelpers'

const msg = (err) => err.response?.data?.message || err.message

function InPersonRegistration() {
    const { user } = useAuth()
    const isHod = user.role === 'HOD' || user.role === 'ADMIN'

    const [students, setStudents] = useState([])
    const [courses, setCourses] = useState([])
    const [session, setSession] = useState(null)
    const [loading, setLoading] = useState(true)

    const [query, setQuery] = useState('')
    const [student, setStudent] = useState(null)
    const [semester, setSemester] = useState('FIRST')
    const [outstanding, setOutstanding] = useState([])
    const [picked, setPicked] = useState({})
    const [form, setForm] = useState(null)
    const [invoice, setInvoice] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        Promise.all([getAllStudents(), getAllCourses(), getAllSessions()])
            .then(([s, c, sess]) => { setStudents(s); setCourses(c); setSession(currentSessionOf(sess)) })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    const term = query.trim().toLowerCase()
    const matches = term && !student
        ? students.filter((s) => s.matricNumber.toLowerCase().includes(term) || s.fullName.toLowerCase().includes(term)).slice(0, 8)
        : []

    async function refreshForm(id = student?.id) {
        if (!isHod || !id) return
        try { setForm(await getStudentForm(id)) } catch (e) { setForm(null) }
    }

    async function choose(s) {
        setStudent(s); setQuery(`${s.matricNumber} — ${s.fullName}`)
        setPicked({}); setInvoice(''); setError(null); setNotice(null); setForm(null)
        try { setOutstanding(await getOutstandingCourses(s.id)) } catch (e) { setOutstanding([]) }
        await refreshForm(s.id)
    }

    function clear() {
        setStudent(null); setQuery(''); setForm(null); setOutstanding([]); setPicked({}); setNotice(null); setError(null)
    }

    const carry = carryOversFor(outstanding, semester)
    const carryIds = new Set(carry.map((c) => c.id))

    const offered = student
        ? courses.filter((c) =>
            c.active !== false && c.levelId === student.levelId && c.semester === semester &&
            c.programmes?.some((p) => p.id === student.programmeId) && !carryIds.has(c.id))
        : []

    const chosen = new Map()
    carry.forEach((c) => chosen.set(c.id, c.unit))
    offered.forEach((c) => { if (picked[c.id]) chosen.set(c.id, c.creditUnit || 0) })

    const ids = [...chosen.keys()]
    const units = [...chosen.values()].reduce((a, b) => a + b, 0)

    async function register() {
        setError(null); setNotice(null); setBusy(true)
        try {
            await registerMultiple(student.id, session.id, semester, ids)
            setNotice('Courses registered.')
            setPicked({})
            await refreshForm()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function clearStudent(e) {
        e.preventDefault()
        setError(null); setNotice(null); setBusy(true)
        try {
            await assistedClear(student.id, invoice)
            setNotice('Invoice recorded and the student is cleared.')
            setInvoice('')
            await refreshForm()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Register a Student in Person</h2>
            <p className="theme-text-muted text-sm mb-6">
                For a student who has paid but cannot register themselves, or whose invoice code was not accepted.
                Find the student, register their courses, then record the invoice they present and clear them.
                {session ? ` Session: ${session.name}.` : ''}
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="card p-4 mb-6 relative">
                <label className="block text-sm font-medium mb-1">Find student</label>
                <input value={query} onChange={(e) => { setQuery(e.target.value); if (student) setStudent(null) }}
                       placeholder="Matric number or name" className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                {matches.length > 0 && (
                    <ul className="absolute z-10 left-4 right-4 mt-1 card divide-y theme-border max-h-64 overflow-y-auto">
                        {matches.map((s) => (
                            <li key={s.id}>
                                <button type="button" onClick={() => choose(s)} className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                    {s.matricNumber} — {s.fullName} <span className="theme-text-muted">· {s.programmeName} {s.levelName}</span>
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
                {student && (
                    <p className="text-sm mt-2">
                        {student.fullName} · {student.programmeName} · {student.levelName}{' '}
                        <button onClick={clear} className="text-xs theme-text-muted hover:underline ml-2">Change</button>
                    </p>
                )}
            </div>

            {student && isHod && form && (
                <div className="card p-4 mb-6">
                    <p className="font-semibold">
                        Status:{' '}
                        <span className={form.state === 'APPROVED' ? 'text-green-700' : form.state === 'NONE' ? 'theme-text-muted' : 'text-amber-700'}>
              {form.state === 'NONE' ? 'Not submitted' : form.state}
            </span>
                    </p>
                    {form.registrationCode && (
                        <p className="text-sm">Registration code <span className="font-mono">{form.registrationCode}</span> · invoice <span className="font-mono">{form.invoiceCode}</span></p>
                    )}
                    <p className="text-xs theme-text-muted mt-2">
                        Registered this session ({form.courses.length} course(s), {form.totalUnits} units):{' '}
                        {form.courses.length ? form.courses.map((c) => c.code).join(', ') : 'none yet'}
                    </p>
                </div>
            )}

            {student && (
                <div className="card p-4 mb-6">
                    <div className="flex items-center justify-between gap-4 flex-wrap mb-3">
                        <p className="font-semibold">Register courses</p>
                        <select value={semester} onChange={(e) => { setSemester(e.target.value); setPicked({}) }}
                                className="border theme-border rounded-lg px-3 py-1.5 text-sm">
                            <option value="FIRST">First semester</option>
                            <option value="SECOND">Second semester</option>
                        </select>
                    </div>

                    {carry.length > 0 && (
                        <div className="mb-3 px-3 py-2 rounded-lg bg-amber-50 text-amber-700 text-sm">
                            Carry-over courses for this semester, registered first automatically: {carry.map((c) => c.code).join(', ')}
                        </div>
                    )}

                    {offered.length === 0 ? (
                        <p className="text-sm theme-text-muted">No courses are set up for this student's programme, level and semester.</p>
                    ) : (
                        <ul className="divide-y theme-border">
                            {offered.map((c) => (
                                <li key={c.id} className="py-2">
                                    <label className="flex items-center gap-3 text-sm">
                                        <input type="checkbox" checked={!!picked[c.id]} onChange={(e) => setPicked({ ...picked, [c.id]: e.target.checked })} />
                                        <span className="font-medium">{c.code}</span>
                                        <span className="flex-1">{c.title}</span>
                                        <span className="theme-text-muted">{c.creditUnit} unit(s) · {c.status || 'C'}</span>
                                    </label>
                                </li>
                            ))}
                        </ul>
                    )}

                    <div className="flex items-center gap-4 mt-4">
                        <button onClick={register} disabled={busy || !session || ids.length === 0}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {busy ? 'Please wait…' : 'Register selected courses'}
                        </button>
                        <span className="text-xs theme-text-muted">
              Selected: {units} unit(s). The required total for this level is checked when you register.
            </span>
                    </div>
                </div>
            )}

            {student && isHod && (
                <form onSubmit={clearStudent} className="card p-4">
                    <p className="font-semibold mb-1">Record the invoice and clear the student</p>
                    <p className="text-xs theme-text-muted mb-3">
                        Check the invoice the student presents against the school's records first. This approves their registration and unlocks
                        class materials, online classes, results and analytics.
                    </p>
                    <div className="flex gap-3 items-end flex-wrap">
                        <input value={invoice} onChange={(e) => setInvoice(e.target.value)} required placeholder="Invoice code"
                               className="border theme-border rounded-lg px-3 py-2 text-sm w-72" />
                        <button type="submit" disabled={busy || (form && form.courses.length === 0)}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            Clear student
                        </button>
                    </div>
                </form>
            )}
        </div>
    )
}

export default InPersonRegistration