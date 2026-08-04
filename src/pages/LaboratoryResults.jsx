import { FlaskConical, CheckCircle2, XCircle, Clock } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import BarTrend from '../components/charts/BarTrend'
import { MONTHLY_TREND } from '../data/mockData'

const STAT_CARDS = [
  { label: 'Samples Collected', value: 9842, icon: FlaskConical, iconCls: 'bg-accent/10 text-accent' },
  { label: 'Positive', value: 4108, icon: CheckCircle2, iconCls: 'bg-danger/10 text-danger' },
  { label: 'Negative', value: 5210, icon: XCircle, iconCls: 'bg-success/10 text-success' },
  { label: 'Pending', value: 524, icon: Clock, iconCls: 'bg-warning/10 text-warning' },
]

export default function LaboratoryResults() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Laboratory Results</h1>
        <p className="text-sm text-textMuted mt-1">National reference laboratory network — testing throughput</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {STAT_CARDS.map((c) => (
          <Card key={c.label} className="p-5">
            <div className={`h-9 w-9 rounded-lg flex items-center justify-center mb-3 ${c.iconCls}`}>
              <c.icon size={18} />
            </div>
            <div className="font-mono text-2xl font-semibold text-textPrimary tabular">{c.value.toLocaleString()}</div>
            <div className="text-xs text-textMuted mt-1">{c.label}</div>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader title="Monthly Testing Volume" subtitle="Samples processed per month, national network" right={<Badge variant="accent">6-month view</Badge>} />
        <div className="px-3 pb-4 pt-2"><BarTrend data={MONTHLY_TREND} dataKey="cases" xKey="month" /></div>
      </Card>
    </div>
  )
}
