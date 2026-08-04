import { useLocation, Link } from 'react-router-dom'
import { ChevronRight, Home } from 'lucide-react'

const LABELS = {
  '': 'Dashboard',
  surveillance: 'Daily Surveillance',
  records: 'Disease Records',
  laboratory: 'Laboratory Results',
  investigation: 'Case Investigation',
  forecasts: 'Forecasts',
  hotspots: 'Hotspots',
  maps: 'Maps',
  analytics: 'Analytics',
  'decision-support': 'Decision Support',
  'dataset-history': 'Dataset History',
  'model-management': 'Model Management',
  settings: 'Settings',
}

export default function Breadcrumbs() {
  const { pathname } = useLocation()
  const parts = pathname.split('/').filter(Boolean)

  return (
    <div className="flex items-center gap-1.5 text-[13px] text-textMuted">
      <Link to="/" className="flex items-center gap-1 hover:text-textPrimary transition-colors">
        <Home size={13} />
      </Link>
      {parts.length === 0 && (
        <>
          <ChevronRight size={13} />
          <span className="text-textPrimary font-medium">Dashboard</span>
        </>
      )}
      {parts.map((p, i) => (
        <span key={i} className="flex items-center gap-1.5">
          <ChevronRight size={13} />
          <span className={i === parts.length - 1 ? 'text-textPrimary font-medium' : ''}>{LABELS[p] || p}</span>
        </span>
      ))}
    </div>
  )
}
