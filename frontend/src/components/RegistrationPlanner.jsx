import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOptions } from '../services/registrationRulesService'
import { registerMultiple } from '../services/registrationService'

const msg = (err) => err.response?.data?.message || err.message
const sum = (list) => list.reduce((t, c) => t + (c.unit || 0), 0)

function Section({ title, bar, children }) {
    return (
        <div className="card overflow-hidden mb-6">
            <div className={`${bar} text-white px-4 py-2 text-sm font-semibold`}>{title}</div>
            {children}
        </div>
    )
}

function Row({ c, checked, disabled, onChange, tag }) {
    return (
        <li className="px-4 py-2">
            <label className="flex items-center gap-3 text-sm">
                <input type="checkbox" checked={checked} disabled={disabled} onChange={onChange} />
                <span className="font-medium w-24">{c.code}</span>
                <span className="flex-1">{c.title}</span>
                <span className="theme-text-muted">{c.unit} unit(s) · {c.status || 'C'} · {c.level}</span>
                {tag && <span className="text-xs text-green-700 font-medium">{tag}</span>}
            </label>
        </li>
    )
}

function RegistrationPlanner({ studentId, sessionId, semester, staff, onRegistered }) {
    const [opt, setOpt] = useState(null)
    const [picked, setPicked] = useState({})
    const [back, setBack] = useState({})
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function load() {
        setOpt(await getOptions(studentId, sessionId, semester))
    }

    useEffect(() => {
        setOpt(null); setPicked({}); setBack({}); setNotice(null); setError(null)
        if (studentId && sessionId) load().catch((e) => setError(msg(e)))
    }, [studentId, sessionId, semester])

    if (error && !opt) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
    if (!opt) return <p className="theme-text-muted">Loading…</p>

    const done = new Set(opt.registeredIds)
    const chosen = (list, state) => list.filter((c) => state[c.id] || done.has(c.id))

    const baseList = [...opt.carryOvers, ...chosen(opt.offered, picked)]
    const backList = chosen(opt.backlog, back)
    const base = sum(baseList)
    const extra = sum(backList)
    const ids = [...baseList, ...backList].map((c) => c.id)

    const canRegister = !opt.notice && opt.requiredUnits > 0 && base === opt.requiredUnits
        && extra <= opt.approvedExtra && ids.length > 0

    async function register() {
        setError(null); setNotice(null); setBusy(true)
        try {
            await registerMultiple(studentId, Number(sessionId), semester, ids)
            setNotice('The courses are registered.')
            setPicked({}); setBack({})
            await load()
            if (onRegistered) onRegistered()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    return (
        <div>
            {opt.notice && <div className="mb-4 px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">{opt.notice}</div>}
            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            {opt.carryOvers.length > 0 && (
                <Section title={`Carry-over courses: failed earlier, so compulsory (${opt.carryOvers.length})`} bar="bg-amber-500">
                    <ul className="divide-y theme-border">
                        {opt.carryOvers.map((c) => <Row key={c.id} c={c} checked disabled onChange={() => {}} tag={done.has(c.id) ? 'Registered' : ''} />)}
                    </ul>
                </Section>
            )}

            <Section title={`${opt.level} ${opt.semester === 'FIRST' ? 'first' : 'second'} semester courses`} bar="theme-sidebar">
                {opt.offered.length === 0 ? (
                    <p className="p-4 text-sm theme-text-muted">No courses have been set up for this programme, level and semester yet. Please check with the H.O.D.</p>
                ) : (
                    <ul className="divide-y theme-border">
                        {opt.offered.map((c) => (
                            <Row key={c.id} c={c} checked={done.has(c.id) || !!picked[c.id]} disabled={done.has(c.id)}
                                 onChange={(e) => setPicked({ ...picked, [c.id]: e.target.checked })} tag={done.has(c.id) ? 'Registered' : ''} />
                        ))}
                    </ul>
                )}
            </Section>

            {opt.directEntry && opt.backlog.length > 0 && (
                <Section title={`Lower-level courses still to clear (${opt.backlog.length})`} bar="bg-indigo-600">
                    {opt.approvedExtra > 0 ? (
                        <>
                            <p className="px-4 py-2 text-xs theme-text-muted">
                                {opt.approvedExtra} extra unit(s) are approved for this semester. You have chosen {extra}.
                            </p>
                            <ul className="divide-y theme-border">
                                {opt.backlog.map((c) => (
                                    <Row key={c.id} c={c} checked={done.has(c.id) || !!back[c.id]} disabled={done.has(c.id)}
                                         onChange={(e) => setBack({ ...back, [c.id]: e.target.checked })} tag={done.has(c.id) ? 'Registered' : ''} />
                                ))}
                            </ul>
                        </>
                    ) : (
                        <>
                            <p className="px-4 py-3 text-sm theme-text-muted">
                                You entered at {opt.entryLevel} level, so these lower-level courses are still open to you. To add them you need extra units approved by your Level Adviser.{' '}
                                {staff
                                    ? 'Grant them on the Direct Entry & Extra Units tab.'
                                    : <Link to="/my-adviser" className="font-medium" style={{ color: 'var(--accent)' }}>See who your Level Adviser is</Link>}
                            </p>
                            <ul className="divide-y theme-border opacity-60">
                                {opt.backlog.map((c) => <Row key={c.id} c={c} checked={false} disabled onChange={() => {}} />)}
                            </ul>
                        </>
                    )}
                </Section>
            )}

            <div className="flex items-center gap-4 flex-wrap">
                <button onClick={register} disabled={busy || !canRegister}
                        className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {busy ? 'Please wait…' : 'Register courses'}
                </button>
                <p className={`text-sm ${base === opt.requiredUnits ? 'text-green-700' : 'theme-text-muted'}`}>
                    Carry-over and current-level courses: {base} unit(s) · Required: {opt.requiredUnits || 'not set yet'}
                    {opt.approvedExtra > 0 ? ` · Extra: ${extra} of ${opt.approvedExtra}` : ''}
                </p>
            </div>
        </div>
    )
}

export default RegistrationPlanner