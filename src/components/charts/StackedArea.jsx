import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts'
import ChartWrap, { axisStyle, gridStroke, tooltipStyle } from './ChartWrap'

export default function StackedArea({ data }) {
  return (
    <ChartWrap height={280}>
      <AreaChart data={data} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="wCases" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgb(var(--c-accent))" stopOpacity={0.5} />
            <stop offset="100%" stopColor="rgb(var(--c-accent))" stopOpacity={0.05} />
          </linearGradient>
          <linearGradient id="wForecast" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgb(var(--c-accent-2))" stopOpacity={0.4} />
            <stop offset="100%" stopColor="rgb(var(--c-accent-2))" stopOpacity={0.03} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke={gridStroke} strokeDasharray="3 5" vertical={false} />
        <XAxis dataKey="week" tick={axisStyle} axisLine={{ stroke: gridStroke }} tickLine={false} />
        <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} />
        <Legend formatter={(v) => <span className="text-textSecondary text-xs">{v}</span>} iconType="circle" iconSize={8} />
        <Area type="monotone" dataKey="cases" name="Observed" stroke="rgb(var(--c-accent))" fill="url(#wCases)" strokeWidth={2} />
        <Area type="monotone" dataKey="forecast" name="Model Forecast" stroke="rgb(var(--c-accent-2))" fill="url(#wForecast)" strokeWidth={2} strokeDasharray="5 4" />
      </AreaChart>
    </ChartWrap>
  )
}
