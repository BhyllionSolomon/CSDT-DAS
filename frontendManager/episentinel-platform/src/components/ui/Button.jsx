const VARIANTS = {
  primary: 'bg-accent text-white hover:bg-accent/90 shadow-sm',
  secondary: 'bg-surface border border-borderc text-textPrimary hover:bg-surfaceHover',
  ghost: 'text-textSecondary hover:text-textPrimary hover:bg-surfaceHover',
  danger: 'bg-danger text-white hover:bg-danger/90',
  outlineAccent2: 'border border-accent2/40 text-accent2 hover:bg-accent2/10',
}

export default function Button({ children, variant = 'primary', size = 'md', icon: Icon, className = '', ...rest }) {
  const sizes = { sm: 'text-xs px-3 py-1.5 gap-1.5', md: 'text-sm px-4 py-2.5 gap-2', lg: 'text-sm px-5 py-3 gap-2' }
  return (
    <button
      className={`focus-ring inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none ${VARIANTS[variant]} ${sizes[size]} ${className}`}
      {...rest}
    >
      {Icon && <Icon size={16} strokeWidth={2.25} />}
      {children}
    </button>
  )
}
