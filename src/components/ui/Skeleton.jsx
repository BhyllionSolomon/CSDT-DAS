export function SkeletonBlock({ className = '' }) {
  return <div className={`skeleton animate-shimmer rounded-md ${className}`} />
}

export function SkeletonCard() {
  return (
    <div className="bg-surface border border-borderc rounded-xl p-5 space-y-3">
      <SkeletonBlock className="h-3 w-24" />
      <SkeletonBlock className="h-7 w-32" />
      <SkeletonBlock className="h-2 w-full" />
    </div>
  )
}

export function SkeletonRow() {
  return (
    <div className="flex items-center gap-4 py-3">
      <SkeletonBlock className="h-3 w-8" />
      <SkeletonBlock className="h-3 w-32" />
      <SkeletonBlock className="h-3 w-24" />
      <SkeletonBlock className="h-3 w-20" />
      <SkeletonBlock className="h-3 w-16" />
    </div>
  )
}
