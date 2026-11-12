import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
    Settings, LogOut, ChevronRight, Lock, ExternalLink, Menu, X, Home, CalendarDays, UserCheck, LineChart,
    ClipboardCheck, FlaskConical, Wallet, MessageSquare, FileSpreadsheet,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getGroupsForRole, GROUP_ICONS, LAB_ROLES } from '../navConfig'
import { getMyClearance } from '../services/clearanceService'
import Logo from './Logo'
import ThemeSwitcher from './ThemeSwitcher'

const RELATED_SYSTEMS = [
    { label: 'SIWES (Industrial Training)', url: 'https://ems-frontend-fv32.onrender.com' },
]

// Four shortcuts for the bottom bar on phones.
const QUICK = {
    STUDENT: [['/', 'Home', Home], ['/timetable', 'Timetable', CalendarDays], ['/my-attendance', 'Attendance', UserCheck], ['/my-progress', 'Progress', LineChart]],
    LECTURER: [['/', 'Home', Home], ['/timetable', 'Timetable', CalendarDays], ['/attendance', 'Attendance', UserCheck], ['/my-feedback', 'Feedback', MessageSquare]],
    ADJUNCT: [['/', 'Home', Home], ['/timetable', 'Timetable', CalendarDays], ['/attendance', 'Attendance', UserCheck], ['/my-feedback', 'Feedback', MessageSquare]],
    LEVEL_ADVISER: [['/', 'Home', Home], ['/reports', 'Reports', FileSpreadsheet], ['/attendance', 'Attendance', UserCheck], ['/timetable', 'Timetable', CalendarDays]],
    HOD: [['/', 'Home', Home], ['/registration-approvals', 'Approvals', ClipboardCheck], ['/timetable-admin', 'Timetable', CalendarDays], ['/reports', 'Reports', FileSpreadsheet]],
    ADMIN: [['/', 'Home', Home], ['/registration-approvals', 'Approvals', ClipboardCheck], ['/timetable-admin', 'Timetable', CalendarDays], ['/reports', 'Reports', FileSpreadsheet]],
    LAB_OFFICER: [['/', 'Home', Home], ['/lab-manuals', 'Sales', FlaskConical], ['/lab-finance', 'Finance', Wallet]],
    TECHNICAL_STAFF: [['/', 'Home', Home], ['/lab-manuals', 'Sales', FlaskConical]],
}

function NavGroup({ group, locked, onNavigate }) {
    const [open, setOpen] = useState(false)
    const Icon = GROUP_ICONS[group.label] || Settings

    return (
        <div className="mb-1 px-3">
            <button
                onClick={() => setOpen(!open)}
                className="w-full flex items-center gap-2 px-3 py-2.5 text-xs font-semibold uppercase tracking-wide text-white/50 hover:text-white btn-press rounded-lg hover:bg-white/5"
            >
                <Icon size={15} />
                <span className="flex-1 text-left">{group.label}</span>
                <ChevronRight size={14} className="transition-transform duration-300 ease-out" style={{ transform: open ? 'rotate(90deg)' : 'rotate(0deg)' }} />
            </button>
            <div className="overflow-hidden transition-all duration-300 ease-out"
                 style={{ maxHeight: open ? `${group.items.length * 40 + 16}px` : '0px', opacity: open ? 1 : 0 }}>
                <div className="space-y-0.5 pb-2 pl-4">
                    {group.items.map((item) => {
                        if (item.disabled) {
                            return (
                                <div key={item.to} className="flex justify-between items-center px-4 py-1.5 text-sm text-white/25 cursor-not-allowed">
                                    {item.label}
                                    <span className="text-[10px] uppercase bg-white/10 text-white/40 px-1.5 py-0.5 rounded">soon</span>
                                </div>
                            )
                        }
                        const isLocked = locked && item.gated
                        return (
                            <NavLink key={item.to} to={isLocked ? '/my-registration' : item.to} end={item.to === '/'} onClick={onNavigate}
                                     className={({ isActive }) =>
                                         `flex items-center justify-between px-4 py-1.5 rounded-lg text-sm font-medium transition-all btn-press ${
                                             isLocked ? 'text-white/35 hover:bg-white/5'
                                                 : isActive ? 'accent-gradient text-white shadow-lg shadow-black/20'
                                                     : 'text-white/60 hover:bg-white/5 hover:text-white'
                                         }`}>
                                {item.label}
                                {isLocked && <Lock size={12} />}
                            </NavLink>
                        )
                    })}
                </div>
            </div>
        </div>
    )
}

