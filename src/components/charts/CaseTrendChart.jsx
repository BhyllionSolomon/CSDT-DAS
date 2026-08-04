import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Line, ComposedChart } from 'recharts'
import ChartWrap, { axisStyle, gridStroke, tooltipStyle } from './ChartWrap'

export default function CaseTrendChart({ data }) {
  return (
    <ChartWrap>
      <ComposedChart data={data} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="casesFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgb(var(--c-accent))" stopOpacity={0.35} />
            <stop offset="100%" stopColor="rgb(var(--c-accent))" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke={gridStroke} strokeDasharray="3 5" vertical={false} />
        <XAxis dataKey="day" tick={axisStyle} axisLine={{ stroke: gridStroke }} tickLine={false} interval={4} />
        <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} labelStyle={{ color: 'rgb(var(--c-text-muted))' }} />
        <Area type="monotone" dataKey="cases" stroke="rgb(var(--c-accent))" fill="url(#casesFill)" strokeWidth={2} animationDuration={900} name="Reported Cases" />
        <Line type="monotone" dataKey="confirmed" stroke="rgb(var(--c-accent-2))" strokeWidth={2} dot={false} animationDuration={900} name="Lab Confirmed" />
      </ComposedChart>
    </ChartWrap>
  )
}
