import { FlaskConical, ShieldCheck } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const ROLE_LABEL = { LAB_OFFICER: 'Lab Officer', TECHNICAL_STAFF: 'Technical Staff' }

function LabDashboard() {
    const { user } = useAuth()

    return (
        <div className="max-w-2xl">
            <h2 className="text-2xl font-bold mb-1">Welcome, {user.fullName}</h2>
            <p className="theme-text-muted text-sm mb-6">{ROLE_LABEL[user.role] || 'Laboratory'} · Laboratory area</p>

            <div className="card card-top-accent p-6 space-y-4">
                <div className="flex items-center gap-3">
                    <div className="w-11 h-11 rounded-xl accent-gradient flex items-center justify-center text-white">
                        <FlaskConical size={20} />
                    </div>
                    <div>
                        <p className="font-bold">Laboratory</p>
                        <p className="text-sm theme-text-muted">Your tools will appear here.</p>
                    </div>
                </div>

                <p className="text-sm">
                    The department is still setting up what the laboratory area does. Once the H.O.D confirms it, the tools for your role
                    will be added to the menu on the left.
                </p>

                <p className="text-xs theme-text-muted flex items-center gap-2">
                    <ShieldCheck size={14} />
                    This area is separate from academic records. Student, course and result information is not available to your account.
                </p>
            </div>
        </div>
    )
}

export default LabDashboard