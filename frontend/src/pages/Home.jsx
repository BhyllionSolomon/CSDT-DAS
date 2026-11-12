import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    ChevronLeft, ChevronRight, ExternalLink, ClipboardList, LineChart, CalendarDays, UserCheck, GraduationCap,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { requestPasswordReset, confirmPasswordReset } from '../services/authService'
import { API_URL } from '../services/api'
import { getPublicSlides, slideImageUrl } from '../services/homeService'
import Logo from '../components/Logo'
import ThemeSwitcher from '../components/ThemeSwitcher'

const SIWES_URL = 'https://ems-frontend-fv32.onrender.com'
const SLIDE_MS = 6000

// Shown until the department uploads its own pictures, and for any slide without one.
const FALLBACK = [
    { eyebrow: 'Welcome to CSDT', heading: 'Shaping the Future Through Technology',
        body: "Discover a department committed to developing the knowledge, skills and creativity needed to solve tomorrow's digital challenges.",
        image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=75' },
    { eyebrow: 'Your Future Starts Here', heading: 'Prepare for a Digital World',
        body: 'Build practical skills, develop critical thinking and prepare for meaningful careers in the global technology industry.',
        image: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1200&q=75' },
    { eyebrow: 'Research and Innovation', heading: 'Creating Solutions That Matter',
        body: 'From artificial intelligence to cybersecurity, our discipline offers opportunities to explore ideas and develop technology for real-world needs.',
        image: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1200&q=75' },
]

const FEATURES = [
    [ClipboardList, 'Registration'],
    [LineChart, 'Results & CGPA'],
    [CalendarDays, 'Timetable'],
    [UserCheck, 'Attendance'],
    [GraduationCap, 'Final-year projects'],
]

const CSS = `
@keyframes csdt-drift { 0%,100% { transform: translate(0,0) scale(1) } 50% { transform: translate(28px,-22px) scale(1.08) } }
.csdt-blob { position: absolute; border-radius: 9999px; filter: blur(70px); opacity: .28; animation: csdt-drift 18s ease-in-out infinite; pointer-events: none }
@media (prefers-reduced-motion: reduce) { .csdt-blob { animation: none } }
@media (max-height: 640px) { .csdt-banner { display: none } }
`

function useCompact() {
    const query = '(max-width: 640px)'
    const [compact, setCompact] = useState(() => window.matchMedia(query).matches)
    useEffect(() => {
        const m = window.matchMedia(query)
        const handler = (e) => setCompact(e.matches)
        m.addEventListener('change', handler)
        return () => m.removeEventListener('change', handler)
    }, [])
    return compact
}

function Field({ label, hint, ...props }) {
    return (
        <div>
            <label className="block text-xs font-semibold mb-1 theme-text-muted">{label}</label>
            <input {...props} required className="w-full h-11 border theme-border rounded-xl px-3 text-sm" />
            {hint && <p className="text-[11px] theme-text-muted mt-1 leading-snug">{hint}</p>}
        </div>
    )
}

function SlideLayers({ slides, index }) {
    return slides.map((s, i) => (
        <div key={i} aria-hidden={i !== index} className="absolute inset-0"
             style={{
                 opacity: i === index ? 1 : 0, transition: 'opacity 1.4s ease-in-out',
                 backgroundColor: 'var(--accent)', backgroundImage: `url("${s.image}")`,
                 backgroundSize: 'cover', backgroundPosition: 'center',
             }} />
    ))
}

// Says what is wrong in plain words.
function explain(err) {
    if (!err.isAxiosError) return err.message || 'Something went wrong.'
    if (!API_URL) return 'This site is not connected to its server yet. Please tell the administrator.'
    if (!err.response) return 'Cannot reach the server. Check your internet connection. If the server was idle, wait a minute and try again.'
    if (err.response.data?.message) return err.response.data.message
    return err.response.status === 401 ? 'Invalid username or password.' : `Something went wrong (error ${err.response.status}).`
}

function Home() {
    const { login, signup } = useAuth()
    const navigate = useNavigate()
    const compact = useCompact()

    const [view, setView] = useState('signin')
    const [step, setStep] = useState(1)
    const [part, setPart] = useState(1)
    const [f, setF] = useState({})
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [busy, setBusy] = useState(false)

    const [slides, setSlides] = useState(FALLBACK)
    const [index, setIndex] = useState(0)

    useEffect(() => {
        getPublicSlides()
            .then((list) => {
                if (!Array.isArray(list) || list.length === 0) return
                setSlides(list.map((s, i) => ({
                    eyebrow: s.eyebrow, heading: s.heading, body: s.body,
                    image: s.hasImage ? slideImageUrl(s) : FALLBACK[i % FALLBACK.length].image,
                })))
                setIndex(0)
            })
            .catch(() => {})
    }, [])

    useEffect(() => {
        if (slides.length < 2 || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
        const timer = setTimeout(() => setIndex((i) => (i + 1) % slides.length), SLIDE_MS)
        return () => clearTimeout(timer)
    }, [index, slides.length])

    const bind = (name) => ({ value: f[name] || '', onChange: (e) => setF({ ...f, [name]: e.target.value }) })

    function go(next, message = null) {
        setView(next); setStep(1); setPart(1); setF({}); setError(null); setNotice(message)
    }

    async function submit(e) {
        e.preventDefault()

        if (view === 'signup' && compact && part === 1) { setPart(2); return }

        setError(null); setNotice(null); setBusy(true)
        try {
            if (view === 'signin') {
                await login(f.username, f.password)
                navigate('/')
            } else if (view === 'signup') {
                const result = await signup(f.idNumber, f.fullName, f.email, f.username, f.password)
                if (result.pendingApproval) go('signin', result.message)
                else navigate('/')
            } else if (step === 1) {
                const result = await requestPasswordReset(f.identifier)
                setStep(2)
                setNotice(result.message)
            } else {
                if (f.newPassword !== f.confirm) throw new Error('The two passwords do not match.')
                const result = await confirmPasswordReset(f.identifier, f.code, f.newPassword)
                go('signin', result.message)
            }
        } catch (err) {
            setError(explain(err))
        } finally { setBusy(false) }
    }

    async function resend() {
        setError(null); setNotice(null); setBusy(true)
        try {
            const result = await requestPasswordReset(f.identifier)
            setNotice(result.message)
        } catch (err) {
            setError(explain(err))
        } finally { setBusy(false) }
    }

    const titles = {
        signin: ['Welcome back', 'Sign in to your department workspace.'],
        signup: ['Create your account', 'Students use a matric number already on the roster.'],
        forgot: step === 1
            ? ['Reset your password', 'We will email a 6-digit code to the address on your account.']
            : ['Enter your code', 'Type the code we emailed you, then choose a new password.'],
    }
    const [title, subtitle] = titles[view]

    const lastPart = !(view === 'signup' && compact && part === 1)
    const buttonLabel = view === 'signin' ? 'Sign In'
        : view === 'signup' ? (lastPart ? 'Create Account' : 'Next')
            : step === 1 ? 'Send Code' : 'Reset Password'

    const cur = slides[index]
    const previous = () => setIndex((index - 1 + slides.length) % slides.length)
    const next = () => setIndex((index + 1) % slides.length)
    const link = { color: 'var(--accent)' }

    return (
        <div className="relative overflow-x-hidden flex items-center justify-center p-3 sm:p-6"
             style={{ minHeight: '100dvh', background: 'var(--bg-gradient), var(--bg)', color: 'var(--text)' }}>
            <style>{CSS}</style>
            <div className="csdt-blob" style={{ width: 340, height: 340, top: '-8%', left: '-6%', background: 'var(--accent)' }} />
            <div className="csdt-blob" style={{ width: 300, height: 300, bottom: '-10%', right: '-6%', background: 'var(--accent-2)', animationDelay: '-9s' }} />

            <div className="relative z-10 w-full max-w-5xl grid lg:grid-cols-[1.05fr_1fr] rounded-3xl overflow-hidden shadow-2xl border theme-border"
                 style={{ backgroundColor: 'var(--surface)' }}>

                {/* picture panel (computers) */}
                <aside className="hidden lg:block relative overflow-hidden text-white" style={{ minHeight: 540 }}>
                    <SlideLayers slides={slides} index={index} />
                    <div className="absolute inset-0" style={{ background: 'var(--accent)', opacity: 0.2 }} />
                    <div className="absolute inset-0" style={{ background: 'linear-gradient(to top, rgba(0,0,0,.78), rgba(0,0,0,.05) 65%)' }} />

                    <div className="absolute inset-x-0 bottom-0 p-8">
                        <div key={index} className="animate-in">
                            <span className="text-[11px] font-bold uppercase tracking-[0.2em] opacity-90">{cur.eyebrow}</span>
                            <h2 className="text-3xl font-extrabold leading-tight mt-2">{cur.heading}</h2>
                            {cur.body && <p className="text-sm text-white/85 mt-2 max-w-md">{cur.body}</p>}
                        </div>

                        <div className="flex flex-wrap gap-2 mt-4">
                            {FEATURES.map(([Icon, label]) => (
                                <span key={label} className="flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full border border-white/25" style={{ backgroundColor: 'rgba(255,255,255,.15)' }}>
                  <Icon size={13} /> {label}
                </span>
                            ))}
                        </div>

                        {slides.length > 1 && (
                            <div className="flex items-center justify-between mt-5">
                                <div className="flex gap-2">
                                    {slides.map((_, i) => (
                                        <button key={i} aria-label={`Picture ${i + 1}`} onClick={() => setIndex(i)}
                                                className="h-2 rounded-full border border-white transition-all" style={{ width: i === index ? 22 : 8, backgroundColor: i === index ? '#fff' : 'transparent' }} />
                                    ))}
                                </div>
                                <div className="flex gap-2">
                                    <button aria-label="Previous picture" onClick={previous} className="w-8 h-8 rounded-full flex items-center justify-center border border-white/30" style={{ backgroundColor: 'rgba(255,255,255,.15)' }}><ChevronLeft size={16} /></button>
                                    <button aria-label="Next picture" onClick={next} className="w-8 h-8 rounded-full flex items-center justify-center border border-white/30" style={{ backgroundColor: 'rgba(255,255,255,.15)' }}><ChevronRight size={16} /></button>
                                </div>
                            </div>
                        )}
                    </div>
                </aside>

                {/* form */}
                <section className="p-5 sm:p-8 flex flex-col">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-2.5">
                            <Logo className="w-10 h-10" />
                            <div>
                                <p className="font-bold leading-tight">CSDT-DAS</p>
                                <p className="text-[11px] sm:text-xs theme-text-muted leading-tight">Computing Science &amp; Digital Technology</p>
                            </div>
                        </div>
                        <ThemeSwitcher />
                    </div>

                    {/* picture banner (phones and tablets) */}
                    <div className="csdt-banner lg:hidden relative h-24 sm:h-32 rounded-2xl overflow-hidden mb-4 text-white">
                        <SlideLayers slides={slides} index={index} />
                        <div className="absolute inset-0" style={{ background: 'var(--accent)', opacity: 0.2 }} />
                        <div className="absolute inset-0" style={{ background: 'linear-gradient(to top, rgba(0,0,0,.72), rgba(0,0,0,.05) 70%)' }} />
                        <div className="absolute inset-x-0 bottom-0 p-3">
                            <p className="text-[10px] font-bold uppercase tracking-widest opacity-90">{cur.eyebrow}</p>
                            <p className="text-sm font-bold leading-tight">{cur.heading}</p>
                        </div>
                    </div>

                    <h1 className="text-xl sm:text-2xl font-bold leading-tight">{title}</h1>
                    <p className="hidden sm:block text-sm theme-text-muted mt-0.5 mb-4">{subtitle}</p>

                    {(notice || error) && (
                        <div className={`mt-3 mb-1 px-3 py-2 rounded-xl text-xs ${error ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-800'}`}>{error || notice}</div>
                    )}

                    <form onSubmit={submit} className="space-y-3 mt-3 sm:mt-0">

                        {view === 'signin' && (
                            <>
                                <Field label="Username" autoComplete="username" {...bind('username')} />
                                <Field label="Password" type="password" autoComplete="current-password" {...bind('password')} />
                            </>
                        )}

                        {view === 'signup' && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {(!compact || part === 1) && (
                                    <>
                                        <Field label="Matric number or Staff ID" placeholder="KDUCSC25086" {...bind('idNumber')} />
                                        <Field label="Full name" {...bind('fullName')} />
                                    </>
                                )}
                                {(!compact || part === 2) && (
                                    <>
                                        <Field label="Email" type="email" {...bind('email')} />
                                        <Field label="Username" {...bind('username')} />
                                        <div className="sm:col-span-2"><Field label="Password" type="password" autoComplete="new-password" {...bind('password')} /></div>
                                    </>
                                )}
                                {(!compact || part === 1) && (
                                    <p className="sm:col-span-2 text-[11px] leading-snug theme-text-muted">
                                        IDs starting with KDU must already be on the student roster. Staff accounts are approved by the H.O.D.
                                    </p>
                                )}
                            </div>
                        )}

                        {view === 'forgot' && step === 1 && <Field label="Matric number or username" {...bind('identifier')} />}
                        {view === 'forgot' && step === 2 && (
                            <>
                                <Field label="6-digit code" inputMode="numeric" maxLength={6} {...bind('code')} />
                                <Field label="New password" type="password" autoComplete="new-password" {...bind('newPassword')} />
                                <Field label="Confirm new password" type="password" autoComplete="new-password" {...bind('confirm')} />
                            </>
                        )}

                        <button type="submit" disabled={busy} className="w-full h-11 rounded-xl text-sm font-semibold text-white accent-gradient btn-press disabled:opacity-50">
                            {busy ? 'Please wait…' : buttonLabel}
                        </button>
                    </form>

                    <div className="mt-3 flex justify-between gap-3 text-sm font-medium" style={link}>
                        {view === 'signin' && (<><button onClick={() => go('forgot')}>Forgot password?</button><button onClick={() => go('signup')}>Create account</button></>)}
                        {view === 'signup' && (
                            <>
                                {compact && part === 2 ? <button onClick={() => setPart(1)}>← Back</button> : <span />}
                                <button onClick={() => go('signin')}>Already registered? Sign in</button>
                            </>
                        )}
                        {view === 'forgot' && (
                            <>
                                {step === 2 ? <button onClick={resend} disabled={busy}>Resend code</button> : <span />}
                                <button onClick={() => go('signin')}>Back to sign in</button>
                            </>
                        )}
                    </div>

                    <div className="mt-auto pt-4">
                        <div className="pt-3 border-t theme-border flex justify-between items-center gap-3 text-[11px] theme-text-muted">
                            <span><span className="sm:hidden">Kola Daisi University</span><span className="hidden sm:inline">Kola Daisi University · Ibadan, Nigeria</span></span>
                            <a href={SIWES_URL} target="_blank" rel="noopener noreferrer" className="flex items-center gap-1 font-medium" style={link}>
                                SIWES <ExternalLink size={11} />
                            </a>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    )
}

export default Home