import { useEffect, useState } from 'react'
import { getAllCourses } from '../services/courseService'
import { getAllSessions } from '../services/sessionService'
import { uploadResultsCsv, uploadResultsDocx } from '../services/resultService'

function UploadResultsCsv() {
    const [courses, setCourses] = useState([])
    const [sessions, setSessions] = useState([])

    const [courseId, setCourseId] = useState('')
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [file, setFile] = useState(null)

    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)
    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        Promise.all([getAllCourses(), getAllSessions()])
            .then(([coursesData, sessionsData]) => {
                setCourses(coursesData)
                setSessions(sessionsData)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    async function handleSubmit(e) {
        e.preventDefault()
        setError(null)
        setResult(null)

        if (!file) {
            setError('Please choose a file.')
            return
        }

        setSubmitting(true)
        try {
            const isDocx = file.name.toLowerCase().endsWith('.docx')
            const data = isDocx
                ? await uploadResultsDocx(courseId, academicSessionId, semester, file)
                : await uploadResultsCsv(courseId, academicSessionId, semester, file)
            setResult(data)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div className="max-w-xl">
            <h2 className="text-2xl font-bold text-slate-800 mb-6">Upload Results</h2>

            <p className="text-sm text-slate-500 mb-4">
                Accepts a CSV file (<code>MatricNumber, FullName, Level, CAScore, ExamScore</code>)
                or a Word scoresheet (<code>.docx</code>) with a table containing Matric No, CA, and Exam columns.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {result && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    <p>Created: {result.created} · Updated: {result.updated}</p>
                    {result.errors.length > 0 && (
                        <ul className="mt-2 list-disc list-inside text-red-700">
                            {result.errors.map((e, i) => (
                                <li key={i}>{e}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Course</label>
                    <select
                        value={courseId}
                        onChange={(e) => setCourseId(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select a course…</option>
                        {courses.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.code} — {c.title}
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
                    <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                    <select
                        value={semester}
                        onChange={(e) => setSemester(e.target.value)}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        File (CSV or DOCX)
                    </label>
                    <input
                        type="file"
                        accept=".csv,.docx"
                        onChange={(e) => setFile(e.target.files[0])}
                        className="w-full text-sm"
                    />
                </div>

                <button
                    type="submit"
                    disabled={submitting}
                    className="w-full bg-blue-600 text-white rounded-md py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {submitting ? 'Uploading…' : 'Upload Results'}
                </button>
            </form>
        </div>
    )
}

export default UploadResultsCsv