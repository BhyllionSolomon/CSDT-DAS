import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Printer } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getAllStudents } from '../services/studentService'
import { calculateFullHistory } from '../services/resultService'
import SemesterResultCard from '../components/SemesterResultCard'

function StudentReport() {
    const { user } = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()
    const studentId = searchParams.get('studentId')

    const [students, setStudents] = useState([])
    const [query, setQuery] = useState('')
    const [history, setHistory] = useState(null)
    const [courseFilter, setCourseFilter] = useState('')
    const [loadingList, setLoadingList] = useState(true)
    const [loadingHistory, setLoadingHistory] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllStudents()
            .then((list) => setStudents(
                user.role === 'LEVEL_ADVISER' ? list.filter((s) => s.programmeId === user.programmeId) : list
            ))
            .catch((err) => setError(err.response?.data?.message || err.message))
            .finally(() => setLoadingList(false))
    }, [user.role, user.programmeId])

    useEffect(() => {
        if (!studentId) { setHistory(null); return }
        setError(null)
        setHistory(null)
        setCourseFilter('')
        setLoadingHistory(true)
        calculateFullHistory(studentId)
            .then(setHistory)
            .catch((err) => setError(err.response?.data?.message || err.message))
            .finally(() => setLoadingHistory(false))
    }, [studentId])

    const student = students.find((s) => String(s.id) === String(studentId))
    const term = query.trim().toLowerCase()
    const matches = term
        ? students.filter((s) => s.matricNumber.toLowerCase().includes(term) || s.fullName.toLowerCase().includes(term)).slice(0, 8)
        : []

    const codes = history ? [...new Set(history.flatMap((h) => h.courses.map((c) => c.courseCode)))].sort() : []

    const shown = history
        ? history
            .map((h) => ({ ...h, courses: courseFilter ? h.courses.filter((c) => c.courseCode === courseFilter) : h.courses }))
            .filter((h) => h.courses.length > 0)
        : []

    const latest = history && history.length > 0 ? history[history.length - 1].currentCumulative : null

    if (loadingList) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2 no-print">Student Result</h2>
            <p className="theme-text-muted text-sm mb-6 no-print">
                Search for a student to see their full academic record, then print it. Use the course filter to
                show every attempt of one course.
            </p>

            {error && <div className="no-print mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="no-print card p-4 mb-6 flex gap-4 items-start flex-wrap">
                <div className="relative">
                    <label className="block text-sm font-medium mb-1">Find student</label>
                    <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Matric number or name"
                           className="border theme-border rounded-lg px-3 py-2 text-sm w-72" />
                    {matches.length > 0 && (
                        <ul className="absolute z-10 mt-1 w-full card divide-y theme-border max-h-64 overflow-y-auto">
                            {matches.map((s) => (
                                <li key={s.id}>
                                    <button type="button"
                                            onClick={() => { setSearchParams({ studentId: String(s.id) }); setQuery('') }}
                                            className="w-full text-left px-3 py-2 text-sm hover:bg-black/5">
                                        {s.matricNumber} — {s.fullName}
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                {history && codes.length > 0 && (
                    <div>
                        <label className="block text-sm font-medium mb-1">Course</label>
                        <select value={courseFilter} onChange={(e) => setCourseFilter(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">All courses</option>
                            {codes.map((c) => <option key={c} value={c}>{c}</option>)}
                        </select>
                    </div>
                )}

                {history && (
                    <button onClick={() => window.print()}
                            className="flex items-center gap-2 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press mt-6">
                        <Printer size={15} /> Print
                    </button>
                )}
            </div>

            {loadingHistory && <p className="theme-text-muted">Loading record…</p>}

            {history && (
                <div>
                    <div className="mb-4">
                        <p className="font-bold text-lg">KOLADAISI UNIVERSITY, IBADAN — STUDENT ACADEMIC RECORD</p>
                        <p className="text-sm">
                            {student ? `${student.fullName} · ${student.matricNumber} · ${student.programmeName} · ${student.levelName}` : `Student #${studentId}`}
                        </p>
                        {latest && (
                            <p className="text-sm font-semibold mt-1">
                                Cumulative CGPA: {Number(latest.cgpa).toFixed(2)} — {latest.degreeClass}
                            </p>
                        )}
                        {courseFilter && <p className="text-sm theme-text-muted">Showing course: {courseFilter}</p>}
                    </div>

                    {shown.map((h, i) => <SemesterResultCard key={i} result={h} />)}
                    {shown.length === 0 && <p className="theme-text-muted">No records for this selection.</p>}
                </div>
            )}
        </div>
    )
}

export default StudentReport