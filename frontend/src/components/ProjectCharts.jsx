export const PALETTE = ['#3b82f6', '#16a34a', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4']

// A ring chart with a legend.
export function Donut({ data, size = 140, thickness = 22, center, sub }) {
    const total = data.reduce((t, d) => t + d.value, 0)
    const r = (size - thickness) / 2
    const c = 2 * Math.PI * r
    let offset = 0

    return (
        <div className="flex items-center gap-4 flex-wrap">
            <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="shrink-0">
                <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--surface-soft)" strokeWidth={thickness} />
                {total > 0 && data.map((d, i) => {
                    const len = (d.value / total) * c
                    const el = (
                        <circle key={i} cx={size / 2} cy={size / 2} r={r} fill="none" stroke={d.color || PALETTE[i % PALETTE.length]} strokeWidth={thickness}
                                strokeDasharray={`${len} ${c - len}`} strokeDashoffset={-offset} transform={`rotate(-90 ${size / 2} ${size / 2})`}>
                            <title>{`${d.label}: ${d.value}`}</title>
                        </circle>
                    )
                    offset += len
                    return el
                })}
                <text x="50%" y={sub ? '47%' : '52%'} textAnchor="middle" fontSize="22" fontWeight="700" fill="currentColor">{center ?? total}</text>
                {sub && <text x="50%" y="63%" textAnchor="middle" fontSize="10" fill="currentColor" opacity="0.6">{sub}</text>}
            </svg>
            <ul className="text-sm space-y-1">
                {data.map((d, i) => (
                    <li key={i} className="flex items-center gap-2">
                        <span className="w-3 h-3 rounded-sm inline-block" style={{ backgroundColor: d.color || PALETTE[i % PALETTE.length] }} />
                        <span>{d.label}</span><b className="ml-1">{d.value}</b>
                    </li>
                ))}
            </ul>
        </div>
    )
}

// Horizontal bars, one per row.
export function HBars({ data, max, suffix = '' }) {
    const top = max || Math.max(1, ...data.map((d) => d.value))
    return (
        <div className="space-y-2">
            {data.map((d, i) => (
                <div key={i} className="flex items-center gap-3 text-sm">
                    <span className="w-40 shrink-0 truncate">{d.label}</span>
                    <div className="flex-1 h-3 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <div className="h-full rounded-full" style={{ width: `${(d.value / top) * 100}%`, backgroundColor: d.color || PALETTE[i % PALETTE.length] }} />
                    </div>
                    <span className="w-14 text-right font-semibold">{d.value}{suffix}</span>
                </div>
            ))}
        </div>
    )
}

// Vertical bars.
export function Columns({ data, height = 120 }) {
    const top = Math.max(1, ...data.map((d) => d.value))
    return (
        <div className="flex items-end gap-4" style={{ height: height + 34 }}>
            {data.map((d, i) => (
                <div key={i} className="flex-1 flex flex-col items-center justify-end h-full">
                    <span className="text-xs font-semibold mb-1">{d.value}</span>
                    <div className="w-full max-w-[44px] rounded-t-md" style={{ height: `${(d.value / top) * height}px`, minHeight: d.value > 0 ? 4 : 0, backgroundColor: d.color || PALETTE[i % PALETTE.length] }} />
                    <span className="text-[10px] theme-text-muted mt-1 text-center leading-tight">{d.label}</span>
                </div>
            ))}
        </div>
    )
}

// A single progress ring with a number in the middle.
export function Ring({ value, total, label, size = 120 }) {
    const thickness = 12
    const r = (size - thickness) / 2
    const c = 2 * Math.PI * r
    const share = total > 0 ? value / total : 0
    return (
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
            <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--surface-soft)" strokeWidth={thickness} />
            <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--accent)" strokeWidth={thickness} strokeLinecap="round"
                    strokeDasharray={`${share * c} ${c}`} transform={`rotate(-90 ${size / 2} ${size / 2})`} />
            <text x="50%" y="48%" textAnchor="middle" fontSize="22" fontWeight="700" fill="currentColor">{value}/{total}</text>
            <text x="50%" y="64%" textAnchor="middle" fontSize="10" fill="currentColor" opacity="0.6">{label}</text>
        </svg>
    )
}