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

import {
  DAILY_TREND,
  DISEASE_DISTRIBUTION,
  STATES,
  RECOMMENDATIONS
} from '../data/mockData'

import { getDashboardSummary } from '../services/dashboardApi'

export default function Dashboard() {

  const [loading, setLoading] = useState(true)

  const [summary, setSummary] = useState({
    totalRecords: 0,
    totalDiseases: 0,
    totalStates: 0,
    totalLgas: 0
  })

  useEffect(() => {

    async function loadDashboard() {

      try {

        const response = await getDashboardSummary()

        setSummary(response)

      } catch (error) {

        console.error("Dashboard API Error:", error)

      } finally {

        setLoading(false)

      }

    }

    loadDashboard()

  }, [])

  return (

    <div className="space-y-6">

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">

        <div>

          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">
            Surveillance Overview
          </h1>

          <p className="text-sm text-textMuted mt-1">
            National disease surveillance summary
          </p>

        </div>

        <div className="flex gap-2">

          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCcw}
          >
            Refresh
          </Button>

          <Button
            variant="primary"
            size="sm"
            icon={Download}
          >
            Export Report
          </Button>

        </div>

      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">

        {loading ? (

          Array.from({ length: 4 }).map((_, i) => (

            <SkeletonCard key={i} />

          ))

        ) : (

          <>

            <KpiCard
              label="Total Records"
              value={summary.totalRecords}
              delta={0}
              direction="up"
              icon="Database"
              index={0}
            />

            <KpiCard
              label="Diseases"
              value={summary.totalDiseases}
              delta={0}
              direction="up"
              icon="Activity"
              index={1}
            />

            <KpiCard
              label="States"
              value={summary.totalStates}
              delta={0}
              direction="up"
              icon="Map"
              index={2}
            />

            <KpiCard
              label="LGAs"
              value={summary.totalLgas}
              delta={0}
              direction="up"
              icon="MapPinned"
              index={3}
            />

          </>

        )}

      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">

        <Card className="xl:col-span-2">

          <CardHeader
            title="Daily Case Trend"
            subtitle="Reported vs laboratory-confirmed cases"
            right={<Badge variant="accent">Live</Badge>}
          />

          <div className="px-3 pb-4 pt-2">

            <CaseTrendChart data={DAILY_TREND} />

          </div>

        </Card>

        <Card>

          <CardHeader
            title="Disease Distribution"
            subtitle="Current Distribution"
          />

          <div className="px-2 pb-4">

            <DiseasePie data={DISEASE_DISTRIBUTION} />

          </div>

        </Card>

      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">

        <Card className="xl:col-span-2">

          <CardHeader
            title="National Risk Map"
            subtitle="Interactive Risk View"
            right={<Badge variant="high">Live</Badge>}
          />

          <div className="px-5 pb-5 pt-2">

            <NigeriaMap />

          </div>

        </Card>

        <div className="space-y-5">

          <Card className="p-5 flex flex-col items-center">

            <CardHeader title="Composite National Risk" />

            <div className="mt-3">

              <RiskGauge value={68} />

            </div>

          </Card>

          <Card>

            <CardHeader
              title="Top Priority Actions"
              right={
                <span className="text-xs text-accent flex items-center gap-0.5 cursor-pointer">
                  View all
                  <ChevronRight size={13} />
                </span>
              }
            />

            <div className="px-5 pb-5 pt-3 space-y-3">

              {RECOMMENDATIONS.slice(0, 3).map((r, i) => (

                <div
                  key={i}
                  className="flex items-start gap-3 pb-3 border-b border-borderc last:border-0"
                >

                  <Badge
                    variant={
                      r.priority === 'High'
                        ? 'high'
                        : r.priority === 'Medium'
                        ? 'medium'
                        : 'low'
                    }
                  >
                    {r.priority}
                  </Badge>

                  <div>

                    <div className="text-[13px] font-medium">

                      {r.title}

                    </div>

                    <div className="text-xs text-textMuted">

                      {r.target}

                    </div>

                  </div>

                </div>

              ))}

            </div>

          </Card>

        </div>

      </div>

    </div>

  )

}