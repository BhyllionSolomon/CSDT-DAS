import { Syringe, Droplets, FlaskConical, Users2, ShieldCheck, Megaphone, Hospital, ArrowRight } from 'lucide-react'
import Card, { CardHeader } from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { RECOMMENDATIONS } from '../data/mockData'

const ICONS = {
  'Deploy Mass Drug Administration': Syringe,
  'Accelerate Vaccination Campaign': ShieldCheck,
  'Reinforce Laboratory Capacity': FlaskConical,
  'Deploy Rapid Surveillance Team': Users2,
  'Increase Chlorine Distribution': Droplets,
  'Launch Public Awareness Campaign': Megaphone,
  'Hospital Preparedness Review': Hospital,
}

const PRIORITY_VARIANT = { High: 'high', Medium: 'medium', Low: 'low' }

export default function DecisionSupport() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Decision Support</h1>
        <p className="text-sm text-textMuted mt-1">AI-generated intervention recommendations, ranked by projected impact</p>
      </div>

      <div className="grid lg:grid-cols-2 gap-5">
        {RECOMMENDATIONS.map((r, i) => {
          const Icon = ICONS[r.title] || ShieldCheck
          return (
            <Card key={i} className="p-5 animate-riseIn" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="h-10 w-10 rounded-lg bg-accent/10 text-accent flex items-center justify-center shrink-0">
                  <Icon size={18} />
                </div>
                <Badge variant={PRIORITY_VARIANT[r.priority]}>{r.priority} priority</Badge>
              </div>
              <div className="font-display font-semibold text-textPrimary">{r.title}</div>
              <div className="text-xs text-accent2 font-medium mt-1">{r.target}</div>
              <p className="text-sm text-textSecondary mt-2 leading-relaxed">{r.rationale}</p>
              <div className="flex gap-2 mt-4">
                <Button variant="primary" size="sm" icon={ArrowRight}>Action Plan</Button>
                <Button variant="secondary" size="sm">Dismiss</Button>
              </div>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
