import { useState } from 'react'
import { Sparkles, TrendingUp, ShieldAlert, Syringe } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import ForecastChart from '../components/charts/ForecastChart'
import RiskGauge from '../components/ui/RiskGauge'
import { DISEASES, STATES, LGAS_BY_STATE, FORECAST_CURVE } from '../data/mockData'

const inputCls = 'w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring focus:border-accent2/60'

export default function Forecasts() {
  const [disease, setDisease] = useState('Cholera')
  const [state, setState] = useState('Kano')
  const [horizon, setHorizon] = useState('4 weeks')
  const [generated, setGenerated] = useState(true)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Forecast Panel</h1>
        <p className="text-sm text-textMuted mt-1">Heterogeneous parallel-branch model — TCN + Bi-LSTM + Transformer, matched to short-, medium-, and long-term dynamics</p>
      </div>

      <Card className="p-5">
        <div className="grid sm:grid-cols-2 lg:grid-cols-5 gap-4 items-end">
          <div>
            <label className="block text-xs font-semibold text-textSecondary mb-1.5">Disease</label>
            <select value={disease} onChange={(e) => setDisease(e.target.value)} className={inputCls}>{DISEASES.map((d) => <option key={d}>{d}</option>)}</select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-textSecondary mb-1.5">State</label>
            <select value={state} onChange={(e) => setState(e.target.value)} className={inputCls}>{STATES.map((s) => <option key={s.name}>{s.name}</option>)}</select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-textSecondary mb-1.5">LGA</label>
            <select className={inputCls}>{(LGAS_BY_STATE[state] || ['All LGAs']).map((l) => <option key={l}>{l}</option>)}</select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-textSecondary mb-1.5">Forecast Horizon</label>
            <select value={horizon} onChange={(e) => setHorizon(e.target.value)} className={inputCls}>
              {['2 weeks', '4 weeks', '8 weeks', '12 weeks'].map((h) => <option key={h}>{h}</option>)}
            </select>
          </div>
          <Button variant="primary" icon={Sparkles} onClick={() => setGenerated(true)}>Generate Forecast</Button>
        </div>
      </Card>

      {generated && (
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-5 animate-riseIn">
          <Card className="xl:col-span-2">
            <CardHeader
              title={`${disease} — ${state} Prediction`}
              subtitle={`${horizon} horizon · 95% confidence interval shaded`}
              right={<Badge variant="accent" dot>Model v3.2</Badge>}
            />
            <div className="px-3 pb-4 pt-2"><ForecastChart data={FORECAST_CURVE} /></div>
          </Card>

          <div className="space-y-5">
            <Card className="p-5 flex flex-col items-center">
              <CardHeader title="Outbreak Risk Level" />
              <div className="mt-3"><RiskGauge value={82} label={`${disease} · ${state}`} /></div>
            </Card>

            <Card className="p-5 space-y-3">
              <div className="flex justify-between text-sm"><span className="text-textMuted">Expected Cases (peak week)</span><span className="font-mono font-semibold text-textPrimary">612</span></div>
              <div className="flex justify-between text-sm"><span className="text-textMuted">Model Confidence</span><span className="font-mono font-semibold text-success">91.4%</span></div>
              <div className="flex justify-between text-sm"><span className="text-textMuted">Trend Direction</span><span className="flex items-center gap-1 text-danger font-semibold"><TrendingUp size={14} /> Increasing</span></div>
            </Card>

            <Card className="p-5">
              <div className="flex items-center gap-2 mb-3"><ShieldAlert size={16} className="text-warning" /><span className="font-display font-semibold text-sm">Suggested Intervention</span></div>
              <div className="flex items-start gap-2.5 text-sm text-textSecondary">
                <Syringe size={15} className="text-accent2 mt-0.5 shrink-0" />
                Deploy mass drug administration and reinforce laboratory testing capacity in {state} within 2 weeks to blunt the projected peak.
              </div>
              <Button variant="outlineAccent2" size="sm" className="mt-4 w-full">Route to Decision Support</Button>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}
