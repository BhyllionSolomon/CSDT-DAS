import { ComposedChart, Area, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine } from 'recharts'
import ChartWrap, { axisStyle, gridStroke, tooltipStyle } from './ChartWrap'

export default function ForecastChart({ data }) {
  return (
    <ChartWrap height={320}>
      <ComposedChart data={data} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="ciFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgb(var(--c-accent-2))" stopOpacity={0.25} />
            <stop offset="100%" stopColor="rgb(var(--c-accent-2))" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke={gridStroke} strokeDasharray="3 5" vertical={false} />
        <XAxis dataKey="period" tick={axisStyle} axisLine={{ stroke: gridStroke }} tickLine={false} />
        <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} />
        <ReferenceLine x="W10" stroke="rgb(var(--c-warning))" strokeDasharray="4 4" label={{ value: 'Forecast start', fill: 'rgb(var(--c-warning))', fontSize: 10, position: 'insideTopRight' }} />
        <Area dataKey="upper" stroke="none" fill="url(#ciFill)" name="Upper CI" />
        <Area dataKey="lower" stroke="none" fill="rgb(var(--c-bg))" fillOpacity={1} name="Lower CI" />
        <Line type="monotone" dataKey="actual" stroke="rgb(var(--c-accent))" strokeWidth={2.5} dot={{ r: 3 }} name="Observed" connectNulls={false} />
        <Line type="monotone" dataKey="predicted" stroke="rgb(var(--c-accent-2))" strokeWidth={2.5} strokeDasharray="6 4" dot={false} name="Predicted" />
      </ComposedChart>
    </ChartWrap>
  )
}
