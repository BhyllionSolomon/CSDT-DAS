const VARIANTS = {
  critical: 'bg-danger/15 text-danger border-danger/30',
  high: 'bg-danger/10 text-danger border-danger/25',
  medium: 'bg-warning/15 text-warning border-warning/30',
  low: 'bg-success/15 text-success border-success/30',
  info: 'bg-info/15 text-info border-info/30',
  neutral: 'bg-textMuted/10 text-textSecondary border-borderc',
  accent: 'bg-accent/15 text-accent border-accent/30',
}

export default function Badge({ children, variant = 'neutral', dot = false, className = '' }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide ${VARIANTS[variant] || VARIANTS.neutral} ${className}`}>
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}
