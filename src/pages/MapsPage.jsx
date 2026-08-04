import { useState } from 'react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import NigeriaMap from '../components/ui/NigeriaMap'
import { STATES } from '../data/mockData'

export default function MapsPage() {
  const [selected, setSelected] = useState(null)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Interactive Surveillance Map</h1>
        <p className="text-sm text-textMuted mt-1">Click a state marker to pin its detail card</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card className="xl:col-span-2">
          <CardHeader title="Nigeria — Risk-Weighted Surveillance Map" subtitle="Marker size reflects active case volume" />
          <div className="px-5 pb-5 pt-2"><NigeriaMap onSelect={setSelected} selected={selected} /></div>
        </Card>

        <Card>
          <CardHeader title="State Rankings" subtitle="Sorted by active case load" />
          <div className="px-5 pb-5 pt-2 space-y-1 max-h-[420px] overflow-y-auto">
            {[...STATES].sort((a, b) => b.cases - a.cases).map((s, i) => (
              <button
                key={s.name}
                onClick={() => setSelected(s)}
                className={`w-full flex items-center justify-between gap-3 rounded-lg px-3 py-2.5 text-left transition-colors ${selected?.name === s.name ? 'bg-accent/10' : 'hover:bg-surfaceHover'}`}
              >
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-textMuted w-4">{i + 1}</span>
                  <span className="text-sm font-medium text-textPrimary">{s.name}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-textSecondary">{s.cases}</span>
                  <Badge variant={s.risk}>{s.risk}</Badge>
                </div>
              </button>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}
