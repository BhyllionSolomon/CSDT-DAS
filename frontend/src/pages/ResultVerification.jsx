import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { getResultsForCourse } from '../services/resultService'
import api from '../services/api'

const LEVEL_CODES = ['100', '200', '300', '400']

function gradeAndRemark(total) {
    if (total >= 70) return { grade: 'A', remark: 'Excellent' }
    if (total >= 60) return { grade: 'B', remark: 'Very Good' }
    if (total >= 50) return { grade: 'C', remark: 'Good' }
    if (total >= 45) return { grade: 'D', remark: 'Fair' }
    if (total >= 40) return { grade: 'E', remark: 'Pass' }
    return { grade: 'F', remark: 'Fail' }
}

async function getAllProgrammes() { return (await api.get('/programmes')).data }
async function getAllLevels() { return (await api.get('/levels')).data }
async function getCoursesByProgramme(id) { return (await api.get(`/courses/programme/${id}`)).data }
async function approveAll(courseId, sessionId, semester) {
    return (await api.put(`/results/course/${courseId}/session/${sessionId}/semester/${semester}/approve-all`)).data
}

function ResultVerification() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [selectedProgramme, setSelectedProgramme] = useState(null)
    const [selectedLevel, setSelectedLevel] = useState(null)
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [scoresheets, setScoresheets] = useState(null)
    const [loadingSheets, setLoadingSheets] = useState(false)
    const [selectedSheet, setSelectedSheet] = useState(null)
    const [verifying, setVerifying] = useState(false)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels(), getAllSessions()])
            .then(([p, l, s]) => { setProgrammes(p); setLevels(l); setSessions(s) })
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false))
    }, [])

    function resetToLevels() { setSelectedLevel(null); setScoresheets(null); setSelectedSheet(null) }
    function resetToProgrammes() { setSelectedProgramme(null); resetToLevels() }

    async function handleLoadSheets() {
        if (!academicSessionId) return
        setError(null); setSelectedSheet(null)
        setLoadingSheets(true)
        try {
            const allCourses = await getCoursesByProgramme(selectedProgramme.id)
            const levelCourses = allCourses.filter((c) => c.levelId === selectedLevel.id && c.semester === semester)

            const sheets = []
            for (const course of levelCourses) {
                const results = await getResultsForCourse(course.id, academicSessionId, semester)
                if (results.length > 0) {
                    const pending = results.filter((r) => r.status === 'PENDING').length
                    const approved = results.filter((r) => r.status === 'APPROVED').length
                    sheets.push({ course, results, pending, approved })
                }
            }
            setScoresheets(sheets)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setLoadingSheets(false) }
    }

    async function handleVerify() {
        setVerifying(true)
        try {
            await approveAll(selectedSheet.course.id, academicSessionId, semester)
            const results = await getResultsForCourse(selectedSheet.course.id, academicSessionId, semester)
            setSelectedSheet({ ...selectedSheet, results, pending: 0, approved: results.length })
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setVerifying(false) }
    }

    const gradeCounts = selectedSheet?.results.reduce((acc, r) => {
        const g = gradeAndRemark(r.totalScore).grade
        acc[g] = (acc[g] || 0) + 1
        return acc
    }, {}) || {}

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Result Verification</h2>
            <p className="theme-text-muted text-sm mb-6">
                Choose a programme, level, session and semester to review submitted exam scoresheets.
            </p>

            {error && <div className="no-print mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            {!selectedSheet && (
                <div className="flex items-center gap-2 text-sm mb-6">
                    <button onClick={resetToProgrammes} className="font-medium" style={{ color: 'var(--accent)' }}>Programmes</button>
                    {selectedProgramme && (<><span className="theme-text-muted">/</span><button onClick={resetToLevels} className="font-medium" style={{ color: 'var(--accent)' }}>{selectedProgramme.name}</button></>)}
                    {selectedLevel && (<><span className="theme-text-muted">/</span><span className="font-medium">{selectedLevel.name}</span></>)}
                </div>
            )}

            {!selectedProgramme && (
                <div className="grid grid-cols-4 gap-4">
                    {programmes.map((p) => (
                        <button key={p.id} onClick={() => setSelectedProgramme(p)} className="card card-hover p-6 text-left">
                            <p className="font-semibold">{p.name}</p>
                            <p className="text-sm theme-text-muted">{p.code}</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && !selectedLevel && (
                <div className="grid grid-cols-4 gap-4">
                    {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => (
                        <button key={l.id} onClick={() => setSelectedLevel(l)} className="card card-hover p-6 text-center">
                            <p className="text-2xl font-bold">{l.code}</p>
                            <p className="text-sm theme-text-muted">Level</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && selectedLevel && !selectedSheet && (
                <div>
                    <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                        <div>
                            <label className="block text-sm font-medium mb-1">Session</label>
                            <select value={academicSessionId} onChange={(e) => setAcademicSessionId(e.target.value)}
                                    className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Select…</option>
                                {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Semester</label>
                            <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                    className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="FIRST">First</option>
                                <option value="SECOND">Second</option>
                            </select>
                        </div>
                        <button onClick={handleLoadSheets} disabled={!academicSessionId || loadingSheets}
                                className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {loadingSheets ? 'Loading…' : 'Load Submitted Scoresheets'}
                        </button>
                    </div>

                    {scoresheets && (
                        scoresheets.length === 0 ? (
                            <p className="theme-text-muted">No exam scoresheets have been submitted yet for this selection.</p>
                        ) : (
                            <div className="grid grid-cols-2 gap-4">
                                {scoresheets.map((sheet) => (
                                    <button key={sheet.course.id} onClick={() => setSelectedSheet(sheet)}
                                            className="card card-hover p-4 text-left">
                                        <p className="font-semibold">{sheet.course.code} — {sheet.course.title}</p>
                                        <p className="text-sm theme-text-muted">{sheet.results.length} students</p>
                                        <span className={`inline-block mt-2 text-xs px-2 py-0.5 rounded-full font-medium ${
                                            sheet.pending === 0 ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                                        }`}>
                      {sheet.pending === 0 ? 'Verified' : `${sheet.pending} pending review`}
                    </span>
                                    </button>
                                ))}
                            </div>
                        )
                    )}
                </div>
            )}

            {selectedSheet && (
                <div>
                    <div className="no-print flex justify-between items-center mb-4">
                        <button onClick={() => setSelectedSheet(null)} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>
                            ← Back to scoresheet list
                        </button>
                        <div className="flex gap-2">
                            <button onClick={() => window.print()} className="border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press">
                                Print
                            </button>
                            <button onClick={handleVerify} disabled={verifying || selectedSheet.pending === 0}
                                    className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                {verifying ? 'Verifying…' : selectedSheet.pending === 0 ? 'Verified ✓' : 'Verify'}
                            </button>
                        </div>
                    </div>

                    <div className="card p-8">
                        <div className="flex items-start gap-4 border-b theme-border pb-4 mb-4">
                            <div className="w-16 h-16 rounded-full accent-gradient flex items-center justify-center text-white font-bold text-xl shrink-0">K</div>
                            <div className="flex-1 text-center">
                                <p className="font-bold text-lg">KOLADAISI UNIVERSITY, IBADAN</p>
                                <p className="font-semibold text-sm">FACULTY OF APPLIED SCIENCES</p>
                                <p className="font-semibold text-sm">EXAMINATION SCORE SHEET</p>
                            </div>
                            <div className="w-16 shrink-0" />
                        </div>

                        <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm mb-6">
                            <p><span className="font-semibold">DEPARTMENT:</span> Computing Sciences and Digital Technologies</p>
                            <p><span className="font-semibold">PROGRAMME:</span> {selectedProgramme.name}</p>
                            <p><span className="font-semibold">COURSE TITLE:</span> {selectedSheet.course.title}</p>
                            <p><span className="font-semibold">COURSE CODE:</span> {selectedSheet.course.code}</p>
                            <p><span className="font-semibold">NO. OF STUDENTS:</span> {selectedSheet.results.length}</p>
                            <p><span className="font-semibold">SEMESTER:</span> {semester === 'FIRST' ? 'First Semester' : 'Second Semester'}</p>
                        </div>

                        <table className="w-full text-sm text-left border-collapse">
                            <thead>
                            <tr className="border-y theme-border">
                                <th className="px-2 py-2 border theme-border">S/N</th>
                                <th className="px-2 py-2 border theme-border">Matric No.</th>
                                <th className="px-2 py-2 border theme-border">Exam</th>
                                <th className="px-2 py-2 border theme-border">C.A.</th>
                                <th className="px-2 py-2 border theme-border">Total</th>
                                <th className="px-2 py-2 border theme-border">Grade</th>
                                <th className="px-2 py-2 border theme-border">Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {selectedSheet.results.map((r, i) => (
                                <tr key={r.id}>
                                    <td className="px-2 py-1 border theme-border">{i + 1}</td>
                                    <td className="px-2 py-1 border theme-border font-medium">{r.matricNumber}</td>
                                    <td className="px-2 py-1 border theme-border text-center">{r.examScore}</td>
                                    <td className="px-2 py-1 border theme-border text-center">{r.caScore}</td>
                                    <td className="px-2 py-1 border theme-border text-center font-medium">{r.totalScore}</td>
                                    <td className="px-2 py-1 border theme-border text-center">{r.grade}</td>
                                    <td className="px-2 py-1 border theme-border text-center">
                                        <span className={r.status === 'APPROVED' ? 'text-green-700' : 'text-amber-700'}>{r.status}</span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div className="mt-6 max-w-sm">
                            <table className="w-full text-sm border-collapse">
                                <thead><tr className="border-y theme-border"><th className="px-2 py-1 border theme-border text-left">Range</th><th className="px-2 py-1 border theme-border">Grade</th><th className="px-2 py-1 border theme-border">No.</th></tr></thead>
                                <tbody>
                                {[['70 and Above', 'A'], ['60-69', 'B'], ['50-59', 'C'], ['45-49', 'D'], ['40-44', 'E'], ['Below 40', 'F']].map(([range, g]) => (
                                    <tr key={g}><td className="px-2 py-1 border theme-border">{range}</td><td className="px-2 py-1 border theme-border text-center">{g}</td><td className="px-2 py-1 border theme-border text-center">{gradeCounts[g] || '-'}</td></tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

export default ResultVerification