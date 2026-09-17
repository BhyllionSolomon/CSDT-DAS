import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAllStudents } from '../services/studentService'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import { createResult } from '../services/resultService'

function EnterResult() {
    const navigate = useNavigate()

    const [students, setStudents] = useState([])
    const [courses, setCourses] = useState([])
    const [sessions, setSessions] = useState([])

    const [studentId, setStudentId] = useState('')
    const [courseId, setCourseId] = useState('')
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [caScore, setCaScore] = useState('')
    const [examScore, setExamScore] = useState('')

    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState(null)
    const [success, setSuccess] = useState(null)

    useEffect(() => {
        Promise.all([getAllStudents(), getAllCourses(), getAllSessions()])
            .then(([studentsData, coursesData, sessionsData]) => {
                setStudents(studentsData)
                setCourses(coursesData)
                setSessions(sessionsData)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    function confirmSuspiciousValues(ca, exam) {

        if (ca === 30) {
            const ok = window.confirm(
                'CA score of 30 is the maximum possible — please confirm this is correct before saving.'
            )
            if (!ok) return false
        }

        if (exam === 69 || exam === 70) {
            const ok = window.confirm(
                `Exam score of ${exam} is very high (close to or at the maximum of 70) — please confirm this is correct before saving.`
            )
            if (!ok) return false
        }

        return true
    }

    async function handleSubmit(e) {
        e.preventDefault()
        setError(null)
        setSuccess(null)

        const ca = Number(caScore)
        const exam = Number(examScore)

        if (!confirmSuspiciousValues(ca, exam)) {
            return
        }

        setSubmitting(true)

        try {
            const created = await createResult({
                studentId: Number(studentId),
                courseId: Number(courseId),
                academicSessionId: Number(academicSessionId),
                semester,
                caScore: ca,
                examScore: exam,
            })

            setSuccess(
                `Result created: ${created.courseCode} — ${created.grade} (${created.totalScore}) — status PENDING`
            )

            setCourseId('')
            setCaScore('')
            setExamScore('')
        } catch (err) {
            const backendMessage = err.response?.data?.message
            setError(backendMessage || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading form data…</p>

    return (
        <div className="max-w-xl">
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Enter Result</h2>
            <p className="text-sm text-slate-500 mb-6">
                CA score maximum is 30, Exam score maximum is 70. Certain borderline
                totals (34, 44, 49, 59, 69, 79) are not allowed and must be corrected
                before saving.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {success && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm flex justify-between items-center">
                    <span>{success}</span>
                    <button
                        onClick={() => navigate(`/students/${studentId}/results`)}
                        className="text-green-800 underline text-xs font-medium"
                    >
                        View Results
                    </button>
                </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Student
                    </label>
                    <select
                        value={studentId}
                        onChange={(e) => setStudentId(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select a student…</option>
                        {students.map((s) => (
                            <option key={s.id} value={s.id}>
                                {s.matricNumber} — {s.fullName}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Course
                    </label>
                    <select
                        value={courseId}
                        onChange={(e) => setCourseId(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select a course…</option>
                        {courses.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.code} — {c.title} ({c.creditUnit} units)
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Academic Session
                    </label>
                    <select
                        value={academicSessionId}
                        onChange={(e) => setAcademicSessionId(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select a session…</option>
                        {sessions.map((s) => (
                            <option key={s.id} value={s.id}>
                                {s.name}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Semester
                    </label>
                    <select
                        value={semester}
                        onChange={(e) => setSemester(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            CA Score (max 30)
                        </label>
                        <input
                            type="number"
                            min="0"
                            max="30"
                            value={caScore}
                            onChange={(e) => setCaScore(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            Exam Score (max 70)
                        </label>
                        <input
                            type="number"
                            min="0"
                            max="70"
                            value={examScore}
                            onChange={(e) => setExamScore(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={submitting}
                    className="w-full bg-blue-600 text-white rounded-md py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {submitting ? 'Submitting…' : 'Submit Result'}
                </button>
            </form>
        </div>
    )
}

export default EnterResult