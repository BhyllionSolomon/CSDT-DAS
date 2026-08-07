import { useState } from 'react'
import {
  Save, CheckCircle2, UploadCloud, FileSpreadsheet, Sparkles,
  ClipboardList, Users, FlaskConical, AlertTriangle, CloudSun,
} from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Button from '../components/ui/Button'
import Badge from '../components/ui/Badge'
import { DISEASES, STATES, LGAS_BY_STATE } from '../data/mockData'

function Field({ label, children, span = 1 }) {
  return (
    <div className={span === 2 ? 'sm:col-span-2' : ''}>
      <label className="block text-xs font-semibold text-textSecondary mb-1.5">{label}</label>
      {children}
    </div>
  )
}

const inputCls = 'w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring focus:border-accent2/60 transition-colors'

function NumberInput({ value, onChange, placeholder = '0' }) {
  return <input type="number" min="0" value={value} onChange={(e) => onChange(+e.target.value)} placeholder={placeholder} className={inputCls} />
}

function Toggle({ checked, onChange, label }) {
  return (
    <label className="flex items-center justify-between gap-3 bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 cursor-pointer select-none">
      <span className="text-sm text-textPrimary">{label}</span>
      <span
        onClick={(e) => { e.preventDefault(); onChange(!checked) }}
        className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors duration-200 ${checked ? 'bg-accent' : 'bg-borderc'}`}
      >
        <span className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform duration-200 ${checked ? 'translate-x-4.5' : 'translate-x-1'}`} style={{ transform: checked ? 'translateX(18px)' : 'translateX(2px)' }} />
      </span>
    </label>
  )
}

const SECTIONS = [
  { id: 'general', label: 'General Information', icon: ClipboardList },
  { id: 'patients', label: 'Patient Statistics', icon: Users },
  { id: 'lab', label: 'Laboratory', icon: FlaskConical },
  { id: 'risk', label: 'Risk Factors', icon: AlertTriangle },
  { id: 'env', label: 'Environmental Variables', icon: CloudSun },
]

