import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllSessions } from '../services/sessionService'
import { calculateClassResults } from '../services/resultService'

const BORDERLINE_SCORES = [39, 49, 59, 69, 79]
const HIGH_ACHIEVER_THRESHOLD = 90

function ResultVerification() {
    const [sessions, setSessions] = useState([])
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [classResults, setClassResults] = useState(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions().then(setSessions).catch((err) => setError(err.message))
    }, [])

    function handleLoad() {
        if (!sessionId) return

        setLoading(true)
        setError(null)

        calculateClassResults(sessionId, semester)
            .then((data) => setClassResults(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }

    // Flatten every course-result across every student into one list,
    // since verification is done row-by-row across the whole class.
    const flatRows = []
    if (classResults) {
        for (const studentResult of classResults) {
            for (const course of studentResult.courses) {
                flatRows.push({
                    studentId: studentResult.studentId,
                    matricNumber: studentResult.matricNumber,
                    studentName: studentResult.studentName,
                    ...course,
                })
            }
        }
    }

    const gradeCounts = flatRows.reduce((acc, row) => {
        acc[row.letterGrade] = (acc[row.letterGrade] || 0) + 1
        return acc
    }, {})

    function isBorderline(score) {
        return BORDERLINE_SCORES.includes(Math.floor(score))
    }

    function isHighAchiever(score) {
        return score >= HIGH_ACHIEVER_THRESHOLD
    }

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-6">
                Result Verification
            </h2>

            <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Academic Session
                    </label>
                    <select
                        value={sessionId}
                        onChange={(e) => setSessionId(e.target.value)}
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select…</option>
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
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>

                <button
                    onClick={handleLoad}
                    disabled={!sessionId || loading}
                    className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {loading ? 'Loading…' : 'Load Results'}
                </button>
            </div>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {classResults && (
                <>
                    <div className="bg-white rounded-lg shadow p-4 mb-6">
                        <p className="font-semibold text-slate-700 mb-2">
                            Grade Distribution ({flatRows.length} results)
                        </p>
                        <div className="flex gap-4 text-sm">
                            {['A', 'B', 'C', 'D', 'E', 'F'].map((grade) => (
                                <span key={grade}>
                  <span className="font-bold">{grade}</span>:{' '}
                                    {gradeCounts[grade] || 0}
                </span>
                            ))}
                        </div>
                    </div>

                    <div className="flex gap-4 mb-4 text-xs">
            <span className="flex items-center gap-1">
              <span className="w-3 h-3 bg-yellow-200 inline-block rounded"></span>
              Borderline (score ends in 39/49/59/69/79)
            </span>
                        <span className="flex items-center gap-1">
              <span className="w-3 h-3 bg-blue-200 inline-block rounded"></span>
              High achiever (≥90) — click to view history
            </span>
                    </div>

                    <div className="bg-white rounded-lg shadow overflow-hidden">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                            <tr>
                                <th className="px-4 py-3">Matric No.</th>
                                <th className="px-4 py-3">Name</th>
                                <th className="px-4 py-3">Course</th>
                                <th className="px-4 py-3">Score</th>
                                <th className="px-4 py-3">Grade</th>
                                <th className="px-4 py-3">GP</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                            {flatRows.map((row) => {
                                const borderline = isBorderline(row.score)
                                const highAchiever = isHighAchiever(row.score)

                                return (
                                    <tr
                                        key={row.resultId}
                                        className={
                                            borderline
                                                ? 'bg-yellow-50'
                                                : highAchiever
                                                    ? 'bg-blue-50'
                                                    : ''
                                        }
                                    >
                                        <td className="px-4 py-3 font-medium">
                                            {row.matricNumber}
                                        </td>
                                        <td className="px-4 py-3">
                                            {highAchiever ? (
                                                <Link
                                                    to={`/students/${row.studentId}/results`}
                                                    className="text-blue-700 underline font-medium"
                                                >
                                                    {row.studentName}
                                                </Link>
                                            ) : (
                                                row.studentName
                                            )}
                                        </td>
                                        <td className="px-4 py-3">
                                            {row.courseCode} — {row.courseTitle}
                                        </td>
                                        <td className="px-4 py-3 font-medium">
                                            {row.score}
                                            {borderline && (
                                                <span className="ml-2 text-yellow-700 text-xs font-semibold">
                            ⚠ BORDERLINE
                          </span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3">{row.letterGrade}</td>
                                        <td className="px-4 py-3">{row.gradePoint}</td>
                                    </tr>
                                )
                            })}
                            </tbody>
                        </table>

                        {flatRows.length === 0 && (
                            <p className="text-center text-slate-400 py-8">
                                No approved results found for this session and semester.
                            </p>
                        )}
                    </div>
                </>
            )}
        </div>
    )
}

export default ResultVerification