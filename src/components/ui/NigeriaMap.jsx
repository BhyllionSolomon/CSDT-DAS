import { useState } from 'react'
import { STATES } from '../../data/mockData'

const RISK_COLOR = {
  critical: 'rgb(var(--c-danger))',
  high: '#F97316',
  medium: 'rgb(var(--c-warning))',
  low: 'rgb(var(--c-success))',
}

const VB_W = 600
const VB_H = 460
const LAT_MIN = 4, LAT_MAX = 14
const LNG_MIN = 2.5, LNG_MAX = 14.5

function project(lat, lng) {
  const x = ((lng - LNG_MIN) / (LNG_MAX - LNG_MIN)) * (VB_W - 80) + 40
  const y = (1 - (lat - LAT_MIN) / (LAT_MAX - LAT_MIN)) * (VB_H - 80) + 40
  return [x, y]
}

// Stylised approximation of Nigeria's silhouette for schematic display purposes.
const OUTLINE = `M 90 120
  C 70 95, 90 55, 140 48
  C 190 40, 250 55, 300 42
  C 360 28, 430 35, 470 60
  C 500 78, 500 110, 530 130
  C 560 150, 555 190, 520 205
  C 540 235, 520 265, 480 270
  C 470 300, 440 320, 420 350
  C 400 385, 360 400, 330 385
  C 310 410, 270 415, 250 390
  C 210 400, 175 380, 170 345
  C 130 340, 100 310, 105 275
  C 75 260, 60 220, 80 190
  C 60 170, 65 140, 90 120 Z`

export default function NigeriaMap({ onSelect, selected }) {
  const [hovered, setHovered] = useState(null)
  const active = hovered || selected

  return (
    <div className="relative w-full">
      <svg viewBox={`0 0 ${VB_W} ${VB_H}`} className="w-full h-auto">
        <defs>
          <pattern id="grid" width="24" height="24" patternUnits="userSpaceOnUse">
            <path d="M 24 0 L 0 0 0 24" fill="none" stroke="rgb(var(--c-border))" strokeWidth="0.5" opacity="0.4" />
          </pattern>
        </defs>
        <rect width={VB_W} height={VB_H} fill="url(#grid)" rx="16" />
        <path d={OUTLINE} fill="rgb(var(--c-surface-hover))" stroke="rgb(var(--c-border))" strokeWidth="1.5" />

        {STATES.map((s) => {
          const [x, y] = project(s.lat, s.lng)
          const r = 9 + Math.min(14, s.cases / 60)
          const isActive = active?.name === s.name
          return (
            <g
              key={s.name}
              transform={`translate(${x} ${y})`}
              onMouseEnter={() => setHovered(s)}
              onMouseLeave={() => setHovered(null)}
              onClick={() => onSelect?.(s)}
              className="cursor-pointer"
            >
              <circle r={r + 6} fill={RISK_COLOR[s.risk]} opacity={isActive ? 0.22 : 0.12} className="transition-all duration-300" />
              <circle
                r={r}
                fill={RISK_COLOR[s.risk]}
                stroke="rgb(var(--c-surface))"
                strokeWidth="2"
                opacity={isActive ? 1 : 0.85}
                className="transition-all duration-300"
              />
              {s.risk === 'critical' && (
                <circle r={r} fill="none" stroke={RISK_COLOR[s.risk]} strokeWidth="2" className="animate-pulseSoft" />
              )}
              <text y={-r - 7} textAnchor="middle" className="fill-textSecondary font-body" style={{ fontSize: 10, fontWeight: isActive ? 700 : 500 }}>
                {s.name}
              </text>
            </g>
          )
        })}
      </svg>

      {active && (
        <div className="absolute top-3 right-3 bg-surface border border-borderc rounded-lg shadow-elevated p-4 w-56 animate-riseIn">
          <div className="flex items-center justify-between mb-2">
            <span className="font-display font-semibold text-sm text-textPrimary">{active.name}</span>
            <span className="h-2 w-2 rounded-full" style={{ background: RISK_COLOR[active.risk] }} />
          </div>
          <dl className="space-y-1.5 text-xs">
            <div className="flex justify-between"><dt className="text-textMuted">Active Cases</dt><dd className="font-mono text-textPrimary">{active.cases}</dd></div>
            <div className="flex justify-between"><dt className="text-textMuted">4-wk Forecast</dt><dd className="font-mono text-textPrimary">{active.forecast}</dd></div>
            <div className="flex justify-between"><dt className="text-textMuted">Risk Level</dt><dd className="capitalize font-semibold" style={{ color: RISK_COLOR[active.risk] }}>{active.risk}</dd></div>
          </dl>
        </div>
      )}

      <div className="flex items-center gap-4 mt-3 px-1">
        {Object.entries(RISK_COLOR).map(([k, c]) => (
          <div key={k} className="flex items-center gap-1.5 text-[11px] text-textMuted capitalize">
            <span className="h-2 w-2 rounded-full" style={{ background: c }} /> {k}
          </div>
        ))}
      </div>
    </div>
  )
}
