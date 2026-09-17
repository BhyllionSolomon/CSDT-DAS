import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import {
    getAllocationsForSession,
    uploadAllocationDocx,
} from '../services/allocationService'

function CourseAllocations() {
    const [sessions, setSessions] = useState([])
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [allocations, setAllocations] = useState([])
    const [loaded, setLoaded] = useState(false)
    const [loading, setLoading] = useState(true)
    const [loadingList, setLoadingList] = useState(false)

    const [file, setFile] = useState(null)
    const [uploading, setUploading] = useState(false)
    const [uploadResult, setUploadResult] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then(setSessions)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    async function handleLoad() {
        if (!academicSessionId) return
        setError(null)
        setLoadingList(true)
        try {
            const data = await getAllocationsForSession(academicSessionId, semester)
            setAllocations(data)
            setLoaded(true)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setLoadingList(false)
        }
    }

    async function handleUpload(e) {
        e.preventDefault()
        setError(null)
        setUploadResult(null)

        if (!file || !academicSessionId) {
            setError('Choose a session and a .docx file first.')
            return
        }

        setUploading(true)
        try {
            const result = await uploadAllocationDocx(academicSessionId, semester, file)
            setUploadResult(result)
            handleLoad()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setUploading(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Course Allocations</h2>
            <p className="text-sm text-slate-500 mb-6">
                The H.O.D can upload the departmental allocation document (.docx) to
                organize courses by lecturer. Re-uploading updates the list without
                overwriting a lecturer who has already claimed their course through CSDT-DAS.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {uploadResult && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    <p>Created: {uploadResult.created} · Updated: {uploadResult.updated}</p>
                    {uploadResult.errors.length > 0 && (
                        <ul className="mt-2 list-disc list-inside text-amber-700">
                            {uploadResult.errors.map((e, i) => (
                                <li key={i}>{e}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Session</label>
                    <select
                        value={academicSessionId}
                        onChange={(e) => setAcademicSessionId(e.target.value)}
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
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
                        onChange={(e) => setSemester(e.target.value)}
                        className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
                <button
                    onClick={handleLoad}
                    disabled={!academicSessionId || loadingList}
                    className="bg-slate-800 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-slate-900 disabled:opacity-50"
                >
                    {loadingList ? 'Loading…' : 'View Allocations'}
                </button>
            </div>

            <form
                onSubmit={handleUpload}
                className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap"
            >
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Upload Allocation Document (.docx)
                    </label>
                    <input
                        type="file"
                        accept=".docx"
                        onChange={(e) => setFile(e.target.files[0])}
                        className="text-sm"
                    />
                </div>
                <button
                    type="submit"
                    disabled={uploading}
                    className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {uploading ? 'Uploading…' : 'Upload Document'}
                </button>
            </form>

            {loaded && (
                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                        <tr>
                            <th className="px-4 py-3">Course</th>
                            <th className="px-4 py-3">Title</th>
                            <th className="px-4 py-3">Lecturer</th>
                            <th className="px-4 py-3">Status</th>
                            <th className="px-4 py-3">Phone</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                        {allocations.map((a) => (
                            <tr key={a.id}>
                                <td className="px-4 py-3 font-medium">{a.courseCode}</td>
                                <td className="px-4 py-3">{a.courseTitle}</td>
                                <td className="px-4 py-3">
                                    {a.lecturerName || (
                                        <span className="text-slate-400 italic">Not yet assigned</span>
                                    )}
                                    {a.lecturerId && (
                                        <span className="ml-2 text-green-600 text-xs">✓ claimed</span>
                                    )}
                                </td>
                                <td className="px-4 py-3">{a.status || '—'}</td>
                                <td className="px-4 py-3">{a.phoneNumber || '—'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>

                    {allocations.length === 0 && (
                        <p className="text-center text-slate-400 py-8">
                            No allocations found for this session and semester yet.
                        </p>
                    )}
                </div>
            )}
        </div>
    )
}

export default CourseAllocations