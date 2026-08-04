import { useEffect, useState } from 'react'
import { Download, RefreshCcw, ChevronRight } from 'lucide-react'
import KpiCard from '../components/ui/KpiCard'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import RiskGauge from '../components/ui/RiskGauge'
import NigeriaMap from '../components/ui/NigeriaMap'
import { SkeletonCard } from '../components/ui/Skeleton'
import CaseTrendChart from '../components/charts/CaseTrendChart'
import DiseasePie from '../components/charts/DiseasePie'
import { KPIS, DAILY_TREND, DISEASE_DISTRIBUTION, STATES, RECOMMENDATIONS } from '../data/mockData'

export default function Dashboard() {
  const [loading, setLoading] = useState(true)
  useEffect(() => { const t = setTimeout(() => setLoading(false), 650); return () => clearTimeout(t) }, [])

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Surveillance Overview</h1>
          <p className="text-sm text-textMuted mt-1">National disease surveillance summary · Updated 4 minutes ago</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" icon={RefreshCcw}>Refresh</Button>
          <Button variant="primary" size="sm" icon={Download}>Export Report</Button>
        </div>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
          : KPIS.map((k, i) => <KpiCard key={k.label} {...k} index={i} />)}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card className="xl:col-span-2">
          <CardHeader
            title="Daily Case Trend"
            subtitle="Reported vs laboratory-confirmed cases, last 30 days"
            right={<Badge variant="accent">Live</Badge>}
          />
          <div className="px-3 pb-4 pt-2">
            <CaseTrendChart data={DAILY_TREND} />
          </div>
        </Card>

        <Card>
          <CardHeader title="Disease Distribution" subtitle="Share of confirmed cases, YTD" />
          <div className="px-2 pb-4">
            <DiseasePie data={DISEASE_DISTRIBUTION} />
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card className="xl:col-span-2">
          <CardHeader
            title="National Risk Map"
            subtitle="Hover a state for live case load and forecast"
            right={<Badge variant="high" dot>3 states critical</Badge>}
          />
          <div className="px-5 pb-5 pt-2">
            <NigeriaMap />
          </div>
        </Card>

        <div className="space-y-5">
          <Card className="p-5 flex flex-col items-center">
            <CardHeader title="Composite National Risk" />
            <div className="mt-3"><RiskGauge value={68} /></div>
          </Card>

          <Card>
            <CardHeader title="Top Priority Actions" right={<span className="text-xs text-accent flex items-center gap-0.5 cursor-pointer">View all <ChevronRight size={13} /></span>} />
            <div className="px-5 pb-5 pt-3 space-y-3">
              {RECOMMENDATIONS.slice(0, 3).map((r, i) => (
                <div key={i} className="flex items-start gap-3 pb-3 border-b border-borderc last:border-0 last:pb-0">
                  <Badge variant={r.priority === 'High' ? 'high' : r.priority === 'Medium' ? 'medium' : 'low'} className="mt-0.5">{r.priority}</Badge>
                  <div className="min-w-0">
                    <div className="text-[13px] font-medium text-textPrimary leading-tight">{r.title}</div>
                    <div className="text-xs text-textMuted mt-0.5">{r.target}</div>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>

      <Card>
        <CardHeader title="High-Risk States Watchlist" subtitle="Ranked by composite outbreak risk score" />
        <div className="overflow-x-auto">
          <table className="w-full text-sm mt-2">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-wide text-textMuted border-y border-borderc">
                <th className="px-5 py-3 font-semibold">State</th>
                <th className="px-5 py-3 font-semibold">Active Cases</th>
                <th className="px-5 py-3 font-semibold">4-wk Forecast</th>
                <th className="px-5 py-3 font-semibold">Trend</th>
                <th className="px-5 py-3 font-semibold">Risk Level</th>
              </tr>
            </thead>
            <tbody>
              {[...STATES].sort((a, b) => b.cases - a.cases).slice(0, 6).map((s) => (
                <tr key={s.name} className="border-b border-borderc last:border-0 hover:bg-surfaceHover transition-colors">
                  <td className="px-5 py-3 font-medium text-textPrimary">{s.name}</td>
                  <td className="px-5 py-3 font-mono">{s.cases.toLocaleString()}</td>
                  <td className="px-5 py-3 font-mono text-textSecondary">{s.forecast.toLocaleString()}</td>
                  <td className="px-5 py-3">
                    <span className={s.forecast > s.cases ? 'text-danger' : 'text-success'}>
                      {s.forecast > s.cases ? '▲' : '▼'} {Math.abs(Math.round(((s.forecast - s.cases) / s.cases) * 100))}%
                    </span>
                  </td>
                  <td className="px-5 py-3"><Badge variant={s.risk}>{s.risk}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