function Sidebar({ groups, locked, user, isLab, logout, onNavigate }) {
    const initials = (user?.fullName || '?').split(' ').map((n) => n[0]).slice(0, 2).join('')

    return (
        <aside className="w-72 theme-sidebar text-white flex flex-col h-full">
            <div className="p-6 flex items-center gap-3 shrink-0">
                <Logo className="w-10 h-10" />
                <div>
                    <h1 className="text-lg font-bold">CSDT-DAS</h1>
                    <p className="text-white/40 text-xs">{isLab ? 'Laboratory' : 'Academic Results'}</p>
                </div>
            </div>

            <nav className="flex-1 py-2 overflow-y-auto min-h-0" style={{ scrollbarWidth: 'thin', scrollbarColor: 'rgba(255,255,255,0.15) transparent' }}>
                {groups.map((group) => <NavGroup key={group.label} group={group} locked={locked} onNavigate={onNavigate} />)}
            </nav>

            {!isLab && (
                <div className="px-6 py-3 border-t border-white/10 shrink-0">
                    <p className="text-[10px] font-semibold uppercase tracking-wide text-white/40 mb-1">Related systems</p>
                    {RELATED_SYSTEMS.map((s) => (
                        <a key={s.url} href={s.url} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 py-1 text-sm text-white/70 hover:text-white">
                            <ExternalLink size={13} /> {s.label}
                        </a>
                    ))}
                </div>
            )}

            <div className="p-4 mx-3 mb-3 rounded-xl bg-white/5 space-y-3 shrink-0">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full accent-gradient flex items-center justify-center text-xs font-bold shrink-0">{initials}</div>
                    <div className="min-w-0">
                        <p className="text-sm font-medium truncate">{user?.fullName}</p>
                        <p className="text-xs text-white/40">{user?.role}</p>
                        <NavLink to="/my-account" onClick={onNavigate} className="text-xs text-white/60 hover:text-white underline">My Account</NavLink>
                    </div>
                </div>
                <div className="flex items-center justify-between">
                    <ThemeSwitcher />
                    <button onClick={logout} className="text-white/50 hover:text-white btn-press" title="Sign out"><LogOut size={16} /></button>
                </div>
            </div>
        </aside>
    )
}

function Layout() {
    const { user, logout } = useAuth()
    const location = useLocation()
    const [clearance, setClearance] = useState(null)
    const [drawer, setDrawer] = useState(false)

    const groups = getGroupsForRole(user?.role)
    const isLab = LAB_ROLES.includes(user?.role)
    const quick = QUICK[user?.role] || [['/', 'Home', Home]]

    useEffect(() => { setDrawer(false) }, [location.pathname])

    // Students: find out whether the H.O.D has approved their registration.
    useEffect(() => {
        if (user?.role !== 'STUDENT') return
        getMyClearance().then(setClearance).catch(() => setClearance(null))
    }, [user?.role, location.pathname])

    const locked = user?.role === 'STUDENT' && !!clearance && clearance.gateEnabled && !clearance.cleared
    const sidebar = (onNavigate) => <Sidebar groups={groups} locked={locked} user={user} isLab={isLab} logout={logout} onNavigate={onNavigate} />

    return (
        <div className="lg:flex" style={{ minHeight: '100dvh' }}>

            {/* computers: fixed side menu */}
            <div className="hidden lg:block sticky top-0 h-screen shrink-0">{sidebar()}</div>

            {/* phones: top bar */}
            <header className="lg:hidden sticky top-0 z-30 theme-sidebar text-white flex items-center gap-3 px-3"
                    style={{ height: 'calc(3.5rem + env(safe-area-inset-top))', paddingTop: 'env(safe-area-inset-top)' }}>
                <button onClick={() => setDrawer(true)} aria-label="Open menu" className="p-2 -ml-1 btn-press"><Menu size={22} /></button>
                <Logo className="w-8 h-8" />
                <span className="font-bold flex-1">CSDT-DAS</span>
                <ThemeSwitcher />
            </header>

            {/* phones: slide-in menu */}
            {drawer && (
                <div className="lg:hidden fixed inset-0 z-50 flex">
                    <div className="h-full max-w-[85%] shadow-2xl overflow-hidden relative">
                        {sidebar(() => setDrawer(false))}
                        <button onClick={() => setDrawer(false)} aria-label="Close menu" className="absolute top-5 right-4 text-white/60"><X size={20} /></button>
                    </div>
                    <button className="flex-1 bg-black/50" aria-label="Close menu" onClick={() => setDrawer(false)} />
                </div>
            )}

            <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 pb-24 lg:pb-8 animate-in">
                {locked && (
                    <div className="no-print mb-6 px-4 py-3 rounded-lg bg-amber-50 text-amber-700 text-sm">
                        {clearance.message}{' '}
                        <NavLink to="/my-registration" className="underline font-medium">Open Registration &amp; Payment</NavLink>
                    </div>
                )}
                <Outlet />
            </main>

            {/* phones: bottom shortcuts */}
            <nav className="no-print lg:hidden fixed bottom-0 inset-x-0 z-30 theme-sidebar text-white grid"
                 style={{ gridTemplateColumns: `repeat(${quick.length}, 1fr)`, paddingBottom: 'env(safe-area-inset-bottom)' }}>
                {quick.map(([to, label, Icon]) => (
                    <NavLink key={to} to={to} end={to === '/'}
                             className={({ isActive }) => `flex flex-col items-center gap-0.5 py-2 text-[10px] font-medium ${isActive ? 'text-white' : 'text-white/50'}`}>
                        <Icon size={20} />
                        {label}
                    </NavLink>
                ))}
            </nav>
        </div>
    )
}

export default Layout