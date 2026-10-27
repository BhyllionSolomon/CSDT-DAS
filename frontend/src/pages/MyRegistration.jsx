import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Printer } from 'lucide-react'
import { getMyClearance, getMyForm, submitInvoice } from '../services/clearanceService'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '')

const TONE = {
    NONE: 'bg-blue-50 text-blue-700',
    PENDING: 'bg-amber-50 text-amber-700',
    APPROVED: 'bg-green-50 text-green-700',
    REJECTED: 'bg-red-50 text-red-700',
    FLAGGED: 'bg-red-50 text-red-700',
}
const LABEL = {
    NONE: 'Not submitted', PENDING: 'Awaiting H.O.D approval', APPROVED: 'Approved',
    REJECTED: 'Not accepted', FLAGGED: 'Not accepted',
}

function MyRegistration() {
    const [status, setStatus] = useState(null)
    const [form, setForm] = useState(null)
    const [invoice, setInvoice] = useState('')
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    async function load() {
        const [s, f] = await Promise.all([getMyClearance(), getMyForm()])
        setStatus(s)
        setForm(f)
    }

    useEffect(() => {
        load().catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    async function handleSubmit(e) {
        e.preventDefault()
        setError(null); setBusy(true)
        try {
            await submitInvoice(invoice)
            setInvoice('')
            await load()
        } catch (err) {
            setError(msg(err))
        } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>
    if (!status || !form) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error || 'Could not load this page.'}</div>

    const hasCourses = form.courses.length > 0
    const state = status.state

    return (
        <div className="max-w-3xl">
            <div className="no-print">
                <h2 className="text-2xl font-bold mb-1">Registration &amp; Payment</h2>
                <p className="theme-text-muted text-sm mb-6">{status.sessionName || 'Current session'}</p>

                {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

                <div className={`mb-6 px-4 py-3 rounded-lg text-sm ${TONE[state]}`}>
                    <p className="font-semibold">{LABEL[state]}</p>
                    <p>{status.message}</p>
                    {status.note && state !== 'APPROVED' && <p className="mt-1">Note: {status.note}</p>}
                </div>

                {state === 'NONE' && (
                    <div className="card p-5 mb-6 space-y-4">
                        <div>
                            <p className="font-semibold">Step 1: register your courses</p>
                            <p className="text-sm theme-text-muted">
                                You have {form.courses.length} course(s) registered this session.{' '}
                                <Link to="/register-courses" className="font-medium" style={{ color: 'var(--accent)' }}>Open Course Registration</Link>
                            </p>
                        </div>
                        <form onSubmit={handleSubmit} className="space-y-2">
                            <p className="font-semibold">Step 2: enter the invoice code from your school fees payment</p>
                            <input value={invoice} onChange={(e) => setInvoice(e.target.value)} required disabled={!hasCourses}
                                   placeholder="Invoice code exactly as printed on your invoice"
                                   className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                            <p className="text-xs theme-text-muted">
                                The H.O.D checks this code against the school's records. A code that is not genuine, or that another student has already used,
                                locks your class materials, online classes, results and analytics until you see the H.O.D in person.
                            </p>
                            <button type="submit" disabled={busy || !hasCourses}
                                    className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                {busy ? 'Please wait…' : 'Get my registration code'}
                            </button>
                        </form>
                    </div>
                )}

                {state !== 'NONE' && (
                    <div className="card p-5 mb-6 flex items-center justify-between gap-4 flex-wrap">
                        <div>
                            <p className="text-xs theme-text-muted">Your registration code</p>
                            <p className="text-2xl font-bold font-mono tracking-wide">{status.registrationCode}</p>
                            <p className="text-xs theme-text-muted mt-1">Invoice code: {status.invoiceCode}</p>
                        </div>
                        {hasCourses && (
                            <button onClick={() => window.print()}
                                    className="flex items-center gap-2 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                                <Printer size={15} /> Print registration form
                            </button>
                        )}
                    </div>
                )}
            </div>

            {state !== 'NONE' && hasCourses && (
                <div className="bg-white text-black p-6 rounded-lg print-area">
                    <div className="text-center mb-4">
                        <p className="font-bold text-base">KOLADAISI UNIVERSITY, IBADAN</p>
                        <p className="font-semibold text-sm">COURSE REGISTRATION FORM</p>
                        <p className="text-xs">Department of Computing Science &amp; Digital Technology · {form.sessionName}</p>
                    </div>

                    <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm mb-4">
                        <p><span className="font-semibold">Name:</span> {form.fullName}</p>
                        <p><span className="font-semibold">Matric No.:</span> {form.matricNumber}</p>
                        <p><span className="font-semibold">Programme:</span> {form.programme}</p>
                        <p><span className="font-semibold">Level:</span> {form.level}</p>
                    </div>

                    <table className="w-full border-collapse text-sm mb-4">
                        <thead>
                        <tr>
                            {['S/N', 'Course Code', 'Course Title', 'Unit', 'Status', 'Semester'].map((h) => (
                                <th key={h} className="border border-black px-2 py-1 text-left">{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {form.courses.map((c, i) => (
                            <tr key={`${c.code}-${c.semester}`}>
                                <td className="border border-black px-2 py-1">{i + 1}</td>
                                <td className="border border-black px-2 py-1">{c.code}</td>
                                <td className="border border-black px-2 py-1">{c.title}</td>
                                <td className="border border-black px-2 py-1">{c.unit}</td>
                                <td className="border border-black px-2 py-1">{c.status}</td>
                                <td className="border border-black px-2 py-1">{c.semester}</td>
                            </tr>
                        ))}
                        <tr>
                            <td colSpan={3} className="border border-black px-2 py-1 font-semibold text-right">Total units</td>
                            <td colSpan={3} className="border border-black px-2 py-1 font-semibold">{form.totalUnits}</td>
                        </tr>
                        </tbody>
                    </table>

                    <div className="border border-black p-3 text-sm mb-6">
                        <p className="font-semibold mb-1">Evidence of payment</p>
                        <p>School invoice code: <span className="font-mono">{form.invoiceCode}</span></p>
                        <p>Registration code: <span className="font-mono font-semibold">{form.registrationCode}</span></p>
                        <p>
                            Status:{' '}
                            <span className="font-semibold">
                {form.state === 'APPROVED'
                    ? `APPROVED by ${form.decidedBy || 'the H.O.D'} on ${when(form.decidedAt)}`
                    : form.state === 'PENDING' ? 'AWAITING H.O.D APPROVAL' : 'NOT ACCEPTED: SEE THE H.O.D'}
              </span>
                        </p>
                    </div>

                    <div className="grid grid-cols-3 gap-10 text-xs mt-10">
                        {['Student signature', 'Level Adviser', 'H.O.D'].map((label) => (
                            <p key={label} className="border-t border-black pt-1 text-center">{label}</p>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}

export default MyRegistration