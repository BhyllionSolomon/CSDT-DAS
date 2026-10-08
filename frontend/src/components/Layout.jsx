import { useState } from 'react'


import { NavLink, Outlet } from 'react-router-dom'
import {
    LayoutDashboard, GraduationCap, BookOpen, Users, UserCog,
    ClipboardCheck, FileSpreadsheet, Settings, LogOut, ChevronRight, Video
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import Logo from './Logo'
import ThemeSwitcher from './ThemeSwitcher'


const hodGroups = [
    { label: 'Department', items: [
            { to: '/programme-overview', label: 'Programme Overview' },
            { to: '/sessions', label: 'Academic Sessions' },
            { to: '/students-per-session', label: 'Students Per Session' },
        ]},
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ]},
    { label: 'Students', items: [
            { to: '/students', label: 'All Students' },
            { to: '/bulk-upload-students', label: 'Bulk Upload Students' },
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/registration-status', label: 'Registration Status' },
        ]},
    { label: 'Courses', items: [
            { to: '/courses', label: 'All Courses' },
            { to: '/create-course', label: 'Create Course' },
            { to: '/bulk-upload-courses', label: 'Bulk Upload Courses' },
            { to: '/set-unit-requirements', label: 'Set Required Units' },
        ]},
    { label: 'Lecturers', items: [
            { to: '/course-allocations', label: 'Course Allocations' },
            { to: '/users', label: 'Manage Lecturers' },
            { to: '/teaching', label: 'Teaching Activity' },
        ]},
    { label: 'Level Advisers', items: [
            { to: '/advisers', label: 'Manage Advisers' },
        ]},
    { label: 'Results', items: [
            { to: '/enter-result', label: 'Enter Result' },
            { to: '/bulk-enter-results', label: 'Bulk Enter Results' },
            { to: '/upload-results-csv', label: 'Upload Results' },
        ]},
    { label: 'Exams', items: [
            { to: '/verification', label: 'Result Verification' },
        ]},
]



const lecturerGroups = [
    { label: 'Courses', items: [
            { to: '/', label: 'My Courses' },
            { to: '/claim-courses', label: 'Claim Course' },
        ]},
    { label: 'Teaching', items: [
            { to: '/teaching', label: 'Materials & Online Classes' },
        ]},
    { label: 'Students', items: [
            { to: '/lecturer-students', label: 'My Students' },
        ]},
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'Take Attendance', disabled: true },
        ]},
    { label: 'Continuous Assessment', items: [
            { to: '/ca/practicals', label: 'Practicals', disabled: true },
            { to: '/ca/assignments', label: 'Assignments', disabled: true },
            { to: '/ca/presentation', label: 'Class Presentation', disabled: true },
            { to: '/ca/test', label: 'Test', disabled: true },
        ]},
    { label: 'Exam Scoresheet', items: [
            { to: '/exam-scoresheet', label: 'Enter Exam Scores' },
        ]},
]

const adviserGroups = [
    { label: 'Programme', items: [{ to: '/', label: 'My Programme' }] },
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ]},
    { label: 'Students', items: [
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/registration-status', label: 'Registration Status' },
        ]},
    { label: 'Results', items: [{ to: '/verification', label: 'Result Verification' }] },
]

const studentGroups = [
    { label: 'Academics', items: [
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/my-carryovers', label: 'Carry-Over Courses' },
        ]},
    { label: 'Learning', items: [
            { to: '/my-learning', label: 'Class Materials & Online Classes' },
        ]},
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'My Attendance', disabled: true },
        ]},
    { label: 'Continuous Assessment', items: [
            { to: '/my-ca', label: 'My C.A. Scores', disabled: true },
        ]},
    { label: 'My Results', items: [
            { to: '/upload-my-results', label: 'Upload & Analyse Results' },
        ]},

]

function getGroupsForRole(role) {
    switch (role) {
        case 'HOD':
        case 'ADMIN':
            return hodGroups
        case 'LECTURER':
        case 'ADJUNCT':
            return lecturerGroups
        case 'LEVEL_ADVISER':
            return adviserGroups
        case 'STUDENT':
            return studentGroups
        default:
            return []
    }
}

const GROUP_ICONS = {
    'Department': LayoutDashboard,
    'Students': GraduationCap,
    'Courses': BookOpen,
    'Lecturers': Users,
    'Level Advisers': UserCog,
    'Results': ClipboardCheck,
    'Exams': FileSpreadsheet,
    'Attendance': ClipboardCheck,
    'Continuous Assessment': ClipboardCheck,
    'Exam Scoresheet': FileSpreadsheet,
    'Programme': BookOpen,
    'Academics': GraduationCap,
    'Teaching': Video, 'Learning': Video,
    'Reports': FileSpreadsheet,
}

function NavGroup({ group }) {
    const [open, setOpen] = useState(false)
    const Icon = GROUP_ICONS[group.label] || Settings

    return (
        <div className="mb-1 px-3">
            <button
                onClick={() => setOpen(!open)}
                className="w-full flex items-center gap-2 px-3 py-2.5 text-xs font-semibold uppercase tracking-wide opacity-60 hover:opacity-100 btn-press rounded-lg hover:bg-[var(--sidebar-hover)]"
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
                                 className="flex justify-between items-center px-4 py-1.5 text-sm opacity-25 cursor-not-allowed">
                                {item.label}
                                <span className="text-[10px] uppercase bg-[var(--sidebar-hover)] opacity-60 px-1.5 py-0.5 rounded">soon</span>
                            </div>
                        ) : (
                            <NavLink key={item.to} to={item.to} end={item.to === '/'}
                                     className={({ isActive }) =>
                                         `block px-4 py-1.5 rounded-lg text-sm font-medium transition-all btn-press ${
                                             isActive
                                                 ? 'accent-gradient text-white shadow-lg shadow-black/20'
                                                 : 'opacity-60 hover:bg-[var(--sidebar-hover)] hover:opacity-100'
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

    return (
        <div className="min-h-screen flex">
            <aside className="w-72 theme-sidebar flex flex-col shrink-0 max-h-screen">
                <div className="p-6 flex items-center gap-3 shrink-0">
                    <Logo className="w-10 h-10" />
                    <div>
                        <h1 className="text-lg font-bold">CSDT-DAS</h1>
                        <p className="opacity-40 text-xs">Academic Results</p>
                    </div>
                </div>

                <nav className="flex-1 py-2 overflow-y-auto min-h-0" style={{ scrollbarWidth: 'thin', scrollbarColor: 'rgba(255,255,255,0.15) transparent' }}>
                    {groups.map((group) => (
                        <NavGroup key={group.label} group={group} />
                    ))}
                </nav>

                <div className="p-4 mx-3 mb-3 rounded-xl bg-[var(--sidebar-hover)] space-y-3 shrink-0">
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full accent-gradient flex items-center justify-center text-xs font-bold shrink-0">
                            {initials}
                        </div>
                        <div className="min-w-0">
                            <p className="text-sm font-medium truncate">{user?.fullName}</p>
                            <p className="text-xs opacity-40">{user?.role}</p>
                        </div>
                    </div>
                    <div className="flex items-center justify-between">
                        <ThemeSwitcher />
                        <button onClick={logout} className="opacity-50 hover:opacity-100 btn-press" title="Sign out">
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