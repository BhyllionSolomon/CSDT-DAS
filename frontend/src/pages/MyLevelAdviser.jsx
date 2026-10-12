import { useEffect, useState } from 'react'
import { Mail, Phone, UserCheck } from 'lucide-react'
import { getMyAdviser } from '../services/adviserService'

function MyLevelAdviser() {
    const [adviser, setAdviser] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getMyAdviser()
            .then(setAdviser)
            .catch((err) => setError(err.response?.data?.message || err.message))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-lg">
            <h2 className="text-2xl font-bold mb-2">My Level Adviser</h2>
            <p className="theme-text-muted text-sm mb-6">Your adviser for your programme and level.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {!adviser ? (
                <div className="card p-6 theme-text-muted text-sm">
                    No Level Adviser has been assigned to your programme and level yet. Please check with the H.O.D's office.
                </div>
            ) : (
                <div className="card card-top-accent p-6 space-y-3">
                    <div className="flex items-center gap-3">
                        <div className="w-11 h-11 rounded-xl accent-gradient flex items-center justify-center text-white"><UserCheck size={20} /></div>
                        <div>
                            <p className="font-bold text-lg">{adviser.fullName}</p>
                            <p className="text-sm theme-text-muted">Level Adviser — {adviser.levelName} {adviser.programmeName}</p>
                        </div>
                    </div>
                    <p className="flex items-center gap-2 text-sm"><Mail size={15} /> {adviser.email}</p>
                    <p className="flex items-center gap-2 text-sm"><Phone size={15} /> {adviser.phoneNumber || 'Phone number not provided yet'}</p>
                </div>
            )}
        </div>
    )
}

export default MyLevelAdviser