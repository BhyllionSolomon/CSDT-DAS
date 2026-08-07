export default function Card({ children, className = '', hover = true, as: Tag = 'div', ...rest }) {
  return (
    <Tag
      className={`bg-surface border border-borderc rounded-xl shadow-card ${hover ? 'transition-all duration-300 hover:shadow-elevated hover:-translate-y-0.5 hover:border-accent2/40' : ''} ${className}`}
      {...rest}
    >
      {children}
    </Tag>
  )
}

export function CardHeader({ title, subtitle, right }) {
  return (
    <div className="flex items-start justify-between gap-4 px-5 pt-5">
      <div>
        <h3 className="font-display text-[15px] font-semibold text-textPrimary tracking-tight">{title}</h3>
        {subtitle && <p className="text-xs text-textMuted mt-0.5">{subtitle}</p>}
      </div>
      {right && <div className="shrink-0">{right}</div>}
    </div>
  )
}
