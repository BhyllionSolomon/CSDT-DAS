import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { Settings, LogOut, ChevronRight } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getGroupsForRole, GROUP_ICONS, LAB_ROLES } from '../navConfig'
import Logo from './Logo'
import ThemeSwitcher from './ThemeSwitcher'

function NavGroup({ group }) {
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
                <ChevronRight
                    size={14}
                    className="transition-transform duration-300 ease-out"
                    style={{ transform: open ? 'rotate(90deg)' : 'rotate(0deg)' }}
                />
            </button>
            <div
                className="overflow-hidden transition-all duration-300 ease-out"
                style={{ maxHeight: open ? `${group.items.length * 40 + 16}px` : '0px', opacity: open ? 1 : 0 }}
            >
                <div className="space-y-0.5 pb-2 pl-4">
                    {group.items.map((item) =>
                        item.disabled ? (
                            <div key={item.to}
                                 className="flex justify-between items-center px-4 py-1.5 text-sm text-white/25 cursor-not-allowed">
                                {item.label}
                                <span className="text-[10px] uppercase bg-white/10 text-white/40 px-1.5 py-0.5 rounded">soon</span>
                            </div>
                        ) : (
                            <NavLink key={item.to} to={item.to} end={item.to === '/'}
                                     className={({ isActive }) =>
                                         `block px-4 py-1.5 rounded-lg text-sm font-medium transition-all btn-press ${
                                             isActive
                                                 ? 'accent-gradient text-white shadow-lg shadow-black/20'
                                                 : 'text-white/60 hover:bg-white/5 hover:text-white'
                                         }`
                                     }>
                                {item.label}
                            </NavLink>
                        )
                    )}
                </div>
            </div>
        </div>
    )
}

function Layout() {
    const { user, logout } = useAuth()
    const groups = getGroupsForRole(user?.role)
    const initials = (user?.fullName || '?').split(' ').map((n) => n[0]).slice(0, 2).join('')
    const subtitle = LAB_ROLES.includes(user?.role) ? 'Laboratory' : 'Academic Results'

    return (
        <div className="min-h-screen flex">
            <aside className="w-72 theme-sidebar text-white flex flex-col shrink-0 max-h-screen">
                <div className="p-6 flex items-center gap-3 shrink-0">
                    <Logo className="w-10 h-10" />
                    <div>
                        <h1 className="text-lg font-bold">CSDT-DAS</h1>
                        <p className="text-white/40 text-xs">{subtitle}</p>
                    </div>
                </div>

                <nav className="flex-1 py-2 overflow-y-auto min-h-0"
                     style={{ scrollbarWidth: 'thin', scrollbarColor: 'rgba(255,255,255,0.15) transparent' }}>
                    {groups.map((group) => (
                        <NavGroup key={group.label} group={group} />
                    ))}
                </nav>

                <div className="p-4 mx-3 mb-3 rounded-xl bg-white/5 space-y-3 shrink-0">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full accent-gradient flex items-center justify-center text-xs font-bold shrink-0">
                            {initials}
                        </div>
                        <div className="min-w-0">
                            <p className="text-sm font-medium truncate">{user?.fullName}</p>
                            <p className="text-xs text-white/40">{user?.role}</p>
                            <NavLink to="/my-account" className="text-xs text-white/60 hover:text-white underline">
                                My Account
                            </NavLink>
                        </div>
                    </div>
                    <div className="flex items-center justify-between">
                        <ThemeSwitcher />
                        <button onClick={logout} className="text-white/50 hover:text-white btn-press" title="Sign out">
                            <LogOut size={16} />
                        </button>
                    </div>
                </div>
            </aside>

            <main className="flex-1 p-8 overflow-y-auto animate-in">
                <Outlet />
            </main>
        </div>
    )
}

export default Layout