import { useEffect, useState } from 'react'
import { ShieldCheck } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getProfile, requestChange, confirmChange } from '../services/authService'

const ROLE_LABEL = {
    HOD: 'Head of Department',
    ADMIN: 'Administrator',
    LECTURER: 'Lecturer',
    ADJUNCT: 'Adjunct Lecturer',
    LEVEL_ADVISER: 'Level Adviser',
    STUDENT: 'Student',
    LAB_OFFICER: 'Lab Officer',
    TECHNICAL_STAFF: 'Technical Staff',
}

const FIELDS = [
    {
        purpose: 'NAME', label: 'Full name', type: 'text', value: (p) => p.fullName,
        help: 'Changes the name on your account. Official academic records are not affected.',
    },
    {
        purpose: 'USERNAME', label: 'Username', type: 'text', value: (p) => p.username,
        help: 'Use 3 to 50 letters, numbers, dots, dashes or underscores. You stay signed in.',
    },
    {
        purpose: 'EMAIL', label: 'Email address', type: 'email', value: (p) => p.email,
        help: 'The code is sent to the NEW address to prove it is yours, and your old address is told about the change.',
    },
    {
        purpose: 'PHONE', label: 'Phone number', type: 'tel', value: (p) => p.phoneNumber || 'Not set',
        help: 'For now the code is sent to the email address on your account.',
    },
    {
        purpose: 'PASSWORD', label: 'Password', type: 'password', value: () => '••••••••',
        help: 'At least 8 characters. Use something you do not use anywhere else.',
    },
]

const msg = (err) => err.response?.data?.message || err.message

function Input({ label, ...props }) {
    return (
        <div>
            <label className="block text-sm font-medium mb-1">{label}</label>
            <input {...props} required
                   className="w-full border theme-border rounded-lg px-3 py-2 text-sm"
                   style={{ backgroundColor: 'var(--surface)' }} />
        </div>
    )
}

function MyAccount() {
    const { refreshUser } = useAuth()
    const [profile, setProfile] = useState(null)
    const [loading, setLoading] = useState(true)
    const [open, setOpen] = useState(null)
    const [step, setStep] = useState(1)
    const [f, setF] = useState({})
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [done, setDone] = useState(null)

    useEffect(() => {
        getProfile()
            .then(setProfile)
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    const bind = (name) => ({ value: f[name] || '', onChange: (e) => setF({ ...f, [name]: e.target.value }) })

    function start(purpose) {
        setOpen(purpose); setStep(1); setF({}); setError(null); setNotice(null); setDone(null)
    }

    function close() {
        setOpen(null); setStep(1); setF({}); setError(null); setNotice(null)
    }

    async function send(e) {
        if (e) e.preventDefault()
        setError(null); setNotice(null); setBusy(true)
        try {
            const result = await requestChange(open, open === 'PASSWORD' ? null : f.value, f.password)
            setStep(2)
            setNotice(result.message)
        } catch (err) {
            setError(msg(err))
        } finally { setBusy(false) }
    }

    async function confirm(e) {
        e.preventDefault()
        setError(null); setBusy(true)
        try {
            if (open === 'PASSWORD' && f.newPassword !== f.confirm) {
                throw new Error('The two passwords do not match.')
            }
            const result = await confirmChange(open, f.code, open === 'PASSWORD' ? f.newPassword : null)
            await refreshUser(result.token)
            setProfile(await getProfile())
            close()
            setDone(result.message)
        } catch (err) {
            setError(msg(err))
        } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    if (!profile) {
        return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error || 'Could not load your account.'}</div>
    }

    return (
        <div className="max-w-2xl">
            <h2 className="text-2xl font-bold mb-1">My Account</h2>
            <p className="theme-text-muted text-sm mb-6">{ROLE_LABEL[profile.role] || profile.role}</p>

            {done && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{done}</div>}
            {error && !open && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card divide-y theme-border">
                {FIELDS.map((field) => (
                    <div key={field.purpose} className="p-4">
                        <div className="flex items-center justify-between gap-4">
                            <div className="min-w-0">
                                <p className="text-xs theme-text-muted">{field.label}</p>
                                <p className="font-medium truncate">{field.value(profile)}</p>
                            </div>
                            {open !== field.purpose && (
                                <button onClick={() => start(field.purpose)} className="text-sm font-medium shrink-0" style={{ color: 'var(--accent)' }}>
                                    Change
                                </button>
                            )}
                        </div>

                        {open === field.purpose && (
                            <form onSubmit={step === 1 ? send : confirm}
                                  className="mt-3 space-y-3 p-4 rounded-lg" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                <p className="text-xs theme-text-muted">{field.help}</p>

                                {error && <div className="px-3 py-2 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                                {notice && <div className="px-3 py-2 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

                                {step === 1 && (
                                    <>
                                        {open !== 'PASSWORD' && (
                                            <Input label={`New ${field.label.toLowerCase()}`} type={field.type} {...bind('value')} />
                                        )}
                                        <Input label="Your current password" type="password" autoComplete="current-password" {...bind('password')} />
                                    </>
                                )}

                                {step === 2 && (
                                    <>
                                        <Input label="6-digit code" inputMode="numeric" maxLength={6} autoComplete="one-time-code" {...bind('code')} />
                                        {open === 'PASSWORD' && (
                                            <>
                                                <Input label="New password" type="password" minLength={8} autoComplete="new-password" {...bind('newPassword')} />
                                                <Input label="Confirm new password" type="password" minLength={8} autoComplete="new-password" {...bind('confirm')} />
                                            </>
                                        )}
                                    </>
                                )}

                                <div className="flex items-center gap-4">
                                    <button type="submit" disabled={busy}
                                            className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                        {busy ? 'Please wait…' : step === 1 ? 'Send code' : 'Confirm change'}
                                    </button>
                                    {step === 2 && (
                                        <button type="button" onClick={send} disabled={busy} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>
                                            Send a new code
                                        </button>
                                    )}
                                    <button type="button" onClick={close} className="text-sm theme-text-muted hover:underline">Cancel</button>
                                </div>
                            </form>
                        )}
                    </div>
                ))}
            </div>

            <div className="card p-4 mt-6 flex gap-3 text-sm">
                <ShieldCheck size={18} className="shrink-0 mt-0.5" style={{ color: 'var(--accent)' }} />
                <div className="space-y-1">
                    <p className="font-medium">How your account is protected</p>
                    <p className="theme-text-muted">
                        Every change needs your current password and a 6-digit code. A code lasts 10 minutes, works once, and is cancelled
                        after 5 wrong tries. We tell your email address when your email or password changes.
                    </p>
                    <p className="theme-text-muted">
                        If you are running the system on your own computer without email set up, the code appears in the server console window instead.
                    </p>
                </div>
            </div>
        </div>
    )
}

export default MyAccount