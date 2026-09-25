import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import { getRegistrationsForCourse } from '../services/registrationService'
import { getResultsForCourse, bulkSaveResults } from '../services/resultService'

const BLOCKED_TOTALS = [39, 44, 49, 59, 69, 79]

function gradeAndRemark(total) {
    if (total >= 70) return { grade: 'A', remark: 'Excellent' }
    if (total >= 60) return { grade: 'B', remark: 'Very Good' }
    if (total >= 50) return { grade: 'C', remark: 'Good' }
    if (total >= 45) return { grade: 'D', remark: 'Fair' }
    if (total >= 40) return { grade: 'E', remark: 'Pass' }
    return { grade: 'F', remark: 'Fail' }
}

async function getMyAllocations(lecturerId) {
    const response = await api.get(`/allocations/lecturer/${lecturerId}`)
    return response.data
}
async function getCourseDetail(courseId) {
    const response = await api.get(`/courses/${courseId}`)
    return response.data
}

function ExamScoresheet() {
    const { user } = useAuth()
    const [allocations, setAllocations] = useState([])
    const [selectedAllocation, setSelectedAllocation] = useState(null)
    const [course, setCourse] = useState(null)

    const [students, setStudents] = useState([])
    const [scores, setScores] = useState({})
    const [loaded, setLoaded] = useState(false)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        getMyAllocations(user.id)
            .then(setAllocations)
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false))
    }, [user.id])

    async function handleSelectAllocation(allocation) {
        setSelectedAllocation(allocation)
        setError(null)
        setResult(null)
        setLoaded(false)
        try {
            const [courseDetail, registrations, existingResults] = await Promise.all([
                getCourseDetail(allocation.courseId),
                getRegistrationsForCourse(allocation.courseId, allocation.academicSessionId, allocation.semester),
                getResultsForCourse(allocation.courseId, allocation.academicSessionId, allocation.semester),
            ])

            setCourse(courseDetail)
            setStudents(registrations.map((r) => ({
                id: r.studentId, matricNumber: r.matricNumber, fullName: r.studentName,
            })))

            const prefill = {}
            existingResults.forEach((r) => {
                prefill[r.studentId] = { ca: String(r.caScore), exam: String(r.examScore) }
            })
            setScores(prefill)
            setLoaded(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        }
    }

    function updateScore(studentId, field, value) {
        setScores((prev) => ({ ...prev, [studentId]: { ...prev[studentId], [field]: value } }))
    }
    function caValue(id) { return scores[id]?.ca ?? '' }
    function examValue(id) { return scores[id]?.exam ?? '' }

    function caError(id) {
        const raw = caValue(id); if (raw === '') return null
        const n = Number(raw)
        if (Number.isNaN(n)) return 'Not a number'
        if (n < 0) return 'Cannot be negative'
        if (n > 30) return 'Impossible — CA cannot exceed 30'
        return null
    }
    function caWarning(id) { const raw = caValue(id); if (raw === '') return null; return Number(raw) === 30 ? 'Perfect CA — confirm' : null }
    function examError(id) {
        const raw = examValue(id); if (raw === '') return null
        const n = Number(raw)
        if (Number.isNaN(n)) return 'Not a number'
        if (n < 0) return 'Cannot be negative'
        if (n > 70) return 'Impossible — Exam cannot exceed 70'
        return null
    }
    function examWarning(id) {
        const raw = examValue(id); if (raw === '') return null
        const n = Number(raw)
        if (n > 68) return 'High score — confirm'
        return null
    }
    function examDisabled(id) { return caValue(id) === '' || caError(id) !== null }
    function totalValue(id) {
        const ca = caValue(id), exam = examValue(id)
        if (ca === '' || exam === '' || caError(id) || examError(id)) return ''
        return Number(ca) + Number(exam)
    }
    function totalError(id) {
        const t = totalValue(id); if (t === '') return null
        if (BLOCKED_TOTALS.includes(Math.round(t))) return `Blocked value ${t} — adjust up or down`
        return null
    }
    function rowIsValid(id) { return caValue(id) !== '' && examValue(id) !== '' && !caError(id) && !examError(id) && !totalError(id) }
    function rowIsBlocked(id) { return caError(id) !== null || examError(id) !== null || totalError(id) !== null }

    const blockedCount = students.filter((s) => rowIsBlocked(s.id)).length

    const gradeCounts = students.reduce((acc, s) => {
        const t = totalValue(s.id)
        if (t === '') return acc
        const g = gradeAndRemark(t).grade
        acc[g] = (acc[g] || 0) + 1
        return acc
    }, {})

    async function handleSubmit() {
        setError(null); setResult(null)
        if (blockedCount > 0) { setError(`${blockedCount} row(s) have invalid values.`); return }
        const entries = students.filter((s) => rowIsValid(s.id))
            .map((s) => ({ studentId: s.id, caScore: Number(caValue(s.id)), examScore: Number(examValue(s.id)) }))
        if (entries.length === 0) { setError('Enter at least one complete score.'); return }
        setSaving(true)
        try {
            const data = await bulkSaveResults(
                selectedAllocation.courseId, selectedAllocation.academicSessionId, selectedAllocation.semester, entries
            )
            setResult(data)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSaving(false) }
    }

    const inputBase = 'w-16 border rounded px-2 py-1 text-sm text-center'
    const inputOk = 'border-slate-300'
    const inputBad = 'border-red-500 bg-red-50'
    const inputWarn = 'border-amber-400 bg-amber-50'

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            {!loaded ? (
                <div>
                    <h2 className="text-2xl font-bold mb-2">Exam Scoresheet</h2>
                    <p className="theme-text-muted text-sm mb-6">Select one of your assigned courses.</p>
                    {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                    {allocations.length === 0 ? (
                        <p className="theme-text-muted">You have no assigned courses yet.</p>
                    ) : (
                        <div className="grid grid-cols-3 gap-4">
                            {allocations.map((a) => (
                                <button key={a.id} onClick={() => handleSelectAllocation(a)}
                                        className="card card-hover p-4 text-left">
                                    <p className="font-semibold">{a.courseCode}</p>
                                    <p className="text-sm theme-text-muted">{a.courseTitle}</p>
                                    <p className="text-xs theme-text-muted mt-1">{a.academicSessionName} · {a.semester}</p>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            ) : (
                <div>
                    <div className="no-print flex justify-between items-center mb-4">
                        <button onClick={() => setLoaded(false)} className="text-sm theme-accent" style={{ color: 'var(--accent)' }}>
                            ← Choose a different course
                        </button>
                        <div className="flex gap-2">
                            <button onClick={() => window.print()} className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                                Print
                            </button>
                            <button onClick={handleSubmit} disabled={saving || blockedCount > 0}
                                    className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                {saving ? 'Submitting…' : 'Submit Scores'}
                            </button>
                        </div>
                    </div>

                    {error && <div className="no-print mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                    {result && (
                        <div className="no-print mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                            Created: {result.created} · Updated: {result.updated}
                        </div>
                    )}
                    {blockedCount > 0 && (
                        <p className="no-print text-sm text-red-600 mb-3">{blockedCount} row(s) invalid — fix before submitting.</p>
                    )}

                    <div className="card p-8">
                        {/* Header block, matching the official sheet */}
                        <div className="flex items-start gap-4 border-b theme-border pb-4 mb-4">
                            <div className="w-16 h-16 rounded-full accent-gradient flex items-center justify-center text-white font-bold text-xl shrink-0">
                                K
                            </div>
                            <div className="flex-1 text-center">
                                <p className="font-bold text-lg">KOLADAISI UNIVERSITY, IBADAN</p>
                                <p className="font-semibold text-sm">FACULTY OF APPLIED SCIENCES</p>
                                <p className="font-semibold text-sm">EXAMINATION SCORE SHEET</p>
                            </div>
                            <div className="w-16 shrink-0" />
                        </div>

                        <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm mb-6">
                            <p><span className="font-semibold">DEPARTMENT:</span> Computing Sciences and Digital Technologies</p>
                            <p><span className="font-semibold">SESSION:</span> {selectedAllocation.academicSessionName}</p>
                            <p><span className="font-semibold">PROGRAMME:</span> {course?.programmes?.map((p) => p.name).join(', ') || '—'}</p>
                            <p><span className="font-semibold">SEMESTER:</span> {selectedAllocation.semester === 'FIRST' ? 'First Semester' : 'Second Semester'}</p>
                            <p><span className="font-semibold">COURSE TITLE:</span> {course?.title}</p>
                            <p><span className="font-semibold">COURSE CODE:</span> {course?.code}</p>
                            <p><span className="font-semibold">NO. OF STUDENTS:</span> {students.length}</p>
                            <p><span className="font-semibold">COURSE UNIT:</span> {course?.creditUnit}</p>
                            <p className="col-span-2"><span className="font-semibold">DATE:</span> {new Date().toLocaleDateString('en-GB')}</p>
                        </div>

                        <table className="w-full text-sm text-left border-collapse">
                            <thead>
                            <tr className="border-y theme-border">
                                <th className="px-2 py-2 border theme-border">S/N</th>
                                <th className="px-2 py-2 border theme-border">Matric No.</th>
                                <th className="px-2 py-2 border theme-border">Name</th>
                                <th className="px-2 py-2 border theme-border">Exam (70)</th>
                                <th className="px-2 py-2 border theme-border">C.A. (30)</th>
                                <th className="px-2 py-2 border theme-border">% Score</th>
                                <th className="px-2 py-2 border theme-border">Grade</th>
                                <th className="px-2 py-2 border theme-border">Remark</th>
                            </tr>
                            </thead>
                            <tbody>
                            {students.map((s, i) => {
                                const t = totalValue(s.id)
                                const gr = t !== '' ? gradeAndRemark(t) : null
                                const cErr = caError(s.id), cWarn = caWarning(s.id)
                                const eErr = examError(s.id), eWarn = examWarning(s.id)
                                const tErr = totalError(s.id)
                                return (
                                    <tr key={s.id} className={rowIsBlocked(s.id) ? 'bg-red-50' : ''}>
                                        <td className="px-2 py-1 border theme-border">{i + 1}</td>
                                        <td className="px-2 py-1 border theme-border font-medium">{s.matricNumber}</td>
                                        <td className="px-2 py-1 border theme-border">{s.fullName}</td>
                                        <td className="px-2 py-1 border theme-border">
                                            <input type="number" value={examValue(s.id)} disabled={examDisabled(s.id)}
                                                   onChange={(e) => updateScore(s.id, 'exam', e.target.value)}
                                                   className={`no-print ${inputBase} ${eErr ? inputBad : eWarn ? inputWarn : inputOk} disabled:bg-slate-100`} />
                                            <span className="print-only hidden">{examValue(s.id)}</span>
                                            {eErr && <p className="no-print text-xs text-red-600">{eErr}</p>}
                                            {!eErr && eWarn && <p className="no-print text-xs text-amber-700">{eWarn}</p>}
                                        </td>
                                        <td className="px-2 py-1 border theme-border">
                                            <input type="number" value={caValue(s.id)}
                                                   onChange={(e) => updateScore(s.id, 'ca', e.target.value)}
                                                   className={`no-print ${inputBase} ${cErr ? inputBad : cWarn ? inputWarn : inputOk}`} />
                                            <span className="print-only hidden">{caValue(s.id)}</span>
                                            {cErr && <p className="no-print text-xs text-red-600">{cErr}</p>}
                                            {!cErr && cWarn && <p className="no-print text-xs text-amber-700">{cWarn}</p>}
                                        </td>
                                        <td className="px-2 py-1 border theme-border font-medium">
                                            {t}
                                            {tErr && <p className="no-print text-xs text-red-600">{tErr}</p>}
                                        </td>
                                        <td className="px-2 py-1 border theme-border">{gr?.grade || ''}</td>
                                        <td className="px-2 py-1 border theme-border">{gr?.remark || ''}</td>
                                    </tr>
                                )
                            })}
                            </tbody>
                        </table>

                        <div className="mt-6 max-w-sm">
                            <table className="w-full text-sm border-collapse">
                                <thead>
                                <tr className="border-y theme-border">
                                    <th className="px-2 py-1 border theme-border text-left">Range</th>
                                    <th className="px-2 py-1 border theme-border">Grade</th>
                                    <th className="px-2 py-1 border theme-border">No.</th>
                                </tr>
                                </thead>
                                <tbody>
                                {[['70 and Above', 'A'], ['60-69', 'B'], ['50-59', 'C'], ['45-49', 'D'], ['40-44', 'E'], ['Below 40', 'F']].map(([range, g]) => (
                                    <tr key={g}>
                                        <td className="px-2 py-1 border theme-border">{range}</td>
                                        <td className="px-2 py-1 border theme-border text-center">{g}</td>
                                        <td className="px-2 py-1 border theme-border text-center">{gradeCounts[g] || '-'}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="grid grid-cols-2 gap-8 mt-10 text-sm">
                            <div>
                                <p className="border-t theme-border pt-1">{user.fullName}</p>
                                <p className="theme-text-muted text-xs">Lecturer's Name &amp; Signature — {new Date().toLocaleDateString('en-GB')}</p>
                            </div>
                            <div>
                                <p className="border-t theme-border pt-1">&nbsp;</p>
                                <p className="theme-text-muted text-xs">H.O.D's Signature &amp; Date</p>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default ExamScoresheet