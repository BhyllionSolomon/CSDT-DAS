import * as Icons from 'lucide-react'
import Card from './Card'

export default function KpiCard({ label, value, delta, direction, icon, index = 0 }) {
  const Icon = Icons[icon] || Icons.Activity
  const isUp = direction === 'up'
  const positiveIsGood = !['Deaths', 'Active Outbreaks', 'High-Risk LGAs'].includes(label)
  const good = positiveIsGood ? isUp : !isUp
  const TrendIcon = isUp ? Icons.TrendingUp : Icons.TrendingDown

  // Deterministic pseudo-curve path per card for the signature epidemic-curve motif
  const seed = index * 13 + 7
  const points = Array.from({ length: 12 }, (_, i) => {
    const n = Math.sin((i + seed) * 0.9) * 6 + Math.sin((i + seed) * 0.35) * 4
    return 18 - n
  })
  const path = points.map((y, i) => `${i === 0 ? 'M' : 'L'} ${i * (120 / 11)} ${y}`).join(' ')

  return (
    <Card className="p-5 relative overflow-hidden animate-riseIn" style={{ animationDelay: `${index * 60}ms` }}>
      <svg className="absolute top-0 left-0 w-full h-9 opacity-70" viewBox="0 0 120 36" preserveAspectRatio="none">
        <path d={path} fill="none" stroke="rgb(var(--c-accent-2))" strokeWidth="1.5" strokeLinecap="round" strokeDasharray="240" className="animate-curve" />
      </svg>
      <div className="flex items-start justify-between relative">
        <div className="h-9 w-9 rounded-lg bg-accent/10 flex items-center justify-center text-accent">
          <Icon size={18} strokeWidth={2.25} />
        </div>
        <div className={`flex items-center gap-1 text-xs font-semibold ${good ? 'text-success' : 'text-danger'}`}>
          <TrendIcon size={13} strokeWidth={2.5} />
          {delta}{typeof delta === 'number' && delta < 10 ? '%' : ''}
        </div>
      </div>
      <div className="mt-4">
        <div className="font-mono text-2xl font-semibold text-textPrimary tabular tracking-tight">
          {value.toLocaleString()}
        </div>
        <div className="text-xs text-textMuted mt-1">{label}</div>
      </div>
    </Card>
  )
}
