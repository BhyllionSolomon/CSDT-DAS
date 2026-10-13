import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { GraduationCap, ShieldCheck, LineChart, Users2, Sparkles } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { resetPassword } from '../services/authService'
import Logo from '../components/Logo'
import ThemeSwitcher from '../components/ThemeSwitcher'

const FEATURES = [
    { icon: GraduationCap, text: 'Course registration with carry-over enforcement' },
    { icon: LineChart, text: 'Real-time CGPA & academic standing tracking' },
    { icon: Users2, text: 'Role-based dashboards for every user type' },
    { icon: ShieldCheck, text: 'Secure, auditable result processing' },
]

function Field({ label, hint, ...props }) {
    return (
        <div>
            <label className="block text-sm font-medium mb-1">{label}</label>
            <input {...props} required
                   className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none"
                   style={{ borderColor: 'var(--border)', backgroundColor: 'var(--surface-soft)' }} />
            {hint && <p className="text-xs theme-text-muted mt-1">{hint}</p>}
        </div>
    )
}

const TITLES = {
    signin: ['Welcome back 👋', 'Sign in to continue'],
    signup: ['Create your account', 'Students sign up with a matric number already on the roster'],
    forgot: ['Reset your password', 'Enter your matric number or username and the email you signed up with'],
}

function Home() {
    const { login, signup } = useAuth()
    const navigate = useNavigate()

    const [view, setView] = useState('signin')
    const [f, setF] = useState({})
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [busy, setBusy] = useState(false)

    const bind = (name) => ({ value: f[name] || '', onChange: (e) => setF({ ...f, [name]: e.target.value }) })

    function go(next, message = null) {
        setView(next); setF({}); setError(null); setNotice(message)
    }

    async function submit(e) {
        e.preventDefault()
        setError(null); setNotice(null); setBusy(true)
        try {
            if (view === 'signin') {
                await login(f.username, f.password)
                navigate('/')
            } else if (view === 'signup') {
                const result = await signup(f.idNumber, f.fullName, f.email, f.username, f.password)
                if (result.pendingApproval) go('signin', result.message)
                else navigate('/')
            } else {
                if (f.newPassword !== f.confirm) throw new Error('The two passwords do not match.')
                const result = await resetPassword(f.identifier, f.email, f.newPassword)
                go('signin', result.message)
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Something went wrong.')
        } finally { setBusy(false) }
    }

    const [title, subtitle] = TITLES[view]
    const buttonLabel = { signin: 'Sign In', signup: 'Create Account ✨', forgot: 'Reset Password' }[view]

    return (
        <div className="min-h-screen flex">
            <div className="hidden md:flex flex-col justify-between w-1/2 p-12 text-white theme-sidebar relative overflow-hidden">
                <div className="absolute -top-24 -right-24 w-96 h-96 rounded-full accent-gradient opacity-20 blur-3xl" />
                <div className="absolute bottom-0 -left-24 w-72 h-72 rounded-full accent-gradient opacity-10 blur-3xl" />

                <div className="flex items-center gap-3 relative z-10">
                    <Logo className="w-10 h-10" />
                    <span className="text-xl font-bold">CSDT-DAS</span>
                </div>

                <div className="animate-in relative z-10">
          <span className="inline-flex items-center gap-1.5 text-xs font-medium bg-white/10 px-3 py-1 rounded-full mb-4">
            <Sparkles size={12} /> Departmental Academic System
          </span>
                    <h1 className="text-4xl font-bold leading-tight mb-4">
                        Department of Computing Science and Digital Technology <br />department, <span className="accent-text">effortlessly.</span>
                    </h1>
                    <p className="text-white/60 max-w-md mb-8">
                        Faculty of Applied Science  — KolaDaisi University , Ibadan
                    </p>
                    <div className="space-y-3">
                        {FEATURES.map((item, i) => (
                            <div key={i} className="flex items-center gap-3 text-sm text-white/80">
                                <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center shrink-0"><item.icon size={16} /></div>
                                {item.text}
                            </div>
                        ))}
                    </div>
                </div>

                <div className="text-white/40 text-xs relative z-10">
                    Computer Science · Software Engineering · Cybersecurity · Information Technology
                </div>
            </div>

            <div className="w-full md:w-1/2 flex flex-col items-center justify-center p-8 relative" style={{ backgroundColor: 'var(--bg)' }}>
                <div className="absolute top-6 right-6"><ThemeSwitcher /></div>

                <div className="card p-8 w-full max-w-sm animate-in">
                    <div className="md:hidden flex items-center gap-2 mb-6">
                        <Logo className="w-8 h-8" /><span className="font-bold text-lg">CSDT-DAS</span>
                    </div>

                    <h2 className="text-xl font-bold mb-1">{title}</h2>
                    <p className="theme-text-muted text-sm mb-6">{subtitle}</p>

                    {view !== 'forgot' && (
                        <div className="flex mb-6 p-1 rounded-xl" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            {[['signin', 'Sign In'], ['signup', 'Sign Up']].map(([key, label]) => (
                                <button key={key} type="button" onClick={() => go(key)}
                                        className={`flex-1 py-2 text-sm font-medium rounded-lg btn-press transition-all ${
                                            view === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                                        }`}>
                                    {label}
                                </button>
                            ))}
                        </div>
                    )}

                    {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm animate-in">{error}</div>}
                    {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm animate-in">{notice}</div>}

                    <form onSubmit={submit} className="space-y-3 animate-in">
                        {view === 'signin' && (
                            <>
                                <Field label="Username" {...bind('username')} autoComplete="username" />
                                <Field label="Password" type="password" {...bind('password')} autoComplete="current-password" />
                                <button type="button" onClick={() => go('forgot')} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>
                                    Forgot your password?
                                </button>
                            </>
                        )}

                        {view === 'signup' && (
                            <>
                                <Field label="Matric Number or Staff ID" {...bind('idNumber')} placeholder="e.g. KDUCSC25086 or STAFF001"
                                       hint="IDs starting with KDU must already be on the student roster. Staff accounts are approved by the H.O.D." />
                                <Field label="Full Name" {...bind('fullName')} />
                                <Field label="Email" type="email" {...bind('email')} />
                                <Field label="Username" {...bind('username')} autoComplete="username" />
                                <Field label="Password" type="password" minLength={6} {...bind('password')} autoComplete="new-password" />
                            </>
                        )}

                        {view === 'forgot' && (
                            <>
                                <Field label="Matric Number or Username" {...bind('identifier')} />
                                <Field label="Email on your account" type="email" {...bind('email')} />
                                <Field label="New Password" type="password" minLength={6} {...bind('newPassword')} autoComplete="new-password" />
                                <Field label="Confirm New Password" type="password" minLength={6} {...bind('confirm')} autoComplete="new-password" />
                            </>
                        )}

                        <button type="submit" disabled={busy}
                                className="w-full accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold disabled:opacity-50 btn-press shadow-lg shadow-black/10">
                            {busy ? 'Please wait…' : buttonLabel}
                        </button>

                        {view === 'forgot' && (
                            <button type="button" onClick={() => go('signin')} className="w-full text-sm theme-text-muted hover:underline">
                                ← Back to sign in
                            </button>
                        )}
                    </form>
                </div>
            </div>
        </div>
    )
}

export default Home
