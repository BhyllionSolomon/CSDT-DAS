import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { GraduationCap, ShieldCheck, LineChart, Users2, Sparkles } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import Logo from '../components/Logo'
import ThemeSwitcher from '../components/ThemeSwitcher'

const FEATURES = [
    { icon: GraduationCap, text: 'Course registration with carry-over enforcement' },
    { icon: LineChart, text: 'Real-time CGPA & academic standing tracking' },
    { icon: Users2, text: 'Role-based dashboards for every user type' },
    { icon: ShieldCheck, text: 'Secure, auditable result processing' },
]

function Home() {
    const { login, signup } = useAuth()
    const navigate = useNavigate()

    const [tab, setTab] = useState('signin')
    const [error, setError] = useState(null)
    const [submitting, setSubmitting] = useState(false)

    const [loginUsername, setLoginUsername] = useState('')
    const [loginPassword, setLoginPassword] = useState('')

    const [idNumber, setIdNumber] = useState('')
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [signupUsername, setSignupUsername] = useState('')
    const [signupPassword, setSignupPassword] = useState('')

    async function handleLogin(e) {
        e.preventDefault()
        setError(null)
        setSubmitting(true)
        try {
            await login(loginUsername, loginPassword)
            navigate('/')
        } catch (err) {
            setError(err.response?.data?.message || 'Invalid username or password.')
        } finally { setSubmitting(false) }
    }

    async function handleSignup(e) {
        e.preventDefault()
        setError(null)
        setSubmitting(true)
        try {
            await signup(idNumber, fullName, email, signupUsername, signupPassword)
            navigate('/')
        } catch (err) {
            setError(err.response?.data?.message || 'Could not create account.')
        } finally { setSubmitting(false) }
    }

    const inputStyle = { borderColor: 'var(--border)', backgroundColor: 'var(--surface-soft)' }

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
                        Manage your whole
                        <br />department, <span className="accent-text">effortlessly.</span>
                    </h1>
                    <p className="text-white/60 max-w-md mb-8">
                        Computing Science &amp; Digital Technology — course allocation, result
                        processing, CGPA tracking and student monitoring, all in one place.
                    </p>

                    <div className="space-y-3">
                        {FEATURES.map((f, i) => (
                            <div key={i} className="flex items-center gap-3 text-sm text-white/80">
                                <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center shrink-0">
                                    <f.icon size={16} />
                                </div>
                                {f.text}
                            </div>
                        ))}
                    </div>
                </div>

                <div className="text-white/40 text-xs relative z-10">
                    Computer Science · Software Engineering · Cybersecurity · Information Technology
                </div>
            </div>

            <div className="w-full md:w-1/2 flex flex-col items-center justify-center p-8 relative" style={{ backgroundColor: 'var(--bg)' }}>
                <div className="absolute top-6 right-6">
                    <ThemeSwitcher />
                </div>

                <div className="card p-8 w-full max-w-sm animate-in">
                    <div className="md:hidden flex items-center gap-2 mb-6">
                        <Logo className="w-8 h-8" />
                        <span className="font-bold text-lg">CSDT-DAS</span>
                    </div>

                    <h2 className="text-xl font-bold mb-1">
                        {tab === 'signin' ? 'Welcome back 👋' : 'Create your account'}
                    </h2>
                    <p className="theme-text-muted text-sm mb-6">
                        {tab === 'signin' ? 'Sign in to continue' : 'Join CSDT-DAS in a few seconds'}
                    </p>

                    <div className="flex mb-6 p-1 rounded-xl" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <button
                            onClick={() => { setTab('signin'); setError(null) }}
                            className={`flex-1 py-2 text-sm font-medium rounded-lg btn-press transition-all ${
                                tab === 'signin' ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                            }`}
                        >
                            Sign In
                        </button>
                        <button
                            onClick={() => { setTab('signup'); setError(null) }}
                            className={`flex-1 py-2 text-sm font-medium rounded-lg btn-press transition-all ${
                                tab === 'signup' ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                            }`}
                        >
                            Sign Up
                        </button>
                    </div>

                    {error && (
                        <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm animate-in">
                            {error}
                        </div>
                    )}

                    {tab === 'signin' ? (
                        <form onSubmit={handleLogin} className="space-y-4 animate-in">
                            <div>
                                <label className="block text-sm font-medium mb-1">Username</label>
                                <input value={loginUsername} onChange={(e) => setLoginUsername(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2" style={inputStyle} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Password</label>
                                <input type="password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2" style={inputStyle} />
                            </div>
                            <button type="submit" disabled={submitting}
                                    className="w-full accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold disabled:opacity-50 btn-press shadow-lg shadow-black/10">
                                {submitting ? 'Signing in…' : 'Sign In'}
                            </button>
                        </form>
                    ) : (
                        <form onSubmit={handleSignup} className="space-y-3 animate-in">
                            <div>
                                <label className="block text-sm font-medium mb-1">Matric Number or Staff ID</label>
                                <input value={idNumber} onChange={(e) => setIdNumber(e.target.value)}
                                       placeholder="e.g. KDUCSC25086 or STAFF001" required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none" style={inputStyle} />
                                <p className="text-xs theme-text-muted mt-1">IDs starting with KDU register as students automatically.</p>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Full Name</label>
                                <input value={fullName} onChange={(e) => setFullName(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none" style={inputStyle} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Email</label>
                                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none" style={inputStyle} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Username</label>
                                <input value={signupUsername} onChange={(e) => setSignupUsername(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none" style={inputStyle} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Password</label>
                                <input type="password" value={signupPassword} onChange={(e) => setSignupPassword(e.target.value)} required
                                       className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none" style={inputStyle} />
                            </div>
                            <button type="submit" disabled={submitting}
                                    className="w-full accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold disabled:opacity-50 btn-press shadow-lg shadow-black/10">
                                {submitting ? 'Creating account…' : 'Create Account ✨'}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    )
}

export default Home