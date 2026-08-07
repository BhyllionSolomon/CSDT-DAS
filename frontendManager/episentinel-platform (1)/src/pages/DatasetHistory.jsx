import { Download, UploadCloud, CheckCircle2, AlertTriangle } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { DATASET_HISTORY } from '../data/mockData'

export default function DatasetHistory() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Dataset History</h1>
          <p className="text-sm text-textMuted mt-1">Curated NCDC WER merges — validation and provenance trail</p>
        </div>
        <Button variant="secondary" size="sm" icon={UploadCloud}>Upload New Dataset</Button>
      </div>

      <Card>
        <CardHeader title="Validation Summary" subtitle="Most recent dataset merge" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-5">
          {[
            { label: 'Total Rows', value: DATASET_HISTORY[0].rows, tone: 'text-textPrimary' },
            { label: 'Valid Rows', value: DATASET_HISTORY[0].valid, tone: 'text-success' },
            { label: 'Invalid Rows', value: DATASET_HISTORY[0].invalid, tone: 'text-danger' },
            { label: 'Error Rate', value: `${((DATASET_HISTORY[0].invalid / DATASET_HISTORY[0].rows) * 100).toFixed(2)}%`, tone: 'text-warning' },
          ].map((s) => (
            <div key={s.label} className="bg-bgSunken rounded-lg p-4 border border-borderc">
              <div className={`font-mono text-xl font-semibold ${s.tone}`}>{s.value.toLocaleString?.() ?? s.value}</div>
              <div className="text-xs text-textMuted mt-1">{s.label}</div>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <CardHeader title="Dataset Versions" />
        <div className="overflow-x-auto">
          <table className="w-full text-sm mt-2">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-wide text-textMuted border-y border-borderc">
                <th className="px-5 py-3 font-semibold">Dataset ID</th>
                <th className="px-5 py-3 font-semibold">Name</th>
                <th className="px-5 py-3 font-semibold">Uploaded By</th>
                <th className="px-5 py-3 font-semibold">Date</th>
                <th className="px-5 py-3 font-semibold">Rows</th>
                <th className="px-5 py-3 font-semibold">Valid / Invalid</th>
                <th className="px-5 py-3 font-semibold">Status</th>
                <th className="px-5 py-3 font-semibold"></th>
              </tr>
            </thead>
            <tbody>
              {DATASET_HISTORY.map((d) => (
                <tr key={d.id} className="border-b border-borderc last:border-0 hover:bg-surfaceHover transition-colors">
                  <td className="px-5 py-3 font-mono text-accent text-xs">{d.id}</td>
                  <td className="px-5 py-3 font-medium text-textPrimary">{d.name}</td>
                  <td className="px-5 py-3 text-textSecondary">{d.uploadedBy}</td>
                  <td className="px-5 py-3 font-mono text-textSecondary">{d.date}</td>
                  <td className="px-5 py-3 font-mono">{d.rows.toLocaleString()}</td>
                  <td className="px-5 py-3">
                    <span className="text-success font-mono">{d.valid}</span>
                    <span className="text-textMuted"> / </span>
                    <span className="text-danger font-mono">{d.invalid}</span>
                  </td>
                  <td className="px-5 py-3">
                    <Badge variant={d.status === 'Validated' ? 'low' : 'medium'} dot>
                      {d.status === 'Validated' ? <CheckCircle2 size={11} className="inline mr-0.5" /> : <AlertTriangle size={11} className="inline mr-0.5" />}
                      {d.status}
                    </Badge>
                  </td>
                  <td className="px-5 py-3"><button className="text-textMuted hover:text-accent transition-colors"><Download size={15} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
