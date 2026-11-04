import { useEffect, useState } from 'react'
import { Mail, Phone, UserCheck } from 'lucide-react'
import { getMySupervisor } from '../services/projectService'
import { designationLabel } from '../utils/projects'

const msg = (err) => err.response?.data?.message || err.message
const when = (v) => (v ? new Date(v).toLocaleDateString('en-GB') : '')

function MyProjectSupervisor() {
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getMySupervisor().then(setData).catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-lg">
            <h2 className="text-2xl font-bold mb-2">My Project Supervisor</h2>
            <p className="theme-text-muted text-sm mb-6">The lecturer who supervises your final-year project.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {data && !data.assigned && (
                <div className="card p-6 text-sm theme-text-muted">
                    You have not been assigned a supervisor yet. The H.O.D assigns supervisors to final-year students once, at the start of the project year.
                </div>
            )}

            {data && data.assigned && (
                <div className="card card-top-accent p-6 space-y-3">
                    <div className="flex items-center gap-3">
                        <div className="w-11 h-11 rounded-xl accent-gradient flex items-center justify-center text-white"><UserCheck size={20} /></div>
                        <div>
                            <p className="font-bold text-lg">{data.supervisorName}</p>
                            <p className="text-sm theme-text-muted">{designationLabel(data.designation)}</p>
                        </div>
                    </div>
                    <dl className="text-sm grid grid-cols-2 gap-y-1">
                        <dt className="theme-text-muted">Status</dt><dd className="font-medium">{data.status}</dd>
                        <dt className="theme-text-muted">Session</dt><dd className="font-medium">{data.sessionName}</dd>
                        <dt className="theme-text-muted">Assigned on</dt><dd className="font-medium">{when(data.assignedAt)}</dd>
                    </dl>
                    {data.supervisorEmail && <p className="flex items-center gap-2 text-sm"><Mail size={15} /> {data.supervisorEmail}</p>}
                    {data.supervisorPhone && <p className="flex items-center gap-2 text-sm"><Phone size={15} /> {data.supervisorPhone}</p>}
                </div>
            )}
        </div>
    )
}

export default MyProjectSupervisor