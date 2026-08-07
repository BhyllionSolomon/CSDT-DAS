import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell } from 'recharts'
import ChartWrap, { axisStyle, gridStroke, tooltipStyle } from './ChartWrap'

export default function BarTrend({ data, dataKey = 'cases', xKey = 'month', highlightLast = true }) {
  return (
    <ChartWrap height={260}>
      <BarChart data={data} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
        <CartesianGrid stroke={gridStroke} strokeDasharray="3 5" vertical={false} />
        <XAxis dataKey={xKey} tick={axisStyle} axisLine={{ stroke: gridStroke }} tickLine={false} />
        <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgb(var(--c-surface-hover))' }} />
        <Bar dataKey={dataKey} radius={[6, 6, 0, 0]} animationDuration={900}>
          {data.map((_, i) => (
            <Cell key={i} fill={highlightLast && i === data.length - 1 ? 'rgb(var(--c-accent-2))' : 'rgb(var(--c-accent))'} fillOpacity={highlightLast && i === data.length - 1 ? 1 : 0.75} />
          ))}
        </Bar>
      </BarChart>
    </ChartWrap>
  )
}
