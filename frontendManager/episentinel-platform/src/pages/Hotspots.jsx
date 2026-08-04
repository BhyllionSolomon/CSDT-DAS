import { Flame } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import { STATES } from '../data/mockData'

export default function Hotspots() {
  const hotspots = [...STATES].filter((s) => s.risk === 'critical' || s.risk === 'high').sort((a, b) => b.cases - a.cases)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Outbreak Hotspots</h1>
        <p className="text-sm text-textMuted mt-1">States and LGAs flagged by the anomaly-detection layer</p>
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {hotspots.map((s, i) => (
          <Card key={s.name} className="p-5 animate-riseIn" style={{ animationDelay: `${i * 60}ms` }}>
            <div className="flex items-center justify-between mb-3">
              <div className="h-10 w-10 rounded-lg bg-danger/10 text-danger flex items-center justify-center">
                <Flame size={18} />
              </div>
              <Badge variant={s.risk}>{s.risk}</Badge>
            </div>
            <div className="font-display font-semibold text-textPrimary">{s.name} State</div>
            <div className="text-xs text-textMuted mt-0.5 mb-4">Composite anomaly score elevated for 3 consecutive weeks</div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <div className="text-textMuted text-xs">Active Cases</div>
                <div className="font-mono font-semibold text-textPrimary">{s.cases}</div>
              </div>
              <div>
                <div className="text-textMuted text-xs">4-wk Forecast</div>
                <div className="font-mono font-semibold text-danger">{s.forecast}</div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
