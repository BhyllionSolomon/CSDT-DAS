import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { getRegistrationsForCourse } from '../services/registrationService'
import { bulkSaveResults } from '../services/resultService'
import api from '../services/api'

async function getAllProgrammes() {
    const response = await api.get('/programmes')
    return response.data
}
async function getAllLevels() {
    const response = await api.get('/levels')
    return response.data
}
async function getCoursesByProgramme(programmeId) {
    const response = await api.get(`/courses/programme/${programmeId}`)
    return response.data
}

const LEVEL_CODES = ['100', '200', '300', '400']
const BLOCKED_TOTALS = [34, 44, 49, 59, 69, 79]

function BulkEnterResults() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessions, setSessions] = useState([])
    const [courses, setCourses] = useState([])
    const [loadingCourses, setLoadingCourses] = useState(false)

    const [selectedProgramme, setSelectedProgramme] = useState(null)
    const [selectedLevel, setSelectedLevel] = useState(null)
    const [courseId, setCourseId] = useState('')
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [search, setSearch] = useState('')

    const [students, setStudents] = useState([])
    const [scores, setScores] = useState({})
    const [loaded, setLoaded] = useState(false)
    const [loadingList, setLoadingList] = useState(false)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels(), getAllSessions()])
            .then(([p, l, s]) => { setProgrammes(p); setLevels(l); setSessions(s) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    function resetToLevels() { setSelectedLevel(null); setCourses([]); setCourseId(''); setLoaded(false) }
    function resetToProgrammes() { setSelectedProgramme(null); resetToLevels() }

    async function handlePickLevel(level) {
        setSelectedLevel(level)
        setLoadingCourses(true)
        try {
            const allCourses = await getCoursesByProgramme(selectedProgramme.id)
            setCourses(allCourses.filter((c) => c.levelId === level.id))
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setLoadingCourses(false)
        }
    }

    async function handleLoad() {
        if (!courseId || !academicSessionId) return
        setError(null)
        setResult(null)
        setScores({})
        setLoadingList(true)
        try {
            const registrations = await getRegistrationsForCourse(courseId, academicSessionId, semester)
            setStudents(registrations.map((r) => ({
                id: r.studentId, matricNumber: r.matricNumber, fullName: r.studentName, levelCode: r.levelCode || '',
            })))
            setLoaded(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setLoadingList(false)
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
    function caWarning(id) { const raw = caValue(id); if (raw === '') return null; return Number(raw) === 30 ? 'Perfect CA — please confirm' : null }
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
        if (n === 70) return 'Perfect exam score — please confirm'
        if (n === 69) return 'Just below maximum — please confirm'
        return null
    }
    function examDisabled(id) { return caValue(id) === '' || caError(id) !== null }
    function totalValue(id) {
        const ca = caValue(id), exam = examValue(id)
        if (ca === '' || exam === '') return ''
        if (caError(id) || examError(id)) return ''
        return Number(ca) + Number(exam)
    }
    function totalError(id) {
        const t = totalValue(id); if (t === '') return null
        if (BLOCKED_TOTALS.includes(Math.round(t))) return `Total ${t} is a blocked borderline value — adjust a mark up or down`
        return null
    }
    function rowIsValid(id) { return caValue(id) !== '' && examValue(id) !== '' && !caError(id) && !examError(id) && !totalError(id) }
    function rowIsBlocked(id) { return caError(id) !== null || examError(id) !== null || totalError(id) !== null }

    const blockedCount = students.filter((s) => rowIsBlocked(s.id)).length

    async function handleSaveAll() {
        setError(null); setResult(null)
        if (blockedCount > 0) { setError(`${blockedCount} row(s) have invalid values. Fix the highlighted rows before saving.`); return }
        const entries = students.filter((s) => rowIsValid(s.id))
            .map((s) => ({ studentId: s.id, caScore: Number(caValue(s.id)), examScore: Number(examValue(s.id)) }))
        if (entries.length === 0) { setError('Enter at least one complete CA and Exam score before saving.'); return }
        setSaving(true)
        try {
            const data = await bulkSaveResults(courseId, academicSessionId, semester, entries)
            setResult(data)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSaving(false) }
    }

    const filteredStudents = students.filter((s) => {
        const term = search.toLowerCase()
        return s.matricNumber.toLowerCase().includes(term) || s.fullName.toLowerCase().includes(term)
    })

    const inputBase = 'w-20 border rounded-md px-2 py-1 text-sm'
    const inputOk = 'border-slate-300'
    const inputBad = 'border-red-500 bg-red-50 text-red-700'
    const inputWarn = 'border-amber-400 bg-amber-50'

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Bulk Enter Results</h2>
            <p className="text-sm text-slate-500 mb-6">
                Choose a programme, then a level, then a course. Only registered students appear.
                CA max 30, Exam max 70. Totals of 34, 44, 49, 59, 69 or 79 are not allowed.
            </p>

            <div className="flex items-center gap-2 text-sm mb-6">
                <button onClick={resetToProgrammes} className="text-blue-600 hover:underline font-medium">Programmes</button>
                {selectedProgramme && (
                    <>
                        <span className="text-slate-400">/</span>
                        <button onClick={resetToLevels} className="text-blue-600 hover:underline font-medium">{selectedProgramme.name}</button>
                    </>
                )}
                {selectedLevel && (
                    <>
                        <span className="text-slate-400">/</span>
                        <span className="font-medium text-slate-700">{selectedLevel.name}</span>
                    </>
                )}
            </div>

            {!selectedProgramme && (
                <div className="grid grid-cols-4 gap-4">
                    {programmes.map((p) => (
                        <button key={p.id} onClick={() => setSelectedProgramme(p)}
                                className="bg-white rounded-lg shadow p-6 text-left hover:shadow-md transition-shadow">
                            <p className="font-semibold text-slate-800">{p.name}</p>
                            <p className="text-sm text-slate-500">{p.code}</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && !selectedLevel && (
                <div className="grid grid-cols-4 gap-4">
                    {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => (
                        <button key={l.id} onClick={() => handlePickLevel(l)}
                                className="bg-white rounded-lg shadow p-6 text-center hover:shadow-md transition-shadow">
                            <p className="text-2xl font-bold text-slate-800">{l.code}</p>
                            <p className="text-sm text-slate-500">Level</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && selectedLevel && (
                <>
                    {error && <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>}
                    {result && (
                        <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                            <p>Created: {result.created} · Updated: {result.updated}</p>
                            {result.errors.length > 0 && (
                                <ul className="mt-2 list-disc list-inside text-red-700">{result.errors.map((e, i) => <li key={i}>{e}</li>)}</ul>
                            )}
                        </div>
                    )}

                    <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Course</label>
                            <select value={courseId} onChange={(e) => setCourseId(e.target.value)}
                                    disabled={loadingCourses} className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                                <option value="">{loadingCourses ? 'Loading…' : 'Select…'}</option>
                                {courses.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Session</label>
                            <select value={academicSessionId} onChange={(e) => setAcademicSessionId(e.target.value)}
                                    className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                                <option value="">Select…</option>
                                {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                            <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                    className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                                <option value="FIRST">First</option>
                                <option value="SECOND">Second</option>
                            </select>
                        </div>
                        <button onClick={handleLoad} disabled={!courseId || !academicSessionId || loadingList}
                                className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                            {loadingList ? 'Loading…' : 'Load Registered Students'}
                        </button>
                        {loaded && (
                            <input type="text" placeholder="Search students…" value={search} onChange={(e) => setSearch(e.target.value)}
                                   className="border border-slate-300 rounded-md px-3 py-2 text-sm ml-auto" />
                        )}
                    </div>

                    {loaded && students.length === 0 && (
                        <p className="text-center text-slate-400 py-8 bg-white rounded-lg shadow">
                            No students are registered for this course, session and semester yet.
                        </p>
                    )}

                    {loaded && students.length > 0 && (
                        <>
                            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                                    <tr>
                                        <th className="px-4 py-3">S/N</th>
                                        <th className="px-4 py-3">Matric No.</th>
                                        <th className="px-4 py-3">Name</th>
                                        <th className="px-4 py-3">Level</th>
                                        <th className="px-4 py-3">CA (30)</th>
                                        <th className="px-4 py-3">Exam (70)</th>
                                        <th className="px-4 py-3">Total</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {filteredStudents.map((s, i) => {
                                        const cErr = caError(s.id), cWarn = caWarning(s.id)
                                        const eErr = examError(s.id), eWarn = examWarning(s.id)
                                        const tErr = totalError(s.id)
                                        return (
                                            <tr key={s.id} className={rowIsBlocked(s.id) ? 'bg-red-50' : ''}>
                                                <td className="px-4 py-2">{i + 1}</td>
                                                <td className="px-4 py-2 font-medium">{s.matricNumber}</td>
                                                <td className="px-4 py-2">{s.fullName}</td>
                                                <td className="px-4 py-2">{s.levelCode}</td>
                                                <td className="px-2 py-2">
                                                    <input type="number" value={caValue(s.id)} onChange={(e) => updateScore(s.id, 'ca', e.target.value)}
                                                           className={`${inputBase} ${cErr ? inputBad : cWarn ? inputWarn : inputOk}`} />
                                                    {cErr && <p className="text-xs text-red-600 mt-1 w-40">{cErr}</p>}
                                                    {!cErr && cWarn && <p className="text-xs text-amber-700 mt-1 w-40">{cWarn}</p>}
                                                </td>
                                                <td className="px-2 py-2">
                                                    <input type="number" value={examValue(s.id)} disabled={examDisabled(s.id)}
                                                           onChange={(e) => updateScore(s.id, 'exam', e.target.value)}
                                                           className={`${inputBase} ${eErr ? inputBad : eWarn ? inputWarn : inputOk} disabled:bg-slate-100 disabled:cursor-not-allowed`} />
                                                    {eErr && <p className="text-xs text-red-600 mt-1 w-40">{eErr}</p>}
                                                    {!eErr && eWarn && <p className="text-xs text-amber-700 mt-1 w-40">{eWarn}</p>}
                                                </td>
                                                <td className="px-4 py-2">
                                                    <input type="text" value={totalValue(s.id)} disabled readOnly
                                                           className={`w-20 border rounded-md px-2 py-1 text-sm font-medium bg-slate-100 cursor-not-allowed ${tErr ? 'border-red-500 text-red-700' : 'border-slate-300'}`} />
                                                    {tErr && <p className="text-xs text-red-600 mt-1 w-48">{tErr}</p>}
                                                </td>
                                            </tr>
                                        )
                                    })}
                                    </tbody>
                                </table>
                            </div>

                            {blockedCount > 0 && (
                                <p className="text-sm text-red-600 mb-3">{blockedCount} row(s) have invalid values and must be corrected before saving.</p>
                            )}

                            <button onClick={handleSaveAll} disabled={saving || blockedCount > 0}
                                    className="bg-green-600 text-white rounded-md px-6 py-2 text-sm font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed">
                                {saving ? 'Saving…' : 'Save All Scores'}
                            </button>
                        </>
                    )}
                </>
            )}
        </div>
    )
}

export default BulkEnterResults