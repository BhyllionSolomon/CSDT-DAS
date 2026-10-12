import { useAuth } from '../context/AuthContext'
import Dashboard from './Dashboard'
import LecturerDashboard from './LecturerDashboard'
import AdviserDashboard from './AdviserDashboard'
import StudentDashboard from './StudentDashboard'
import LabDashboard from './LabDashboard'

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
        case 'LAB_OFFICER':
        case 'TECHNICAL_STAFF':
            return <LabDashboard />
        default:
            return <p className="theme-text-muted">No dashboard is available for this role.</p>
    }
}

export default RoleDashboard