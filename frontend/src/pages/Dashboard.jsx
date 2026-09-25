import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Users, BookOpen, Calendar, LayoutGrid, UserPlus, UploadCloud, ClipboardCheck } from 'lucide-react'
import { getAllStudents } from '../services/studentService'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'

function Dashboard() {
    const [counts, setCounts] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        Promise.all([getAllStudents(), getAllCourses(), getAllSessions()])
            .then(([students, courses, sessions]) => {
                setCounts({ students: students.length, courses: courses.length, sessions: sessions.length })
            })
            .finally(() => setLoading(false))
    }, [])

    const cards = [
        { label: 'Students', value: counts?.students, to: '/students', icon: Users },
        { label: 'Courses', value: counts?.courses, to: '/courses', icon: BookOpen },
        { label: 'Academic Sessions', value: counts?.sessions, to: '/sessions', icon: Calendar },
    ]

    const quickLinks = [
        { label: 'Programme Overview', to: '/programme-overview', desc: 'Per-programme, per-level monitoring', icon: LayoutGrid },
        { label: 'Course Registration', to: '/register-courses', desc: 'Students register semester courses', icon: UserPlus },
        { label: 'Course Allocations', to: '/course-allocations', desc: 'Upload/view lecturer assignments', icon: UploadCloud },
        { label: 'Bulk Enter Results', to: '/bulk-enter-results', desc: 'Grid-style result entry for a course', icon: ClipboardCheck },
        { label: 'Result Verification', to: '/verification', desc: 'Class-wide review with flags', icon: ClipboardCheck },
    ]

    return (
        <div>
            <h2 className="text-2xl font-bold mb-1">Dashboard</h2>
            <p className="theme-text-muted text-sm mb-6">Welcome back. Here's your department at a glance.</p>

            {loading ? (
                <div className="grid grid-cols-3 gap-4 mb-8">
                    {[1, 2, 3].map((i) => <div key={i} className="card h-28 skeleton" />)}
                </div>
            ) : (
                <div className="grid grid-cols-3 gap-4 mb-8">
                    {cards.map((c) => (
                        <Link key={c.label} to={c.to} className="card card-hover card-top-accent p-6 animate-in">
                            <div className="flex items-start justify-between">
                                <div>
                                    <p className="text-3xl font-bold">{c.value}</p>
                                    <p className="text-sm theme-text-muted mt-1">{c.label}</p>
                                </div>
                                <div className="w-10 h-10 rounded-xl accent-gradient flex items-center justify-center text-white">
                                    <c.icon size={18} />
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            )}

            <h3 className="text-sm font-semibold theme-text-muted uppercase mb-3">Quick Access</h3>
            <div className="grid grid-cols-2 gap-3">
                {quickLinks.map((q) => (
                    <Link key={q.to} to={q.to} className="card card-hover p-4 flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg flex items-center justify-center shrink-0" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            <q.icon size={16} style={{ color: 'var(--accent)' }} />
                        </div>
                        <div>
                            <p className="font-medium">{q.label}</p>
                            <p className="text-sm theme-text-muted">{q.desc}</p>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    )
}

export default Dashboard