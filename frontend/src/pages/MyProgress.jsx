import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Info } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import SessionSemesterBar from '../components/SessionSemesterBar'
import api from '../services/api'
import { getMyCa, caLabel } from '../services/caService'
import { getMyAttendance } from '../services/attendanceService'

function MyProgress() {
    const { user } = useAuth()
    const [sel, setSel] = useState(null)
    const [ca, setCa] = useState(null)
    const [att, setAtt] = useState(null)
    const [carry, setCarry] = useState(null)

    useEffect(() => {
        if (!user.studentId) return
        api.get(`/results/student/${user.studentId}/outstanding-courses`)
            .then((r) => setCarry(r.data))
            .catch(() => setCarry([]))
    }, [user.studentId])

    useEffect(() => {
        if (!sel) return
        setCa(null); setAtt(null)
        getMyCa(sel.sessionId, sel.semester).then(setCa).catch(() => setCa({ cap: 30, courses: [] }))
        getMyAttendance(sel.sessionId, sel.semester).then(setAtt).catch(() => setAtt({ required: 75, courses: [] }))
    }, [sel])

    if (!user.studentId) {
        return <div className="card p-6 max-w-xl text-sm theme-text-muted">Your account isn't linked to a student record yet.</div>
    }

    return (
        <div className="max-w-3xl">
            <h2 className="text-2xl font-bold mb-2">My Progress</h2>

            <div className="flex items-start gap-3 mb-6 px-4 py-3 rounded-lg bg-blue-50 text-blue-800 text-sm">
                <Info size={18} className="shrink-0 mt-0.5" />
                <span>Your final results and CGPA are on the school portal. This page shows what you can act on here: carry-overs, continuous assessment and attendance.</span>
            </div>

            <div className="card p-4 mb-6">
                <p className="font-semibold mb-2">Carry-over courses</p>
                {carry === null ? <p className="text-sm theme-text-muted">Loading…</p>
                    : carry.length === 0 ? <p className="text-sm text-green-700">You have no carry-over course.</p>
                        : (
                            <>
                                <ul className="flex flex-wrap gap-2 mb-2">
                                    {carry.map((c) => (
                                        <li key={c.id || c.code} className="text-xs px-3 py-1 rounded-full bg-amber-100 text-amber-800 font-medium">{c.code}</li>
                                    ))}
                                </ul>
                                <p className="text-xs theme-text-muted">You register these first each semester. <Link to="/my-carryovers" className="underline" style={{ color: 'var(--accent)' }}>See the details</Link></p>
                            </>
                        )}
            </div>

            <SessionSemesterBar onChange={setSel} />

            <div className="grid md:grid-cols-2 gap-4">
                <div className="card p-4">
                    <p className="font-semibold mb-3">Continuous assessment</p>
                    {ca === null ? <p className="text-sm theme-text-muted">Loading…</p>
                        : ca.courses.length === 0 ? <p className="text-sm theme-text-muted">Nothing has been released yet.</p>
                            : ca.courses.map((c) => (
                                <div key={c.courseId} className="mb-3">
                                    <div className="flex justify-between text-sm"><span className="font-medium">{c.code}</span><span>{c.earned} <span className="theme-text-muted">of {c.releasedMax} released</span></span></div>
                                    <div className="h-2 rounded-full overflow-hidden mt-1" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                        <div className="h-full accent-gradient" style={{ width: `${c.releasedMax ? (c.earned / c.releasedMax) * 100 : 0}%` }} />
                                    </div>
                                    <p className="text-[11px] theme-text-muted mt-1">{c.items.map((i) => caLabel(i.category)).join(' · ')}</p>
                                </div>
                            ))}
                    <Link to="/my-ca" className="text-xs underline" style={{ color: 'var(--accent)' }}>Open my C.A. scores</Link>
                </div>

                <div className="card p-4">
                    <p className="font-semibold mb-3">Attendance</p>
                    {att === null ? <p className="text-sm theme-text-muted">Loading…</p>
                        : att.courses.length === 0 ? <p className="text-sm theme-text-muted">No attendance recorded yet.</p>
                            : att.courses.map((c) => (
                                <div key={c.courseId} className="mb-3">
                                    <div className="flex justify-between text-sm">
                                        <span className="font-medium">{c.code}</span>
                                        <span className={c.percent == null ? 'theme-text-muted' : c.eligible ? 'text-green-700' : 'text-red-600'}>{c.percent == null ? '—' : `${c.percent}%`}</span>
                                    </div>
                                    <div className="h-2 rounded-full overflow-hidden mt-1" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                        <div className="h-full" style={{ width: `${c.percent || 0}%`, backgroundColor: c.eligible ? '#16a34a' : '#dc2626' }} />
                                    </div>
                                </div>
                            ))}
                    <Link to="/my-attendance" className="text-xs underline" style={{ color: 'var(--accent)' }}>Open my attendance</Link>
                </div>
            </div>
        </div>
    )
}

export default MyProgress