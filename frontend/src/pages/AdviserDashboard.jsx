import { useAuth } from '../context/AuthContext'
import ProgrammeOverview from './ProgrammeOverview'

function AdviserDashboard() {
    const { user } = useAuth()

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-1">Welcome, {user.fullName}</h2>
            <p className="text-sm text-slate-500 mb-6">
                Level Adviser — {user.programmeName}
            </p>
            <ProgrammeOverview lockedProgrammeId={user.programmeId} />
        </div>
    )
}

export default AdviserDashboard