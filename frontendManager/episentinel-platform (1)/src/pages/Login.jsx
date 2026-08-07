import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShieldHalf, Eye, EyeOff, Moon, Sun, ArrowRight, Activity, Radar, FlaskConical } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

export default function Login() {
  const { login } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [remember, setRemember] = useState(true)
  const [loading, setLoading] = useState(false)

  const isDark = theme !== 'light'

  const submit = (e) => {
    e.preventDefault()
    setLoading(true)
    setTimeout(() => {
      login(username || 'A. Balogun')
      navigate('/')
    }, 700)
  }

  return (
    <div className="min-h-screen w-full flex bg-bg overflow-hidden">
      {/* Left — animated brand panel */}
      <div className="hidden lg:flex w-1/2 relative overflow-hidden bg-bgSunken">
        <div className="absolute inset-0">
          {[...Array(3)].map((_, i) => (
            <div
              key={i}
              className="absolute rounded-full opacity-20 animate-pulseSoft"
              style={{
                width: 400 + i * 160,
                height: 400 + i * 160,
                left: `${-10 + i * 8}%`,
                top: `${-10 + i * 18}%`,
                background: 'radial-gradient(circle, rgb(var(--c-accent-2)) 0%, transparent 70%)',
                animationDelay: `${i * 0.6}s`,
                animationDuration: `${4 + i}s`,
              }}
            />
          ))}
        </div>

        <svg className="absolute inset-0 w-full h-full opacity-40" viewBox="0 0 800 800" preserveAspectRatio="none">
          <path d="M0 620 Q 100 560 200 600 T 400 560 T 600 600 T 800 540" fill="none" stroke="rgb(var(--c-accent-2))" strokeWidth="2" className="animate-curve" style={{ strokeDasharray: 1600 }} />
          <path d="M0 680 Q 100 640 200 660 T 400 630 T 600 660 T 800 610" fill="none" stroke="rgb(var(--c-accent))" strokeWidth="2" opacity="0.6" className="animate-curve" style={{ strokeDasharray: 1600, animationDelay: '0.3s' }} />
        </svg>

        <div className="relative z-10 flex flex-col justify-between p-14 text-textPrimary w-full">
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 rounded-xl bg-gradient-to-br from-accent to-accent2 flex items-center justify-center shadow-glow">
              <ShieldHalf size={22} className="text-white" strokeWidth={2.5} />
            </div>
            <div>
              <div className="font-display font-bold text-lg leading-tight">EpiSentinel</div>
              <div className="text-xs text-textMuted uppercase tracking-wider">National Surveillance Platform</div>
            </div>
          </div>

          <div className="max-w-md">
            <h1 className="font-display text-4xl font-bold leading-tight tracking-tight mb-4">
              AI-driven forecasting for a healthier nation.
            </h1>
            <p className="text-textSecondary text-[15px] leading-relaxed">
              Real-time epidemiological surveillance, outbreak detection, and predictive
              intelligence — built for NCDC, ministries of health, and frontline
              disease-control officers.
            </p>
            <div className="grid grid-cols-3 gap-4 mt-8">
              {[
                { icon: Activity, label: '2.3M+', sub: 'Records processed' },
                { icon: Radar, label: '99.2%', sub: 'Uptime SLA' },
                { icon: FlaskConical, label: '91.4%', sub: 'Model accuracy' },
              ].map((s, i) => (
                <div key={i} className="bg-surface/60 backdrop-blur border border-borderc rounded-xl p-3.5">
                  <s.icon size={16} className="text-accent2 mb-2" />
                  <div className="font-mono text-lg font-semibold">{s.label}</div>
                  <div className="text-[11px] text-textMuted">{s.sub}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="text-xs text-textMuted">© 2026 Nigeria Centre for Disease Control · Federal Ministry of Health</div>
        </div>
      </div>

      {/* Right — login form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-10 relative">
        <button
          onClick={() => setTheme(isDark ? 'light' : 'blue')}
          className="absolute top-6 right-6 focus-ring flex items-center gap-2 rounded-full border border-borderc bg-surface px-3.5 py-2 text-xs font-medium text-textSecondary hover:bg-surfaceHover transition-colors"
        >
          {isDark ? <Sun size={14} /> : <Moon size={14} />}
          {isDark ? 'Light mode' : 'Dark mode'}
        </button>

        <div className="w-full max-w-sm animate-riseIn">
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-accent to-accent2 flex items-center justify-center">
              <ShieldHalf size={18} className="text-white" />
            </div>
            <div className="font-display font-bold text-textPrimary">EpiSentinel</div>
          </div>

          <h2 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Welcome back</h2>
          <p className="text-textMuted text-sm mt-1.5 mb-8">Sign in to access the national surveillance dashboard.</p>

          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-textSecondary mb-1.5">Username or Officer ID</label>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                type="text"
                required
                placeholder="e.g. NCDC-0421"
                className="w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary placeholder:text-textMuted focus-ring focus:border-accent2/60 transition-colors"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-textSecondary">Password</label>
                <button type="button" className="text-xs text-accent hover:underline">Forgot password?</button>
              </div>
              <div className="relative">
                <input
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  type={showPw ? 'text' : 'password'}
                  required
                  placeholder="••••••••••••"
                  className="w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary placeholder:text-textMuted focus-ring focus:border-accent2/60 transition-colors pr-10"
                />
                <button type="button" onClick={() => setShowPw((s) => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-textMuted hover:text-textPrimary">
                  {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <label className="flex items-center gap-2 text-xs text-textSecondary cursor-pointer select-none">
              <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} className="accent-accent h-3.5 w-3.5 rounded" />
              Remember me on this device
            </label>

            <button
              type="submit"
              disabled={loading}
              className="focus-ring w-full flex items-center justify-center gap-2 rounded-lg bg-accent text-white py-3 text-sm font-semibold hover:bg-accent/90 transition-all active:scale-[0.99] disabled:opacity-70 shadow-sm mt-2"
            >
              {loading ? (
                <span className="h-4 w-4 rounded-full border-2 border-white/40 border-t-white animate-spin" />
              ) : (
                <>Sign in <ArrowRight size={15} /></>
              )}
            </button>
          </form>

          <p className="text-[11px] text-textMuted text-center mt-8 leading-relaxed">
            This system is restricted to authorized personnel of NCDC and affiliated
            ministries of health. Unauthorized access is prohibited and monitored.
          </p>
        </div>
      </div>
    </div>
  )
}
