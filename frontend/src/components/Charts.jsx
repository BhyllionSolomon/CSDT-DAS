export const GRADE_COLORS = { A: '#16a34a', B: '#2563eb', C: '#0891b2', D: '#d97706', E: '#ea580c', F: '#dc2626' }

const muted = { fill: 'var(--text-muted)', fontSize: 11 }

export function StatCard({ label, value, hint }) {
    return (
        <div className="card p-4">
            <p className="text-2xl font-bold">{value}</p>
            <p className="text-xs theme-text-muted">{label}</p>
            {hint && <p className="text-[11px] theme-text-muted mt-1">{hint}</p>}
        </div>
    )
}

export function Legend({ items }) {
    return (
        <div className="flex flex-wrap gap-4 text-xs mb-2">
            {items.map((i) => (
                <span key={i.name} className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: i.color }} />
                    {i.name}
        </span>
            ))}
        </div>
    )
}

export function BarChart({ data, height = 230, max }) {
    const W = 600
    const pad = { l: 34, r: 10, t: 22, b: 34 }
    const innerW = W - pad.l - pad.r
    const innerH = height - pad.t - pad.b
    const top = max || Math.max(1, ...data.map((d) => d.value))
    const step = innerW / Math.max(1, data.length)
    const barW = Math.min(60, step * 0.62)

    return (
        <svg viewBox={`0 0 ${W} ${height}`} width="100%" role="img">
            {[0, 0.5, 1].map((f) => {
                const y = pad.t + innerH - f * innerH
                return (
                    <g key={f}>
                        <line x1={pad.l} x2={W - pad.r} y1={y} y2={y} stroke="var(--border)" />
                        <text x={pad.l - 6} y={y + 4} textAnchor="end" style={muted}>{Math.round(top * f)}</text>
                    </g>
                )
            })}
            {data.map((d, i) => {
                const h = (d.value / top) * innerH
                const x = pad.l + i * step + (step - barW) / 2
                const y = pad.t + innerH - h
                const label = d.label.length > 14 ? `${d.label.slice(0, 13)}…` : d.label
                return (
                    <g key={d.label}>
                        <rect x={x} y={y} width={barW} height={Math.max(h, 0)} rx="4" fill={d.color || 'var(--accent)'} />
                        <text x={x + barW / 2} y={y - 6} textAnchor="middle" style={{ ...muted, fontWeight: 600 }}>{d.value}</text>
                        <text x={x + barW / 2} y={height - 12} textAnchor="middle" style={muted}>{label}</text>
                    </g>
                )
            })}
        </svg>
    )
}

export function LineChart({ labels, series, height = 240, max = 100 }) {
    const W = 600
    const pad = { l: 36, r: 14, t: 16, b: 34 }
    const innerW = W - pad.l - pad.r
    const innerH = height - pad.t - pad.b
    const x = (i) => pad.l + (labels.length <= 1 ? innerW / 2 : (i / (labels.length - 1)) * innerW)
    const y = (v) => pad.t + innerH - (v / max) * innerH

    return (
        <svg viewBox={`0 0 ${W} ${height}`} width="100%" role="img">
            {[0, 0.25, 0.5, 0.75, 1].map((f) => (
                <g key={f}>
                    <line x1={pad.l} x2={W - pad.r} y1={y(f * max)} y2={y(f * max)} stroke="var(--border)" />
                    <text x={pad.l - 6} y={y(f * max) + 4} textAnchor="end" style={muted}>{Math.round(f * max)}</text>
                </g>
            ))}
            {labels.map((l, i) => (
                <text key={`${l}-${i}`} x={x(i)} y={height - 12} textAnchor="middle" style={muted}>{l}</text>
            ))}
            {series.map((s) => {
                const points = s.values.map((v, i) => (v == null ? null : [x(i), y(v)])).filter(Boolean)
                return (
                    <g key={s.name}>
                        {points.length > 1 && (
                            <polyline points={points.map((p) => p.join(',')).join(' ')} fill="none" stroke={s.color} strokeWidth="2.5" />
                        )}
                        {s.values.map((v, i) => v == null ? null : (
                            <g key={i}>
                                <circle cx={x(i)} cy={y(v)} r="4" fill={s.color} />
                                <text x={x(i)} y={y(v) - 9} textAnchor="middle" style={{ ...muted, fontWeight: 600 }}>{v}</text>
                            </g>
                        ))}
                    </g>
                )
            })}
        </svg>
    )
}

export function HBars({ items, max = 100, unit = '' }) {
    return (
        <ul className="space-y-3">
            {items.map((it) => (
                <li key={it.label} className="text-sm">
                    <div className="flex justify-between gap-3">
                        <span className="truncate">{it.label}</span>
                        <span className="font-medium shrink-0">{it.value}{unit}</span>
                    </div>
                    <div className="h-2 rounded-full mt-1" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <div className="h-2 rounded-full"
                             style={{ width: `${Math.min(100, (it.value / (max || 1)) * 100)}%`, backgroundColor: it.color || 'var(--accent)' }} />
                    </div>
                    {it.sub && <p className="text-[11px] theme-text-muted mt-0.5">{it.sub}</p>}
                </li>
            ))}
        </ul>
    )
}