import { useState } from 'react'
import { LayoutGrid, Filter } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import CaseTrendChart from '../components/charts/CaseTrendChart'
import StackedArea from '../components/charts/StackedArea'
import DiseasePie from '../components/charts/DiseasePie'
import BarTrend from '../components/charts/BarTrend'
import CalendarHeatmap from '../components/charts/CalendarHeatmap'
import { DAILY_TREND, WEEKLY_TREND, DISEASE_DISTRIBUTION, MONTHLY_TREND, DISEASES } from '../data/mockData'

export default function Analytics() {
  const [disease, setDisease] = useState('All Diseases')

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Analytics</h1>
          <p className="text-sm text-textMuted mt-1">Drill down across disease, geography, and time</p>
        </div>
        <div className="flex gap-2">
          <select value={disease} onChange={(e) => setDisease(e.target.value)} className="bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm focus-ring">
            <option>All Diseases</option>{DISEASES.map((d) => <option key={d}>{d}</option>)}
          </select>
          <button className="focus-ring flex items-center gap-2 rounded-lg border border-borderc bg-surface px-3.5 py-2.5 text-sm text-textSecondary hover:bg-surfaceHover">
            <Filter size={15} /> Filters
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <Card>
          <CardHeader title="Weekly Trend — Observed vs Model Forecast" subtitle="12-week rolling window" right={<Badge variant="accent">{disease}</Badge>} />
          <div className="px-3 pb-4 pt-2"><StackedArea data={WEEKLY_TREND} /></div>
        </Card>
        <Card>
          <CardHeader title="Daily Trend" subtitle="Last 30 days" />
          <div className="px-3 pb-4 pt-2"><CaseTrendChart data={DAILY_TREND} /></div>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card>
          <CardHeader title="Disease Distribution" />
          <div className="px-2 pb-4"><DiseasePie data={DISEASE_DISTRIBUTION} /></div>
        </Card>
        <Card>
          <CardHeader title="Monthly Case Volume" />
          <div className="px-3 pb-4 pt-2"><BarTrend data={MONTHLY_TREND} /></div>
        </Card>
        <Card>
          <CardHeader title="Case Density" subtitle="Illustrative severity index by state" />
          <div className="px-5 pb-5 pt-2 grid grid-cols-4 gap-2">
            {Array.from({ length: 16 }).map((_, i) => {
              const intensity = Math.abs(Math.sin(i * 1.7)) 
              const bg = intensity > 0.75 ? 'bg-danger' : intensity > 0.5 ? 'bg-warning' : intensity > 0.25 ? 'bg-accent2' : 'bg-success'
              return <div key={i} className={`h-9 rounded-md ${bg} opacity-80 hover:opacity-100 transition-opacity`} />
            })}
          </div>
        </Card>
      </div>

      <Card>
        <CardHeader title="Reporting Activity Heatmap" subtitle="Daily reporting-completeness intensity, last 26 weeks" right={<LayoutGrid size={16} className="text-textMuted" />} />
        <div className="px-5 pb-6 pt-3 overflow-x-auto"><CalendarHeatmap /></div>
      </Card>
    </div>
  )
}
