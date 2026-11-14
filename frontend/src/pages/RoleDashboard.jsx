import { useAuth } from '../context/AuthContext'
import TodayPanel from '../components/TodayPanel'
import Dashboard from './Dashboard'
import AdviserDashboard from './AdviserDashboard'
import LecturerDashboard from './LecturerDashboard'
import StudentDashboard from './StudentDashboard'
import LabDashboard from './LabDashboard'

function RoleDashboard() {
    const { user } = useAuth()

    let page
    switch (user.role) {
        case 'LEVEL_ADVISER':
            page = <AdviserDashboard />
            break
        case 'LECTURER':
        case 'ADJUNCT':
            page = <LecturerDashboard />
            break
        case 'STUDENT':
            page = <StudentDashboard />
            break
        case 'LAB_OFFICER':
        case 'TECHNICAL_STAFF':
            page = <LabDashboard />
            break
        default:
            page = <Dashboard />
    }

    return (
        <>
            <TodayPanel />
            {page}
        </>
    )
}

export default RoleDashboard