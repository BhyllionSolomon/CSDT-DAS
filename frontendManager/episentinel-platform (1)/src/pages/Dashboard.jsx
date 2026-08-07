import { useEffect, useState } from 'react'
import { Download, RefreshCcw, ChevronRight, AlertTriangle } from 'lucide-react'
import KpiCard from '../components/ui/KpiCard'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import RiskGauge from '../components/ui/RiskGauge'
import NigeriaMap from '../components/ui/NigeriaMap'
import { SkeletonCard } from '../components/ui/Skeleton'
import CaseTrendChart from '../components/charts/CaseTrendChart'
import DiseasePie from '../components/charts/DiseasePie'
// DAILY_TREND and RECOMMENDATIONS are still mock — see the "Daily Case Trend"
// and "Top Priority Actions" notes below for why those two aren't live yet.
import { DAILY_TREND, RECOMMENDATIONS } from '../data/mockData'
import { loadDashboardData } from '../services/dashboardService'

export default function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [kpis, setKpis] = useState([])
  const [diseaseDistribution, setDiseaseDistribution] = useState([])
  const [states, setStates] = useState([])
  const [riskScore, setRiskScore] = useState(0)
  const [topActions, setTopActions] = useState([])
  const [loadError, setLoadError] = useState(null)

  const fetchAll = async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const result = await loadDashboardData()
      setKpis(result.kpis)
      setDiseaseDistribution(result.diseaseDistribution)
      setStates(result.states)
      setRiskScore(result.prediction.riskScore)
      setTopActions(result.prediction.topActions)

      const failed = Object.entries(result.errors).filter(([, err]) => err)
      if (failed.length) {
        setLoadError(`${failed.length} of 4 dashboard endpoints failed — showing partial data. Check the console for details.`)
      }
    } catch (err) {
      console.error('[Dashboard] Failed to load dashboard data', err)
      setLoadError('Could not reach the backend. Is it running at the configured API base URL?')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchAll() }, [])

  // Fall back to mock recommendations only if the backend hasn't sent any yet,
  // so the "Top Priority Actions" card never renders empty during integration.
  const displayedActions = topActions.length ? topActions : RECOMMENDATIONS.slice(0, 3)
  const sortedStates = [...states].sort((a, b) => b.cases - a.cases).slice(0, 6)
  const criticalCount = states.filter((s) => s.risk === 'critical').length

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Surveillance Overview</h1>
          <p className="text-sm text-textMuted mt-1">National disease surveillance summary · Updated 4 minutes ago</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" icon={RefreshCcw} onClick={fetchAll}>Refresh</Button>
          <Button variant="primary" size="sm" icon={Download}>Export Report</Button>
        </div>
      </div>

      {loadError && (
        <div className="flex items-center gap-2.5 rounded-lg border border-warning/30 bg-warning/10 px-4 py-3 text-sm text-warning">
          <AlertTriangle size={16} className="shrink-0" />
          {loadError}
        </div>
      )}

      <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {loading
          ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
          : kpis.map((k, i) => <KpiCard key={k.label} {...k} index={i} />)}
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
            <DiseasePie data={diseaseDistribution} />
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card className="xl:col-span-2">
          <CardHeader
            title="National Risk Map"
            subtitle="Hover a state for live case load and forecast"
            right={<Badge variant="high" dot>{criticalCount} states critical</Badge>}
          />
          <div className="px-5 pb-5 pt-2">
            <NigeriaMap states={states.length ? states : undefined} />
          </div>
        </Card>

        <div className="space-y-5">
          <Card className="p-5 flex flex-col items-center">
            <CardHeader title="Composite National Risk" />
            <div className="mt-3"><RiskGauge value={riskScore} /></div>
          </Card>

          <Card>
            <CardHeader title="Top Priority Actions" right={<span className="text-xs text-accent flex items-center gap-0.5 cursor-pointer">View all <ChevronRight size={13} /></span>} />
            <div className="px-5 pb-5 pt-3 space-y-3">
              {displayedActions.map((r, i) => (
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
              {sortedStates.map((s) => (
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
