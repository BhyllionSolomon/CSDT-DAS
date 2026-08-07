import { useState } from 'react'
import { Palette, Globe2, Bell, Building2, Cpu, LineChart, Check } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Button from '../components/ui/Button'
import { useTheme, THEMES } from '../context/ThemeContext'

const inputCls = 'w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring focus:border-accent2/60'

const TABS = [
  { id: 'theme', label: 'Theme', icon: Palette },
  { id: 'lang', label: 'Language', icon: Globe2 },
  { id: 'notif', label: 'Notifications', icon: Bell },
  { id: 'org', label: 'Organization', icon: Building2 },
  { id: 'model', label: 'AI Model', icon: Cpu },
  { id: 'forecast', label: 'Forecast Defaults', icon: LineChart },
]

export default function Settings() {
  const [tab, setTab] = useState('theme')
  const { theme, setTheme } = useTheme()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Settings</h1>
        <p className="text-sm text-textMuted mt-1">Configure your platform preferences</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-5">
        <Card className="p-3 h-fit lg:col-span-1">
          {TABS.map((t) => (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={`w-full flex items-center gap-3 rounded-lg px-3 py-2.5 text-[13px] font-medium transition-colors ${tab === t.id ? 'bg-accent/12 text-accent' : 'text-textSecondary hover:bg-surfaceHover'}`}
            >
              <t.icon size={16} /> {t.label}
            </button>
          ))}
        </Card>

        <Card className="lg:col-span-3">
          {tab === 'theme' && (
            <>
              <CardHeader title="Interface Theme" subtitle="Applies instantly and is remembered for future visits" />
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 p-5">
                {THEMES.map((t) => (
                  <button
                    key={t.id}
                    onClick={() => setTheme(t.id)}
                    className={`relative rounded-xl border p-4 text-left transition-all duration-200 ${theme === t.id ? 'border-accent2 shadow-glow' : 'border-borderc hover:border-accent2/40'}`}
                  >
                    <div className="h-16 rounded-lg mb-3" style={{ background: `linear-gradient(135deg, ${t.swatch}33, ${t.swatch}CC)` }} />
                    <div className="text-sm font-medium text-textPrimary">{t.label}</div>
                    {theme === t.id && <Check size={16} className="absolute top-3 right-3 text-accent2" />}
                  </button>
                ))}
              </div>
            </>
          )}

          {tab === 'lang' && (
            <>
              <CardHeader title="Language & Region" />
              <div className="grid sm:grid-cols-2 gap-4 p-5">
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Display Language</label>
                  <select className={inputCls}><option>English (Nigeria)</option><option>Hausa</option><option>Yoruba</option><option>Igbo</option><option>French</option></select>
                </div>
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Date Format</label>
                  <select className={inputCls}><option>DD/MM/YYYY</option><option>MM/DD/YYYY</option><option>YYYY-MM-DD</option></select>
                </div>
              </div>
            </>
          )}

          {tab === 'notif' && (
            <>
              <CardHeader title="Notification Preferences" />
              <div className="p-5 space-y-3">
                {['Outbreak alerts', 'Forecast completion', 'Model training updates', 'Dataset validation results', 'Weekly summary digest'].map((n) => (
                  <label key={n} className="flex items-center justify-between bg-bgSunken border border-borderc rounded-lg px-4 py-3 cursor-pointer">
                    <span className="text-sm text-textPrimary">{n}</span>
                    <input type="checkbox" defaultChecked className="accent-accent h-4 w-4 rounded" />
                  </label>
                ))}
              </div>
            </>
          )}

          {tab === 'org' && (
            <>
              <CardHeader title="Organization" />
              <div className="grid sm:grid-cols-2 gap-4 p-5">
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Organization Name</label><input className={inputCls} defaultValue="Nigeria Centre for Disease Control" /></div>
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Region</label><input className={inputCls} defaultValue="South West Zone" /></div>
              </div>
            </>
          )}

          {tab === 'model' && (
            <>
              <CardHeader title="AI Model Configuration" />
              <div className="grid sm:grid-cols-2 gap-4 p-5">
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Active Model</label>
                  <select className={inputCls}><option>DCTMN-v3.2 (TCN + Bi-LSTM + Transformer)</option><option>DCTMN-v3.1</option></select>
                </div>
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Confidence Threshold</label><input type="number" defaultValue={85} className={inputCls} /></div>
              </div>
            </>
          )}

          {tab === 'forecast' && (
            <>
              <CardHeader title="Forecast Defaults" />
              <div className="grid sm:grid-cols-2 gap-4 p-5">
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Default Horizon</label>
                  <select className={inputCls}><option>4 weeks</option><option>8 weeks</option><option>12 weeks</option></select>
                </div>
                <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Default Confidence Interval</label>
                  <select className={inputCls}><option>90%</option><option>95%</option><option>99%</option></select>
                </div>
              </div>
            </>
          )}

          <div className="flex justify-end gap-2 border-t border-borderc px-5 py-4">
            <Button variant="secondary" size="sm">Reset to Defaults</Button>
            <Button variant="primary" size="sm">Save Changes</Button>
          </div>
        </Card>
      </div>
    </div>
  )
}
