import { ResponsiveContainer } from 'recharts'
export default function ChartWrap({ height = 280, children }) {
  return (
    <div style={{ width: '100%', height }}>
      <ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer>
    </div>
  )
}
export const axisStyle = { fontSize: 11, fill: 'rgb(var(--c-text-muted))', fontFamily: 'IBM Plex Mono' }
export const gridStroke = 'rgb(var(--c-border))'
export const tooltipStyle = {
  backgroundColor: 'rgb(var(--c-surface))',
  border: '1px solid rgb(var(--c-border))',
  borderRadius: 10,
  fontSize: 12,
  color: 'rgb(var(--c-text-primary))',
  boxShadow: '0 8px 30px rgba(0,0,0,0.16)',
}
