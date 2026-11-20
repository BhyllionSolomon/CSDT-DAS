import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertCircle, Info, CheckCircle2, ChevronRight, Download, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getToday } from '../services/todayService'

const TONE = {
    warn: { icon: AlertCircle, color: '#d97706', bg: 'rgba(245,158,11,.14)' },
    info: { icon: Info, color: 'var(--accent)', bg: 'color-mix(in srgb, var(--accent) 14%, transparent)' },
    ok: { icon: CheckCircle2, color: '#16a34a', bg: 'rgba(22,163,74,.14)' },
}

// Lets people put the app on their phone's home screen.
function InstallChip() {
    const [event, setEvent] = useState(null)
    const [ios, setIos] = useState(false)
    const [hidden, setHidden] = useState(() => localStorage.getItem('csdt-install-hidden') === '1')

    useEffect(() => {
        const handler = (e) => { e.preventDefault(); setEvent(e) }
        window.addEventListener('beforeinstallprompt', handler)
        setIos(/iphone|ipad|ipod/i.test(navigator.userAgent))
        return () => window.removeEventListener('beforeinstallprompt', handler)
    }, [])

    const installed = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone
    if (installed || hidden) return null

    const hide = () => { localStorage.setItem('csdt-install-hidden', '1'); setHidden(true) }

    if (event) {
        return (
            <button onClick={async () => { event.prompt(); await event.userChoice; setEvent(null) }}
                    className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full accent-gradient text-white btn-press">
                <Download size={13} /> Install app
            </button>
        )
    }
    if (ios) {
        return (
            <span className="flex items-center gap-2 text-xs px-3 py-1.5 rounded-full theme-text-muted border theme-border">
        On iPhone: tap Share, then Add to Home Screen
        <button onClick={hide} aria-label="Hide"><X size={12} /></button>
      </span>
        )
    }
    return null
}

function TodayPanel() {
    const { user } = useAuth()
    const [data, setData] = useState(null)
    const [failed, setFailed] = useState(false)

    useEffect(() => {
        getToday().then(setData).catch(() => setFailed(true))
    }, [])

    if (failed) return null

    const hour = new Date().getHours()
    const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'
    const first = (user?.fullName || '').split(' ')[0]
    const today = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })

    return (
        <section className="mb-8">
            <div className="flex justify-between items-start gap-3 flex-wrap mb-4">
                <div>
                    <h2 className="text-xl sm:text-2xl font-bold leading-tight">{greeting}, {first}</h2>
                    <p className="text-sm theme-text-muted">
                        {today}{data?.sessionName ? ` · ${data.sessionName} · ${data.semester === 'FIRST' ? 'First' : 'Second'} semester` : ''}
                    </p>
                </div>
                <InstallChip />
            </div>

            {!data ? (
                <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
                    {[0, 1, 2].map((i) => <div key={i} className="skeleton h-16 rounded-2xl" />)}
                </div>
            ) : (
                <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
                    {data.items.map((item) => {
                        const t = TONE[item.tone] || TONE.info
                        const Icon = t.icon
                        const body = (
                            <>
                <span className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style={{ background: t.bg, color: t.color }}>
                  <Icon size={18} />
                </span>
                                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-semibold leading-snug">{item.title}</span>
                                    {item.detail && <span className="block text-xs theme-text-muted mt-0.5">{item.detail}</span>}
                </span>
                                {item.link && <ChevronRight size={16} className="theme-text-muted mt-2 shrink-0" />}
                            </>
                        )
                        return item.link
                            ? <Link key={item.key} to={item.link} className="card card-hover p-3.5 flex items-start gap-3">{body}</Link>
                            : <div key={item.key} className="card p-3.5 flex items-start gap-3">{body}</div>
                    })}
                </div>
            )}
        </section>
    )
}

export default TodayPanel