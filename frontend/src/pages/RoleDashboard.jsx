import { useAuth } from '../context/AuthContext'
import Dashboard from './Dashboard'
import LecturerDashboard from './LecturerDashboard'
import AdviserDashboard from './AdviserDashboard'
import StudentDashboard from './StudentDashboard'

function RoleDashboard() {
    const { user } = useAuth()

    if (!user) return null

    switch (user.role) {
        case 'HOD':
        case 'ADMIN':
            return <Dashboard />
        case 'LEVEL_ADVISER':
            return <AdviserDashboard />
        case 'LECTURER':
        case 'ADJUNCT':
            return <LecturerDashboard />
        case 'STUDENT':
            return <StudentDashboard />
        default:
            return <p className="text-slate-500">No dashboard available for this role.</p>
    }
}

export default RoleDashboard