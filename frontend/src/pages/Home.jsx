import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, ExternalLink, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { requestPasswordReset, confirmPasswordReset } from '../services/authService'
import { API_URL } from '../services/api'
import { getPublicSlides, slideImageUrl } from '../services/homeService'

const C = { maroon: '#681329', dark: '#4b0b1d', gold: '#c9a45c' }
const SIWES_URL = 'https://ems-frontend-fv32.onrender.com'
const SLIDE_MS = 6000

// Shown until the department uploads its own pictures, and for any slide without one.
const FALLBACK = [
    { eyebrow: 'Welcome to CSDT', heading: 'Shaping the Future Through Technology',
        body: "Discover a department committed to developing the knowledge, skills and creativity needed to solve tomorrow's digital challenges.",
        sideHeading: 'Learn. Innovate. Create.', sideText: 'Explore opportunities in computing, software engineering, information technology and cybersecurity.',
        image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1600&q=80' },
    { eyebrow: 'Your Future Starts Here', heading: 'Prepare for a Digital World',
        body: 'Build practical skills, develop critical thinking and prepare for meaningful careers in the global technology industry.',
        sideHeading: 'Education Beyond the Classroom', sideText: 'Learn through practical projects, collaboration, research and industry-focused experiences.',
        image: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1600&q=80' },
    { eyebrow: 'Research and Innovation', heading: 'Creating Solutions That Matter',
        body: 'From artificial intelligence to cybersecurity, our discipline offers opportunities to explore ideas and develop technology for real-world needs.',
        sideHeading: 'Ideas Into Impact', sideText: 'Join a learning community where curiosity, research and innovation drive progress.',
        image: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=1600&q=80' },
]

const PROGRAMMES = [
    ['Computer Science', 'The theory and practice of computing: algorithms, systems and intelligent software.'],
    ['Software Engineering', 'Designing, building and maintaining reliable software systems.'],
    ['Cybersecurity', 'Protecting systems, networks and data from digital threats.'],
    ['Information Technology', 'Applying computing to solve the everyday problems of organisations and people.'],
]

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
            <label className="block text-xs font-semibold mb-1" style={{ color: '#444' }}>{label}</label>
            <input {...props} required
                   className="w-full h-10 border border-slate-300 rounded-md px-3 text-sm bg-white placeholder:text-slate-400 focus:outline-none focus:ring-2"
                   style={{ color: '#252525', '--tw-ring-color': C.maroon }} />
            {hint && <p className="text-[11px] leading-snug mt-1" style={{ color: '#6b7280' }}>{hint}</p>}
        </div>
    )
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
    const [showInfo, setShowInfo] = useState(false)

    const [slides, setSlides] = useState(FALLBACK)
    const [index, setIndex] = useState(0)

    useEffect(() => {
        getPublicSlides()
            .then((list) => {
                if (!Array.isArray(list) || list.length === 0) return
                setSlides(list.map((s, i) => ({
                    eyebrow: s.eyebrow, heading: s.heading, body: s.body, sideHeading: s.sideHeading, sideText: s.sideText,
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
        signin: ['Welcome back', 'Sign in to continue'],
        signup: ['Create your account', 'Students use a matric number already on the roster'],
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
    const navItem = (active) => ({ backgroundColor: active ? C.maroon : 'transparent', color: active ? '#fff' : '#252525' })

    return (
        <div className="flex flex-col overflow-hidden bg-white" style={{ height: '100dvh', color: '#252525' }}>

            {/* top strip */}
            <div className="shrink-0 flex items-center justify-between px-[5%] h-7 sm:h-9 text-[10px] sm:text-xs text-white" style={{ backgroundColor: C.dark }}>
                <span>Kola Daisi University · Ibadan, Nigeria</span>
                <span>Departmental Academic System</span>
            </div>

            {/* header */}
            <header className="shrink-0 flex items-center justify-center gap-3 sm:gap-5 px-4 py-2 sm:py-3">
                <div className="shrink-0 flex items-center justify-center rounded-full font-bold w-11 h-11 sm:w-16 sm:h-16 text-sm sm:text-xl"
                     style={{ border: `3px solid ${C.maroon}`, color: C.maroon }}>CSDT</div>
                <div className="text-center">
                    <h1 className="font-extrabold leading-tight text-sm sm:text-2xl lg:text-3xl">Computing Science &amp; Digital Technology</h1>
                    <p className="hidden sm:block mt-1 text-sm font-bold" style={{ color: C.maroon }}>Innovation, Knowledge and Digital Transformation</p>
                </div>
            </header>

            {/* navigation */}
            <nav className="shrink-0 grid grid-cols-4 border-y border-slate-200 text-center text-[11px] sm:text-sm font-bold">
                <button onClick={() => go('signin')} className="py-2.5 sm:py-3.5 border-r border-slate-200" style={navItem(view === 'signin')}>Sign In</button>
                <button onClick={() => go('signup')} className="py-2.5 sm:py-3.5 border-r border-slate-200" style={navItem(view === 'signup')}>Create Account</button>
                <button onClick={() => setShowInfo(true)} className="py-2.5 sm:py-3.5 border-r border-slate-200 hover:bg-rose-50">Our Programmes</button>
                <a href={SIWES_URL} target="_blank" rel="noopener noreferrer" className="py-2.5 sm:py-3.5 flex items-center justify-center gap-1 hover:bg-rose-50">
                    SIWES <ExternalLink size={12} />
                </a>
            </nav>

            {/* hero */}
            <main className="relative flex-1 min-h-0 overflow-hidden text-white" style={{ backgroundColor: C.dark }}>
                {slides.map((s, i) => (
                    <div key={i} aria-hidden={i !== index} className="absolute inset-0"
                         style={{
                             opacity: i === index ? 1 : 0, transition: 'opacity 1.8s ease-in-out',
                             backgroundImage: `linear-gradient(90deg, rgba(0,0,0,.72), rgba(0,0,0,.28) 75%), url("${s.image}")`,
                             backgroundSize: 'cover', backgroundPosition: 'center',
                         }} />
                ))}

                <div className="relative z-10 h-full grid items-center content-center gap-3 px-4 sm:px-8 lg:px-[5%] lg:grid-cols-[minmax(0,1fr)_400px]">

                    <div className="max-w-xl animate-in" key={index}>
                        <span className="block font-bold uppercase text-[10px] sm:text-xs tracking-[0.2em] mb-1 sm:mb-3" style={{ color: C.gold }}>{cur.eyebrow}</span>
                        <h2 className={`font-extrabold leading-tight text-lg sm:text-3xl lg:text-5xl mb-2 sm:mb-4 ${compact && view !== 'signin' ? 'hidden' : ''}`}>{cur.heading}</h2>
                        {cur.body && <p className="hidden md:block text-sm lg:text-base leading-relaxed text-white/90 max-w-md">{cur.body}</p>}
                        {cur.sideHeading && (
                            <div className="hidden xl:block mt-5 pl-4 max-w-sm" style={{ borderLeft: `4px solid ${C.gold}` }}>
                                <p className="font-bold">{cur.sideHeading}</p>
                                {cur.sideText && <p className="text-sm text-white/80 mt-1">{cur.sideText}</p>}
                            </div>
                        )}
                    </div>

                    {/* sign in, sign up, forgot password */}
                    <div className="w-full max-w-[400px] justify-self-center lg:justify-self-end rounded-lg shadow-2xl bg-white/95 backdrop-blur max-h-full overflow-y-auto p-4 sm:p-6"
                         style={{ borderTop: `4px solid ${C.gold}`, color: '#252525' }}>

                        <h3 className="font-bold text-base sm:text-xl leading-tight">{title}</h3>
                        <p className="hidden sm:block text-xs mt-1 mb-3" style={{ color: '#6b7280' }}>{subtitle}</p>

                        {(notice || error) && (
                            <div className={`mt-2 mb-2 px-3 py-2 rounded-md text-xs ${error ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-800'}`}>{error || notice}</div>
                        )}

                        <form onSubmit={submit} className="space-y-2.5 mt-2 sm:mt-0">

                            {view === 'signin' && (
                                <>
                                    <Field label="Username" autoComplete="username" {...bind('username')} />
                                    <Field label="Password" type="password" autoComplete="current-password" {...bind('password')} />
                                </>
                            )}

                            {view === 'signup' && (
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
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
                                        <p className="sm:col-span-2 text-[11px] leading-snug" style={{ color: '#6b7280' }}>
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

                            <button type="submit" disabled={busy} className="w-full h-10 rounded-md text-sm font-bold text-white disabled:opacity-50 hover:brightness-110"
                                    style={{ backgroundColor: C.maroon }}>
                                {busy ? 'Please wait…' : buttonLabel}
                            </button>
                        </form>

                        <div className="mt-3 flex justify-between gap-3 text-xs font-semibold" style={{ color: C.maroon }}>
                            {view === 'signin' && (<><button onClick={() => go('forgot')}>Forgot password?</button><button onClick={() => go('signup')}>Create account</button></>)}
                            {view === 'signup' && (
                                <>
                                    {compact && part === 2 ? <button onClick={() => setPart(1)}>← Back</button> : <span />}
                                    <button onClick={() => go('signin')}>Already have an account? Sign in</button>
                                </>
                            )}
                            {view === 'forgot' && (
                                <>
                                    {step === 2 ? <button onClick={resend} disabled={busy}>Resend code</button> : <span />}
                                    <button onClick={() => go('signin')}>Back to sign in</button>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                {/* slider controls */}
                {slides.length > 1 && (
                    <>
                        <button aria-label="Previous slide" onClick={() => setIndex((index - 1 + slides.length) % slides.length)}
                                className="hidden md:flex absolute z-20 left-0 top-1/2 -translate-y-1/2 w-11 h-14 items-center justify-center text-white hover:brightness-125"
                                style={{ backgroundColor: C.maroon }}><ChevronLeft size={26} /></button>
                        <button aria-label="Next slide" onClick={() => setIndex((index + 1) % slides.length)}
                                className="hidden md:flex absolute z-20 right-0 top-1/2 -translate-y-1/2 w-11 h-14 items-center justify-center text-white hover:brightness-125"
                                style={{ backgroundColor: C.maroon }}><ChevronRight size={26} /></button>
                        <div className="absolute z-20 bottom-2 sm:bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                            {slides.map((_, i) => (
                                <button key={i} aria-label={`Slide ${i + 1}`} onClick={() => setIndex(i)}
                                        className="w-2.5 h-2.5 rounded-full border-2 border-white" style={{ backgroundColor: i === index ? '#fff' : 'transparent' }} />
                            ))}
                        </div>
                    </>
                )}
            </main>

            {/* footer */}
            <footer className="shrink-0 text-center text-white text-[10px] sm:text-xs py-1.5 px-2" style={{ backgroundColor: C.dark }}>
                © {new Date().getFullYear()} Department of Computing Science and Digital Technology · Kola Daisi University, Ibadan, Nigeria
            </footer>

            {/* programmes */}
            {showInfo && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(0,0,0,.65)' }} onClick={() => setShowInfo(false)}>
                    <div className="relative w-full max-w-2xl rounded-lg bg-white p-5 sm:p-7 max-h-full overflow-y-auto" style={{ borderTop: `4px solid ${C.gold}` }} onClick={(e) => e.stopPropagation()}>
                        <button aria-label="Close" onClick={() => setShowInfo(false)} className="absolute top-3 right-3" style={{ color: C.maroon }}><X size={20} /></button>
                        <p className="text-[11px] font-bold uppercase tracking-[0.2em]" style={{ color: C.maroon }}>About the department</p>
                        <h3 className="text-lg sm:text-2xl font-extrabold mt-1 mb-2">Our Programmes</h3>
                        <p className="text-sm mb-4" style={{ color: '#555' }}>
                            The Department of Computing Science and Digital Technology provides an environment for learning, research and innovation across computing-related disciplines.
                        </p>
                        <div className="grid sm:grid-cols-2 gap-3">
                            {PROGRAMMES.map(([name, text]) => (
                                <div key={name} className="rounded-md border border-slate-200 p-3">
                                    <p className="font-bold text-sm" style={{ color: C.maroon }}>{name}</p>
                                    <p className="text-xs mt-1" style={{ color: '#555' }}>{text}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default Home