const WEEKS = 26
const DAYS = 7

function seededValue(w, d) {
  return Math.abs(Math.sin(w * 12.9898 + d * 78.233) * 43758.5453) % 1
}

function colorFor(v) {
  if (v > 0.85) return 'rgb(var(--c-danger))'
  if (v > 0.65) return '#F97316'
  if (v > 0.4) return 'rgb(var(--c-warning))'
  if (v > 0.18) return 'rgb(var(--c-success))'
  return 'rgb(var(--c-border))'
}

export default function CalendarHeatmap() {
  const cells = []
  for (let w = 0; w < WEEKS; w++) {
    for (let d = 0; d < DAYS; d++) {
      cells.push({ w, d, v: seededValue(w, d) })
    }
  }
  return (
    <div>
      <div className="grid grid-flow-col gap-[3px]" style={{ gridTemplateRows: `repeat(${DAYS}, 12px)` }}>
        {cells.map((c, i) => (
          <div
            key={i}
            title={`Week ${c.w + 1}, Day ${c.d + 1} — intensity ${(c.v * 100).toFixed(0)}%`}
            className="w-3 h-3 rounded-[3px] transition-transform duration-150 hover:scale-125"
            style={{ background: colorFor(c.v) }}
          />
        ))}
      </div>
      <div className="flex items-center gap-2 mt-3 text-[11px] text-textMuted">
        <span>Low</span>
        {['rgb(var(--c-border))', 'rgb(var(--c-success))', 'rgb(var(--c-warning))', '#F97316', 'rgb(var(--c-danger))'].map((c, i) => (
          <span key={i} className="w-3 h-3 rounded-[3px]" style={{ background: c }} />
        ))}
        <span>High</span>
      </div>
    </div>
  )
}
