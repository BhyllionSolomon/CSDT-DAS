import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, ClipboardList, Database, FlaskConical, Search,
  LineChart, Flame, Map, BarChart3, Lightbulb, History, Cpu, Settings,
  LogOut, ShieldHalf, ChevronsLeft, ChevronsRight,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/surveillance', label: 'Daily Surveillance', icon: ClipboardList },
  { to: '/records', label: 'Disease Records', icon: Database },
  { to: '/laboratory', label: 'Laboratory Results', icon: FlaskConical },
  { to: '/investigation', label: 'Case Investigation', icon: Search },
  { to: '/forecasts', label: 'Forecasts', icon: LineChart },
  { to: '/hotspots', label: 'Hotspots', icon: Flame },
  { to: '/maps', label: 'Maps', icon: Map },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/decision-support', label: 'Decision Support', icon: Lightbulb },
  { to: '/dataset-history', label: 'Dataset History', icon: History },
  { to: '/model-management', label: 'Model Management', icon: Cpu },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export default function Sidebar({ collapsed, setCollapsed }) {
  const { logout } = useAuth()

  return (
    <aside
      className={`hidden md:flex flex-col shrink-0 bg-surface border-r border-borderc h-screen sticky top-0 transition-all duration-300 ${collapsed ? 'w-[76px]' : 'w-[268px]'}`}
    >
      <div className="flex items-center gap-2.5 px-5 h-16 border-b border-borderc shrink-0">
        <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-accent to-accent2 flex items-center justify-center shrink-0 shadow-glow">
          <ShieldHalf size={17} className="text-white" strokeWidth={2.5} />
        </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <div className="font-display font-bold text-[13.5px] text-textPrimary leading-tight whitespace-nowrap">EpiSentinel</div>
            <div className="text-[10px] text-textMuted whitespace-nowrap tracking-wide uppercase">NCDC Surveillance AI</div>
          </div>
        )}
      </div>

      <nav className="flex-1 overflow-y-auto py-3 px-3 space-y-0.5">
        {NAV.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-[13px] font-medium transition-colors duration-150 focus-ring ${
                isActive
                  ? 'bg-accent/12 text-accent'
                  : 'text-textSecondary hover:bg-surfaceHover hover:text-textPrimary'
              }`
            }
            title={collapsed ? label : undefined}
          >
            {({ isActive }) => (
              <>
                {isActive && <span className="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-full bg-accent" />}
                <Icon size={17} strokeWidth={2.1} className="shrink-0" />
                {!collapsed && <span className="whitespace-nowrap">{label}</span>}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-borderc p-3 space-y-0.5">
        <button
          onClick={logout}
          className="w-full flex items-center gap-3 rounded-lg px-3 py-2.5 text-[13px] font-medium text-textSecondary hover:bg-danger/10 hover:text-danger transition-colors duration-150 focus-ring"
        >
          <LogOut size={17} strokeWidth={2.1} />
          {!collapsed && <span>Logout</span>}
        </button>
        <button
          onClick={() => setCollapsed((c) => !c)}
          className="w-full flex items-center gap-3 rounded-lg px-3 py-2.5 text-[13px] font-medium text-textMuted hover:bg-surfaceHover hover:text-textPrimary transition-colors duration-150 focus-ring"
        >
          {collapsed ? <ChevronsRight size={17} /> : <ChevronsLeft size={17} />}
          {!collapsed && <span>Collapse</span>}
        </button>
      </div>
    </aside>
  )
}
