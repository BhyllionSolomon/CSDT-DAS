import { PieChart, Pie, Cell, Tooltip, Legend } from 'recharts'
import ChartWrap, { tooltipStyle } from './ChartWrap'

export default function DiseasePie({ data }) {
  return (
    <ChartWrap height={280}>
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          innerRadius={62}
          outerRadius={92}
          paddingAngle={3}
          animationDuration={900}
        >
          {data.map((d, i) => (
            <Cell key={i} fill={d.color} stroke="rgb(var(--c-surface))" strokeWidth={2} />
          ))}
        </Pie>
        <Tooltip contentStyle={tooltipStyle} />
        <Legend
          layout="vertical"
          align="right"
          verticalAlign="middle"
          iconType="circle"
          iconSize={8}
          formatter={(v) => <span className="text-textSecondary text-xs">{v}</span>}
        />
      </PieChart>
    </ChartWrap>
  )
}
