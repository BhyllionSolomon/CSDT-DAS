import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function StudentDashboard() {
    const { user } = useAuth()

    if (!user.studentId) {
        return (
            <div>
                <h2 className="text-2xl font-bold text-slate-800 mb-2">Welcome, {user.fullName}</h2>
                <div className="bg-amber-50 border border-amber-300 rounded-lg p-4 text-sm text-amber-800">
                    Your account isn't linked to a student record yet. If you've just been
                    added to the department's roster, try signing up again with your matric
                    number, or contact your Level Adviser.
                </div>
            </div>
        )
    }

    const links = [
        { label: 'Register Courses', to: '/register-courses', desc: 'Carry-overs first, then this semester' },
        { label: 'My Results', to: `/students/${user.studentId}/results`, desc: 'Scores, CGPA, academic history' },
    ]

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-1">Welcome, {user.fullName}</h2>
            <p className="text-sm text-slate-500 mb-6">{user.matricNumber}</p>

            <div className="grid grid-cols-2 gap-4">
                {links.map((l) => (
                    <Link
                        key={l.to}
                        to={l.to}
                        className="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow"
                    >
                        <p className="font-semibold text-slate-800">{l.label}</p>
                        <p className="text-sm text-slate-500">{l.desc}</p>
                    </Link>
                ))}
            </div>
        </div>
    )
}

export default StudentDashboard