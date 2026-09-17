import { useEffect, useState } from 'react'
import { getAllStudents } from '../services/studentService'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import {
    getOutstandingCourses,
    registerMultiple,
    getRequiredUnits,
} from '../services/registrationService'

function StudentCourseRegistration() {
    const [students, setStudents] = useState([])
    const [allCourses, setAllCourses] = useState([])
    const [sessions, setSessions] = useState([])

    const [matricNumber, setMatricNumber] = useState('')
    const [studentId, setStudentId] = useState('')
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [outstanding, setOutstanding] = useState([])
    const [selectedCourses, setSelectedCourses] = useState({})
    const [checkedIn, setCheckedIn] = useState(false)
    const [requiredUnits, setRequired] = useState(null)

    const [loading, setLoading] = useState(true)
    const [checking, setChecking] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [outcome, setOutcome] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllStudents(), getAllCourses(), getAllSessions()])
            .then(([studentsData, coursesData, sessionsData]) => {
                setStudents(studentsData)
                setAllCourses(coursesData)
                setSessions(sessionsData)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    const selectedStudent = students.find((s) => String(s.id) === String(studentId))

    const currentSemesterCourses = selectedStudent
        ? allCourses.filter(
            (c) =>
                c.levelId === selectedStudent.levelId &&
                c.semester === semester &&
                c.programmes &&
                c.programmes.some((p) => p.id === selectedStudent.programmeId)
        )
        : []

    const outstandingIds = new Set(outstanding.map((c) => c.id))

    async function handleFindStudent() {
        setError(null)
        setOutcome(null)
        setCheckedIn(false)
        setRequired(null)

        const match = students.find(
            (s) => s.matricNumber.toLowerCase() === matricNumber.trim().toLowerCase()
        )

        if (!match) {
            setError('No student found with that matric number.')
            return
        }

        setStudentId(match.id)
        setChecking(true)

        try {
            const carryovers = await getOutstandingCourses(match.id)
            setOutstanding(carryovers)

            const preselect = {}
            carryovers.forEach((c) => {
                preselect[c.id] = true
            })
            setSelectedCourses(preselect)

            try {
                const limit = await getRequiredUnits(match.programmeId, match.levelId, semester)
                setRequired(limit.requiredUnits)
            } catch {
                setRequired(null)
            }

            setCheckedIn(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setChecking(false)
        }
    }

    async function handleSemesterChange(newSemester) {
        setSemester(newSemester)
        setRequired(null)

        if (selectedStudent) {
            try {
                const limit = await getRequiredUnits(
                    selectedStudent.programmeId,
                    selectedStudent.levelId,
                    newSemester
                )
                setRequired(limit.requiredUnits)
            } catch {
                setRequired(null)
            }
        }
    }

    function toggleCourse(courseId) {
        // carryover courses cannot be unchecked — they are mandatory
        if (outstandingIds.has(courseId)) return
        setSelectedCourses((prev) => ({ ...prev, [courseId]: !prev[courseId] }))
    }

    const selectedIds = Object.entries(selectedCourses)
        .filter(([, checked]) => checked)
        .map(([id]) => Number(id))

    const allSelectedCourses = [...outstanding, ...currentSemesterCourses]
        .filter((c, i, arr) => arr.findIndex((x) => x.id === c.id) === i)
        .filter((c) => selectedIds.includes(c.id))

    const runningTotal = allSelectedCourses.reduce((sum, c) => sum + c.creditUnit, 0)

    async function handleSubmit() {
        setError(null)
        setOutcome(null)

        if (selectedIds.length === 0) {
            setError('Select at least one course.')
            return
        }

        setSubmitting(true)

        try {
            const result = await registerMultiple(
                studentId,
                Number(academicSessionId),
                semester,
                selectedIds
            )
            setOutcome(result)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div className="max-w-2xl">
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Course Registration</h2>
            <p className="text-sm text-slate-500 mb-6">
                Enter your matric number to begin. Any outstanding carryover courses
                must be registered before current-semester courses can be selected.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm whitespace-pre-line">
                    {error}
                </div>
            )}

            {outcome && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    Registered {outcome.registered} course(s) ({outcome.skipped} already registered).
                    Total units: {outcome.totalUnits} / {outcome.maxUnits}
                </div>
            )}

            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                <div className="flex gap-3 items-end">
                    <div className="flex-1">
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            Matric Number
                        </label>
                        <input
                            value={matricNumber}
                            onChange={(e) => setMatricNumber(e.target.value)}
                            placeholder="e.g. KDUSEN23002"
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <button
                        onClick={handleFindStudent}
                        disabled={checking || !matricNumber.trim()}
                        className="bg-slate-800 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-slate-900 disabled:opacity-50"
                    >
                        {checking ? 'Checking…' : 'Find'}
                    </button>
                </div>

                {selectedStudent && (
                    <p className="text-sm text-slate-600">
                        {selectedStudent.fullName} — {selectedStudent.programmeName}, {selectedStudent.levelName}
                    </p>
                )}

                {checkedIn && (
                    <>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    Academic Session
                                </label>
                                <select
                                    value={academicSessionId}
                                    onChange={(e) => setAcademicSessionId(e.target.value)}
                                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                                >
                                    <option value="">Select…</option>
                                    {sessions.map((s) => (
                                        <option key={s.id} value={s.id}>{s.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                                <select
                                    value={semester}
                                    onChange={(e) => handleSemesterChange(e.target.value)}
                                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                                >
                                    <option value="FIRST">First</option>
                                    <option value="SECOND">Second</option>
                                </select>
                            </div>
                        </div>

                        {outstanding.length > 0 && (
                            <div>
                                <p className="text-sm font-semibold text-amber-700 mb-2">
                                    Outstanding Carryover Courses (must be registered)
                                </p>
                                <div className="border border-amber-300 bg-amber-50 rounded-md divide-y divide-amber-200">
                                    {outstanding.map((c) => (
                                        <div key={c.id} className="flex items-center gap-3 px-3 py-2 text-sm">
                                            <input type="checkbox" checked disabled />
                                            <span className="font-medium">{c.code}</span>
                                            <span className="text-slate-600">{c.title}</span>
                                            <span className="text-slate-400 ml-auto">{c.creditUnit} units</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div>
                            <p className="text-sm font-semibold text-slate-700 mb-2">
                                {semester === 'FIRST' ? 'First' : 'Second'} Semester Courses — {selectedStudent?.levelName}
                            </p>

                            {currentSemesterCourses.length === 0 ? (
                                <p className="text-sm text-slate-400">
                                    No matching courses found for this level/semester/programme.
                                </p>
                            ) : (
                                <div className="border border-slate-200 rounded-md divide-y divide-slate-100 max-h-72 overflow-y-auto">
                                    {currentSemesterCourses.map((c) => (
                                        <label
                                            key={c.id}
                                            className="flex items-center gap-3 px-3 py-2 text-sm hover:bg-slate-50 cursor-pointer"
                                        >
                                            <input
                                                type="checkbox"
                                                checked={!!selectedCourses[c.id]}
                                                onChange={() => toggleCourse(c.id)}
                                            />
                                            <span className="font-medium">{c.code}</span>
                                            <span className="text-slate-600">{c.title}</span>
                                            <span className="text-slate-400 ml-auto">{c.creditUnit} units</span>
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="flex justify-between items-center pt-2 border-t border-slate-200">
              <span
                  className={`text-sm font-medium ${
                      requiredUnits !== null && runningTotal === requiredUnits
                          ? 'text-green-700'
                          : 'text-slate-700'
                  }`}
              >
                Total selected: {runningTotal}
                  {requiredUnits !== null ? ` / ${requiredUnits} required` : ' units'}
              </span>
                            <button
                                onClick={handleSubmit}
                                disabled={submitting || !academicSessionId || selectedIds.length === 0}
                                className="bg-blue-600 text-white rounded-md px-6 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                            >
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