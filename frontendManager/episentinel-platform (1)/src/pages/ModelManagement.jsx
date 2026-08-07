import { Cpu, UploadCloud, Play, History } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts'
import ChartWrap, { axisStyle, gridStroke, tooltipStyle } from '../components/charts/ChartWrap'
import { MODEL_STATUS } from '../data/mockData'

function ModelCard({ title, m, badge }) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between mb-3">
        <div className="h-9 w-9 rounded-lg bg-accent/10 text-accent flex items-center justify-center"><Cpu size={17} /></div>
        <Badge variant={badge}>{m.status}</Badge>
      </div>
      <div className="text-xs text-textMuted mb-0.5">{title}</div>
      <div className="font-display font-semibold text-textPrimary text-[15px] leading-snug">{m.name}</div>
      <div className="grid grid-cols-3 gap-3 mt-4">
        <div><div className="text-textMuted text-xs">Accuracy</div><div className="font-mono font-semibold text-success">{m.accuracy}%</div></div>
        <div><div className="text-textMuted text-xs">MAE</div><div className="font-mono font-semibold text-textPrimary">{m.mae}</div></div>
        <div><div className="text-textMuted text-xs">RMSE</div><div className="font-mono font-semibold text-textPrimary">{m.rmse}</div></div>
      </div>
      <div className="text-[11px] text-textMuted mt-4">Trained {m.trainedOn}</div>
    </Card>
  )
}

export default function ModelManagement() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Model Management</h1>
          <p className="text-sm text-textMuted mt-1">Heterogeneous parallel-branch forecasting model — lifecycle & evaluation</p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" icon={UploadCloud}>Upload New Model</Button>
          <Button variant="primary" size="sm" icon={Play}>Start Training Run</Button>
        </div>
      </div>

      <div className="grid sm:grid-cols-2 gap-5">
        <ModelCard title="Current Model" m={MODEL_STATUS.current} badge="low" />
        <ModelCard title="Previous Model" m={MODEL_STATUS.previous} badge="neutral" />
      </div>

      <Card>
        <CardHeader title="Training & Validation Loss" subtitle="Latest training run — loss per 10 epochs" right={<History size={16} className="text-textMuted" />} />
        <div className="px-3 pb-4 pt-2">
          <ChartWrap height={280}>
            <LineChart data={MODEL_STATUS.trainingRuns} margin={{ top: 6, right: 8, left: -18, bottom: 0 }}>
              <CartesianGrid stroke={gridStroke} strokeDasharray="3 5" vertical={false} />
              <XAxis dataKey="epoch" tick={axisStyle} axisLine={{ stroke: gridStroke }} tickLine={false} />
              <YAxis tick={axisStyle} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Legend formatter={(v) => <span className="text-textSecondary text-xs">{v}</span>} iconType="circle" iconSize={8} />
              <Line type="monotone" dataKey="trainLoss" name="Train Loss" stroke="rgb(var(--c-accent))" strokeWidth={2.5} dot={{ r: 3 }} />
              <Line type="monotone" dataKey="valLoss" name="Validation Loss" stroke="rgb(var(--c-accent-2))" strokeWidth={2.5} strokeDasharray="6 4" dot={{ r: 3 }} />
            </LineChart>
          </ChartWrap>
        </div>
      </Card>
    </div>
  )
}
