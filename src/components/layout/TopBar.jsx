import { useState, useRef, useEffect } from 'react'
import { Search, Bell, Menu, ChevronDown, Palette, Check, Activity, Flame, Cpu, Database, CheckCircle2 } from 'lucide-react'
import { useTheme, THEMES } from '../../context/ThemeContext'
import { useAuth } from '../../context/AuthContext'
import { NOTIFICATIONS } from '../../data/mockData'
import Breadcrumbs from './Breadcrumbs'

const NOTIF_ICON = { forecast: Activity, outbreak: Flame, model: Cpu, data: Database, validation: CheckCircle2 }

function useClickOutside(ref, onOutside) {
  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) onOutside() }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [ref, onOutside])
}

export default function TopBar({ onMenuClick }) {
  const { theme, setTheme } = useTheme()
  const { user } = useAuth()
  const [openThemes, setOpenThemes] = useState(false)
  const [openNotifs, setOpenNotifs] = useState(false)
  const [openProfile, setOpenProfile] = useState(false)

  const themeRef = useRef(); const notifRef = useRef(); const profileRef = useRef()
  useClickOutside(themeRef, () => setOpenThemes(false))
  useClickOutside(notifRef, () => setOpenNotifs(false))
  useClickOutside(profileRef, () => setOpenProfile(false))

  return (
    <header className="sticky top-0 z-30 h-16 shrink-0 flex items-center gap-4 px-4 md:px-6 bg-surface/80 backdrop-blur-md border-b border-borderc">
      <button onClick={onMenuClick} className="md:hidden text-textSecondary focus-ring rounded-md p-1">
        <Menu size={20} />
      </button>

      <div className="hidden md:block"><Breadcrumbs /></div>

      <div className="flex-1 max-w-md ml-0 md:ml-4">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-textMuted" />
          <input
            type="text"
            placeholder="Search cases, LGAs, diseases, reports…"
            className="w-full bg-bgSunken border border-borderc rounded-lg pl-9 pr-3 py-2 text-[13px] text-textPrimary placeholder:text-textMuted focus-ring focus:border-accent2/50 transition-colors"
          />
          <kbd className="hidden lg:inline-block absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-textMuted border border-borderc rounded px-1.5 py-0.5">⌘K</kbd>
        </div>
      </div>

      <div className="flex items-center gap-1.5 md:gap-2 ml-auto">
        {/* Theme switcher */}
        <div className="relative" ref={themeRef}>
          <button
            onClick={() => setOpenThemes((o) => !o)}
            className="focus-ring flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-textSecondary hover:bg-surfaceHover transition-colors"
          >
            <Palette size={17} />
            <ChevronDown size={13} className={`transition-transform ${openThemes ? 'rotate-180' : ''}`} />
          </button>
          {openThemes && (
            <div className="absolute right-0 mt-2 w-56 bg-surface border border-borderc rounded-xl shadow-elevated p-1.5 animate-riseIn z-40">
              {THEMES.map((t) => (
                <button
                  key={t.id}
                  onClick={() => { setTheme(t.id); setOpenThemes(false) }}
                  className="w-full flex items-center gap-3 rounded-lg px-3 py-2 text-[13px] text-textPrimary hover:bg-surfaceHover transition-colors"
                >
                  <span className="h-4 w-4 rounded-full border border-borderc shrink-0" style={{ background: t.swatch }} />
                  <span className="flex-1 text-left">{t.label}</span>
                  {theme === t.id && <Check size={14} className="text-accent" />}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Notifications */}
        <div className="relative" ref={notifRef}>
          <button
            onClick={() => setOpenNotifs((o) => !o)}
            className="focus-ring relative rounded-lg p-2 text-textSecondary hover:bg-surfaceHover transition-colors"
          >
            <Bell size={17} />
            <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-danger ring-2 ring-surface animate-pulseSoft" />
          </button>
          {openNotifs && (
            <div className="absolute right-0 mt-2 w-80 bg-surface border border-borderc rounded-xl shadow-elevated animate-riseIn z-40 overflow-hidden">
              <div className="px-4 py-3 border-b border-borderc flex items-center justify-between">
                <span className="font-display font-semibold text-sm">Notifications</span>
                <span className="text-[11px] text-accent font-medium">{NOTIFICATIONS.length} new</span>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {NOTIFICATIONS.map((n) => {
                  const Icon = NOTIF_ICON[n.type] || Activity
                  return (
                    <div key={n.id} className="flex gap-3 px-4 py-3 hover:bg-surfaceHover transition-colors border-b border-borderc last:border-0">
                      <div className="h-8 w-8 rounded-lg bg-accent/10 text-accent flex items-center justify-center shrink-0"><Icon size={15} /></div>
                      <div className="min-w-0">
                        <div className="text-[13px] font-medium text-textPrimary">{n.title}</div>
                        <div className="text-xs text-textMuted truncate">{n.desc}</div>
                        <div className="text-[10.5px] text-textMuted mt-0.5">{n.time}</div>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </div>

        {/* Profile */}
        <div className="relative pl-1.5 ml-1 border-l border-borderc" ref={profileRef}>
          <button onClick={() => setOpenProfile((o) => !o)} className="focus-ring flex items-center gap-2.5 rounded-lg pl-2 pr-1.5 py-1.5 hover:bg-surfaceHover transition-colors">
            <div className="h-8 w-8 rounded-full bg-gradient-to-br from-accent to-accent2 flex items-center justify-center text-white text-xs font-semibold shrink-0">
              {user?.initials || 'AB'}
            </div>
            <div className="hidden lg:block text-left leading-tight">
              <div className="text-[12.5px] font-medium text-textPrimary">{user?.name || 'Dr. A. Balogun'}</div>
              <div className="text-[10.5px] text-textMuted">{user?.role || 'Surveillance Officer'}</div>
            </div>
            <ChevronDown size={13} className="hidden lg:block text-textMuted" />
          </button>
          {openProfile && (
            <div className="absolute right-0 mt-2 w-56 bg-surface border border-borderc rounded-xl shadow-elevated p-1.5 animate-riseIn z-40">
              <div className="px-3 py-2 border-b border-borderc mb-1">
                <div className="text-[13px] font-medium text-textPrimary">{user?.name}</div>
                <div className="text-[11px] text-textMuted">{user?.org}</div>
              </div>
              {['My Profile', 'Preferences', 'Help & Support'].map((it) => (
                <button key={it} className="w-full text-left rounded-lg px-3 py-2 text-[13px] text-textSecondary hover:bg-surfaceHover hover:text-textPrimary transition-colors">{it}</button>
              ))}
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
