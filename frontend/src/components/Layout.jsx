import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navItems = [
    { to: '/', label: 'Dashboard', end: true },
    { to: '/students', label: 'Students' },
    { to: '/bulk-upload-students', label: 'Bulk Upload Students' },
    { to: '/courses', label: 'Courses' },
    { to: '/create-course', label: 'Create Course' },
    { to: '/sessions', label: 'Academic Sessions' },
    { to: '/enter-result', label: 'Enter Result' },
    { to: '/bulk-enter-results', label: 'Bulk Enter Results' },
    { to: '/upload-results-csv', label: 'Upload Results' },
    { to: '/verification', label: 'Result Verification' },
    { to: '/register-courses', label: 'Student Course Registration' },
]

function Layout() {
    const { user, logout } = useAuth()

    return (
        <div className="min-h-screen bg-slate-100 flex">
            <aside className="w-64 bg-slate-900 text-white flex flex-col">
                <div className="p-6 border-b border-slate-700">
                    <h1 className="text-xl font-bold">CSDT-DAS</h1>
                    <p className="text-slate-400 text-sm mt-1">Academic Results</p>
                </div>
                <nav className="flex-1 p-4 space-y-1">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) =>
                                `block px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                    isActive
                                        ? 'bg-blue-600 text-white'
                                        : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                                }`
                            }
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>
                <div className="p-4 border-t border-slate-700">
                    <p className="text-sm font-medium">{user?.fullName}</p>
                    <p className="text-xs text-slate-400 mb-3">{user?.role}</p>
                    <button
                        onClick={logout}
                        className="text-xs text-slate-300 hover:text-white underline"
                    >
                        Sign out
                    </button>
                </div>
            </aside>

            <main className="flex-1 p-8">
                <Outlet />
            </main>
        </div>
    )
}

export default Layout