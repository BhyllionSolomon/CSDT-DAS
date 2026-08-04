import { Search, UserRound, MapPin, Calendar } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import { DISEASE_RECORDS } from '../data/mockData'

const cases = DISEASE_RECORDS.slice(0, 6)

export default function CaseInvestigation() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Case Investigation</h1>
        <p className="text-sm text-textMuted mt-1">Field investigation queue for confirmed and suspected cases</p>
      </div>

      <Card className="p-4">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-textMuted" />
          <input placeholder="Search by case ID or contact name…" className="w-full bg-bgSunken border border-borderc rounded-lg pl-9 pr-3 py-2.5 text-sm focus-ring" />
        </div>
      </Card>

      <div className="grid lg:grid-cols-2 gap-5">
        {cases.map((c, i) => (
          <Card key={c.id} className="p-5 animate-riseIn" style={{ animationDelay: `${i * 50}ms` }}>
            <div className="flex items-center justify-between mb-3">
              <span className="font-mono text-xs text-accent">{c.id}</span>
              <Badge variant={c.status === 'Confirmed' ? 'high' : 'medium'}>{c.status}</Badge>
            </div>
            <div className="font-display font-semibold text-textPrimary">{c.disease}</div>
            <div className="flex items-center gap-4 mt-3 text-xs text-textMuted">
              <span className="flex items-center gap-1"><MapPin size={12} /> {c.lga}, {c.state}</span>
              <span className="flex items-center gap-1"><Calendar size={12} /> {c.reportedDate}</span>
            </div>
            <div className="flex items-center gap-2 mt-3 pt-3 border-t border-borderc text-xs text-textSecondary">
              <UserRound size={13} /> Assigned investigator: {c.officer}
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}
