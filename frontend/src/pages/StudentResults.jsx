import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getStudent } from '../services/studentService'
import {
    getStudentResults,
    calculateFullHistory,
    approveResult,
    rejectResult,
} from '../services/resultService'
import SemesterResultCard from '../components/SemesterResultCard'

function StudentResults() {
    const { studentId } = useParams()

    const [student, setStudent] = useState(null)
    const [rawResults, setRawResults] = useState([])
    const [history, setHistory] = useState([])
    const [view, setView] = useState('raw') // 'raw' or 'history'
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    function loadData() {
        setLoading(true)
        setError(null)

        Promise.all([
            getStudent(studentId),
            getStudentResults(studentId),
        ])
            .then(([studentData, resultsData]) => {
                setStudent(studentData)
                setRawResults(resultsData)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        loadData()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [studentId])

    function loadHistory() {
        setLoading(true)
        setError(null)

        calculateFullHistory(studentId)
            .then((data) => {
                setHistory(data)
                setView('history')
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }

    async function handleApprove(resultId) {
        try {
            await approveResult(resultId)
            loadData()
        } catch (err) {
            setError(err.message)
        }
    }

    async function handleReject(resultId) {
        try {
            await rejectResult(resultId)
            loadData()
        } catch (err) {
            setError(err.message)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>
    if (error) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <div className="mb-6">
                <h2 className="text-2xl font-bold text-slate-800">
                    {student?.fullName}
                </h2>
                <p className="text-slate-500">
                    {student?.matricNumber} · {student?.programmeName} · {student?.levelName}
                </p>
            </div>

            <div className="flex gap-2 mb-6">
                <button
                    onClick={() => setView('raw')}
                    className={`px-4 py-2 rounded-md text-sm font-medium ${
                        view === 'raw'
                            ? 'bg-blue-600 text-white'
                            : 'bg-white text-slate-600 border border-slate-300'
                    }`}
                >
                    All Results
                </button>
                <button
                    onClick={loadHistory}
                    className={`px-4 py-2 rounded-md text-sm font-medium ${
                        view === 'history'
                            ? 'bg-blue-600 text-white'
                            : 'bg-white text-slate-600 border border-slate-300'
                    }`}
                >
                    Full Academic History / CGPA
                </button>
            </div>

            {view === 'raw' && (
                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                        <tr>
                            <th className="px-4 py-3">Session</th>
                            <th className="px-4 py-3">Semester</th>
                            <th className="px-4 py-3">Course</th>
                            <th className="px-4 py-3">Score</th>
                            <th className="px-4 py-3">Grade</th>
                            <th className="px-4 py-3">Status</th>
                            <th className="px-4 py-3">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                        {rawResults.map((result) => (
                            <tr key={result.id} className="hover:bg-slate-50">
                                <td className="px-4 py-3">{result.academicSessionName}</td>
                                <td className="px-4 py-3">{result.semester}</td>
                                <td className="px-4 py-3 font-medium">
                                    {result.courseCode} — {result.courseTitle}
                                </td>
                                <td className="px-4 py-3">{result.totalScore}</td>
                                <td className="px-4 py-3">{result.grade}</td>
                                <td className="px-4 py-3">
                    <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${
                            result.status === 'APPROVED'
                                ? 'bg-green-100 text-green-700'
                                : result.status === 'REJECTED'
                                    ? 'bg-red-100 text-red-700'
                                    : 'bg-yellow-100 text-yellow-700'
                        }`}
                    >
                      {result.status}
                    </span>
                                </td>
                                <td className="px-4 py-3 space-x-2">
                                    {result.status !== 'APPROVED' && (
                                        <button
                                            onClick={() => handleApprove(result.id)}
                                            className="text-green-600 hover:underline text-xs font-medium"
                                        >
                                            Approve
                                        </button>
                                    )}
                                    {result.status !== 'REJECTED' && (
                                        <button
                                            onClick={() => handleReject(result.id)}
                                            className="text-red-600 hover:underline text-xs font-medium"
                                        >
                                            Reject
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    {rawResults.length === 0 && (
                        <p className="text-center text-slate-400 py-8">
                            No results found for this student.
                        </p>
                    )}
                </div>
            )}

            {view === 'history' && (
                <div>
                    {history.map((semesterResult, index) => (
                        <SemesterResultCard key={index} result={semesterResult} />
                    ))}

                    {history.length === 0 && (
                        <p className="text-center text-slate-400 py-8">
                            No approved results found — approve at least one result to see
                            calculated history.
                        </p>
                    )}
                </div>
            )}
        </div>
    )
}

export default StudentResults