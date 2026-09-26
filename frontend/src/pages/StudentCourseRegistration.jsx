import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import { getOutstandingCourses, registerMultiple, getRequiredUnits } from '../services/registrationService'

function StudentCourseRegistration() {
    const { user } = useAuth()
    const [allCourses, setAllCourses] = useState([])
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [outstanding, setOutstanding] = useState([])
    const [selectedCourses, setSelectedCourses] = useState({})
    const [checkedIn, setCheckedIn] = useState(false)
    const [requiredUnits, setRequired] = useState(null)

    const [submitting, setSubmitting] = useState(false)
    const [outcome, setOutcome] = useState(null)

    useEffect(() => {
        Promise.all([getAllCourses(), getAllSessions()])
            .then(([c, s]) => { setAllCourses(c); setSessions(s) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    if (!user.studentId) {
        return (
            <div className="card p-6">
                <p className="theme-text-muted">
                    Your account isn't linked to a student record yet. Contact your Level Adviser or the H.O.D.
                </p>
            </div>
        )
    }

    const currentSemesterCourses = allCourses.filter(
        (c) =>
            c.levelId === user.levelId &&
            c.semester === semester &&
            c.programmes &&
            c.programmes.some((p) => p.id === user.programmeId)
    )

    const outstandingIds = new Set(outstanding.map((c) => c.id))

    async function handleCheckIn() {
        setError(null)
        setOutcome(null)
        try {
            const carryovers = await getOutstandingCourses(user.studentId)
            setOutstanding(carryovers)

            const preselect = {}
            carryovers.forEach((c) => { preselect[c.id] = true })
            setSelectedCourses(preselect)

            try {
                const limit = await getRequiredUnits(user.programmeId, user.levelId, semester)
                setRequired(limit.requiredUnits)
            } catch { setRequired(null) }

            setCheckedIn(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        }
    }

    async function handleSemesterChange(newSemester) {
        setSemester(newSemester)
        setRequired(null)
        if (checkedIn) {
            try {
                const limit = await getRequiredUnits(user.programmeId, user.levelId, newSemester)
                setRequired(limit.requiredUnits)
            } catch { setRequired(null) }
        }
    }

    function toggleCourse(courseId) {
        if (outstandingIds.has(courseId)) return
        setSelectedCourses((prev) => ({ ...prev, [courseId]: !prev[courseId] }))
    }

    const selectedIds = Object.entries(selectedCourses).filter(([, c]) => c).map(([id]) => Number(id))
    const allSelectedCourses = [...outstanding, ...currentSemesterCourses]
        .filter((c, i, arr) => arr.findIndex((x) => x.id === c.id) === i)
        .filter((c) => selectedIds.includes(c.id))
    const runningTotal = allSelectedCourses.reduce((sum, c) => sum + c.creditUnit, 0)

    async function handleSubmit() {
        setError(null)
        setOutcome(null)
        if (selectedIds.length === 0) { setError('Select at least one course.'); return }
        setSubmitting(true)
        try {
            const result = await registerMultiple(user.studentId, Number(academicSessionId), semester, selectedIds)
            setOutcome(result)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSubmitting(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-2xl">
            <h2 className="text-2xl font-bold mb-1">Course Registration</h2>
            <p className="theme-text-muted text-sm mb-6">
                {user.fullName} — {user.matricNumber} · {user.programmeName}
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm whitespace-pre-line">{error}</div>}
            {outcome && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    Registered {outcome.registered} course(s) ({outcome.skipped} already registered).
                    Total units: {outcome.totalUnits} / {outcome.maxUnits}
                </div>
            )}

            <div className="card p-6 space-y-4">
                {!checkedIn ? (
                    <div>
                        <div className="grid grid-cols-2 gap-4 mb-4">
                            <div>
                                <label className="block text-sm font-medium mb-1">Academic Session</label>
                                <select value={academicSessionId} onChange={(e) => setAcademicSessionId(e.target.value)}
                                        className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="">Select…</option>
                                    {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Semester</label>
                                <select value={semester} onChange={(e) => handleSemesterChange(e.target.value)}
                                        className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="FIRST">First</option>
                                    <option value="SECOND">Second</option>
                                </select>
                            </div>
                        </div>
                        <button onClick={handleCheckIn} disabled={!academicSessionId}
                                className="w-full accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                            Continue to Course Selection
                        </button>
                    </div>
                ) : (
                    <>
                        {outstanding.length > 0 && (
                            <div>
                                <p className="text-sm font-semibold text-amber-700 mb-2">Outstanding Carryover Courses (must be registered)</p>
                                <div className="border border-amber-300 bg-amber-50 rounded-lg divide-y divide-amber-200">
                                    {outstanding.map((c) => (
                                        <div key={c.id} className="flex items-center gap-3 px-3 py-2 text-sm">
                                            <input type="checkbox" checked disabled />
                                            <span className="font-medium">{c.code}</span>
                                            <span className="text-slate-600">{c.title}</span>
                                            <span className="theme-text-muted ml-auto">{c.creditUnit} units</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div>
                            <p className="text-sm font-semibold mb-2">
                                {semester === 'FIRST' ? 'First' : 'Second'} Semester Courses — {user.levelName}
                            </p>
                            {currentSemesterCourses.length === 0 ? (
                                <p className="text-sm theme-text-muted">No matching courses found.</p>
                            ) : (
                                <div className="border theme-border rounded-lg divide-y theme-border max-h-72 overflow-y-auto">
                                    {currentSemesterCourses.map((c) => (
                                        <label key={c.id} className="flex items-center gap-3 px-3 py-2 text-sm hover:bg-black/5 cursor-pointer">
                                            <input type="checkbox" checked={!!selectedCourses[c.id]} onChange={() => toggleCourse(c.id)} />
                                            <span className="font-medium">{c.code}</span>
                                            <span>{c.title}</span>
                                            <span className="theme-text-muted ml-auto">{c.creditUnit} units</span>
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="flex justify-between items-center pt-2 border-t theme-border">
              <span className={`text-sm font-medium ${requiredUnits !== null && runningTotal === requiredUnits ? 'text-green-700' : ''}`}>
                Total selected: {runningTotal}{requiredUnits !== null ? ` / ${requiredUnits} required` : ' units'}
              </span>
                            <button onClick={handleSubmit} disabled={submitting || selectedIds.length === 0}
                                    className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-semibold btn-press disabled:opacity-50">
                                {submitting ? 'Registering…' : 'Register Selected Courses'}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    )
}

export default StudentCourseRegistration