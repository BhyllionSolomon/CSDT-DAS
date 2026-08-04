export default function RiskGauge({ value = 0, label = 'Composite Risk Index', size = 180 }) {
  const pct = Math.min(100, Math.max(0, value))
  const angle = (pct / 100) * 180
  const r = size / 2 - 14
  const cx = size / 2
  const cy = size / 2
  const toXY = (deg) => {
    const rad = (Math.PI * (180 - deg)) / 180
    return [cx + r * Math.cos(rad), cy - r * Math.sin(rad)]
  }
  const [nx, ny] = toXY(angle)
  const color = pct > 75 ? 'rgb(var(--c-danger))' : pct > 45 ? 'rgb(var(--c-warning))' : 'rgb(var(--c-success))'
  const tier = pct > 75 ? 'Critical' : pct > 45 ? 'Elevated' : 'Stable'

  return (
    <div className="flex flex-col items-center">
      <svg width={size} height={size / 1.65} viewBox={`0 0 ${size} ${size / 1.65}`}>
        <path
          d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
          fill="none"
          stroke="rgb(var(--c-border))"
          strokeWidth="14"
          strokeLinecap="round"
        />
        <path
          d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
          fill="none"
          stroke={color}
          strokeWidth="14"
          strokeLinecap="round"
          strokeDasharray={`${(angle / 180) * Math.PI * r} ${Math.PI * r}`}
          className="transition-all duration-700 ease-out"
        />
        <circle cx={nx} cy={ny} r="6" fill={color} stroke="rgb(var(--c-surface))" strokeWidth="2" />
        <text x={cx} y={cy - 4} textAnchor="middle" className="fill-textPrimary font-mono font-semibold" style={{ fontSize: size * 0.15 }}>
          {pct.toFixed(0)}
        </text>
      </svg>
      <div className="text-center -mt-1">
        <div className="text-[11px] font-semibold uppercase tracking-wide" style={{ color }}>{tier}</div>
        <div className="text-xs text-textMuted mt-0.5">{label}</div>
      </div>
    </div>
  )
}