export default function DailySurveillance() {
  const [state, setState] = useState('Lagos')
  const [step, setStep] = useState(0)
  const [validated, setValidated] = useState(false)
  const [risk, setRisk] = useState({ flooding: false, water: false, displacement: false, sanitation: true, conflict: false, movement: false })
  const [stats, setStats] = useState({ suspected: 12, confirmed: 8, deaths: 0, recovered: 5, male: 7, female: 5, children: 4, adults: 8, pregnant: 1 })
  const [lab, setLab] = useState({ collected: 12, positive: 8, negative: 3, pending: 1 })
  const [env, setEnv] = useState({ temp: 31.5, rainfall: 12.4, humidity: 68, vegetation: 0.42, density: 1240 })

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Daily Surveillance Entry</h1>
          <p className="text-sm text-textMuted mt-1">Weekly Epidemiological Report — field data capture</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" icon={UploadCloud}>Upload CSV</Button>
          <Button variant="secondary" size="sm" icon={FileSpreadsheet}>Import Excel</Button>
        </div>
      </div>

      {/* Stepper */}
      <div className="flex flex-wrap gap-2">
        {SECTIONS.map((s, i) => (
          <button
            key={s.id}
            onClick={() => setStep(i)}
            className={`focus-ring flex items-center gap-2 rounded-full px-4 py-2 text-xs font-semibold border transition-all duration-200 ${
              step === i ? 'bg-accent text-white border-accent shadow-sm' : 'bg-surface text-textSecondary border-borderc hover:bg-surfaceHover'
            }`}
          >
            <s.icon size={14} /> {s.label}
          </button>
        ))}
      </div>

      <Card className="animate-riseIn">
        {step === 0 && (
          <>
            <CardHeader title="General Information" subtitle="Identify the reporting context for this record" />
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 p-5">
              <Field label="Disease">
                <select className={inputCls}>{DISEASES.map((d) => <option key={d}>{d}</option>)}</select>
              </Field>
              <Field label="Country"><input className={inputCls} value="Nigeria" readOnly /></Field>
              <Field label="State">
                <select className={inputCls} value={state} onChange={(e) => setState(e.target.value)}>
                  {STATES.map((s) => <option key={s.name}>{s.name}</option>)}
                </select>
              </Field>
              <Field label="LGA">
                <select className={inputCls}>
                  {(LGAS_BY_STATE[state] || ['Central', 'North', 'South']).map((l) => <option key={l}>{l}</option>)}
                </select>
              </Field>
              <Field label="Reporting Facility"><input className={inputCls} placeholder="e.g. General Hospital, Ikeja" /></Field>
              <Field label="Reporting Officer"><input className={inputCls} placeholder="e.g. O. Adeyemi" /></Field>
              <Field label="Date of Report"><input type="date" className={inputCls} /></Field>
              <Field label="Epidemiological Week"><input className={inputCls} placeholder="e.g. Week 40, 2026" /></Field>
            </div>
          </>
        )}

        {step === 1 && (
          <>
            <CardHeader title="Patient Statistics" subtitle="Case counts recorded for this reporting period" />
            <div className="grid sm:grid-cols-3 lg:grid-cols-5 gap-4 p-5">
              <Field label="Suspected"><NumberInput value={stats.suspected} onChange={(v) => setStats({ ...stats, suspected: v })} /></Field>
              <Field label="Confirmed"><NumberInput value={stats.confirmed} onChange={(v) => setStats({ ...stats, confirmed: v })} /></Field>
              <Field label="Deaths"><NumberInput value={stats.deaths} onChange={(v) => setStats({ ...stats, deaths: v })} /></Field>
              <Field label="Recovered"><NumberInput value={stats.recovered} onChange={(v) => setStats({ ...stats, recovered: v })} /></Field>
              <Field label="Pregnant Women"><NumberInput value={stats.pregnant} onChange={(v) => setStats({ ...stats, pregnant: v })} /></Field>
              <Field label="Male"><NumberInput value={stats.male} onChange={(v) => setStats({ ...stats, male: v })} /></Field>
              <Field label="Female"><NumberInput value={stats.female} onChange={(v) => setStats({ ...stats, female: v })} /></Field>
              <Field label="Children (<15y)"><NumberInput value={stats.children} onChange={(v) => setStats({ ...stats, children: v })} /></Field>
              <Field label="Adults (≥15y)"><NumberInput value={stats.adults} onChange={(v) => setStats({ ...stats, adults: v })} /></Field>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <CardHeader title="Laboratory" subtitle="Sample testing pipeline status" />
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 p-5">
              <Field label="Samples Collected"><NumberInput value={lab.collected} onChange={(v) => setLab({ ...lab, collected: v })} /></Field>
              <Field label="Positive"><NumberInput value={lab.positive} onChange={(v) => setLab({ ...lab, positive: v })} /></Field>
              <Field label="Negative"><NumberInput value={lab.negative} onChange={(v) => setLab({ ...lab, negative: v })} /></Field>
              <Field label="Pending"><NumberInput value={lab.pending} onChange={(v) => setLab({ ...lab, pending: v })} /></Field>
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <CardHeader title="Risk Factors" subtitle="Environmental and social conditions observed in the LGA" />
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3 p-5">
              <Toggle label="Flooding" checked={risk.flooding} onChange={(v) => setRisk({ ...risk, flooding: v })} />
              <Toggle label="Water contamination" checked={risk.water} onChange={(v) => setRisk({ ...risk, water: v })} />
              <Toggle label="Displacement" checked={risk.displacement} onChange={(v) => setRisk({ ...risk, displacement: v })} />
              <Toggle label="Poor sanitation" checked={risk.sanitation} onChange={(v) => setRisk({ ...risk, sanitation: v })} />
              <Toggle label="Conflict" checked={risk.conflict} onChange={(v) => setRisk({ ...risk, conflict: v })} />
              <Toggle label="Population movement" checked={risk.movement} onChange={(v) => setRisk({ ...risk, movement: v })} />
            </div>
          </>
        )}

        {step === 4 && (
          <>
            <CardHeader title="Environmental Variables" subtitle="Remote-sensed and climatic covariates for this LGA / week" />
            <div className="grid sm:grid-cols-2 lg:grid-cols-5 gap-4 p-5">
              <Field label="Temperature (°C)"><NumberInput value={env.temp} onChange={(v) => setEnv({ ...env, temp: v })} /></Field>
              <Field label="Rainfall (mm)"><NumberInput value={env.rainfall} onChange={(v) => setEnv({ ...env, rainfall: v })} /></Field>
              <Field label="Humidity (%)"><NumberInput value={env.humidity} onChange={(v) => setEnv({ ...env, humidity: v })} /></Field>
              <Field label="Vegetation Index (NDVI)"><NumberInput value={env.vegetation} onChange={(v) => setEnv({ ...env, vegetation: v })} /></Field>
              <Field label="Population Density (/km²)"><NumberInput value={env.density} onChange={(v) => setEnv({ ...env, density: v })} /></Field>
            </div>
          </>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-borderc px-5 py-4">
          <div>
            {validated
              ? <Badge variant="low" dot><CheckCircle2 size={12} className="inline mr-1" />Validated — ready to submit</Badge>
              : <Badge variant="neutral" dot>Draft — not yet validated</Badge>}
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" icon={Save}>Save Draft</Button>
            <Button variant="secondary" size="sm" icon={CheckCircle2} onClick={() => setValidated(true)}>Validate</Button>
            <Button variant="primary" size="sm" icon={Sparkles} disabled={!validated}>Generate Forecast</Button>
            <Button variant="primary" size="sm" disabled={!validated}>Submit Report</Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
