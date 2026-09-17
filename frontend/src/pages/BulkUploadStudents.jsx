import { useState } from 'react'
import { uploadStudentsCsv } from '../services/studentService'

function BulkUploadStudents() {
    const [departmentId, setDepartmentId] = useState('1')
    const [academicSessionId, setAcademicSessionId] = useState('1')
    const [file, setFile] = useState(null)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)
    const [submitting, setSubmitting] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError(null)
        setResult(null)

        if (!file) {
            setError('Please choose a CSV file.')
            return
        }

        setSubmitting(true)
        try {
            const data = await uploadStudentsCsv(departmentId, academicSessionId, file)
            setResult(data)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="max-w-xl">
            <h2 className="text-2xl font-bold text-slate-800 mb-6">Bulk Upload Students</h2>

            <p className="text-sm text-slate-500 mb-4">
                CSV columns required: <code>MatricNumber, FullName, ProgrammeCode, LevelCode</code>
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {result && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    <p>Created: {result.created} · Skipped (already existed): {result.skipped}</p>
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
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Department ID
                    </label>
                    <input
                        type="number"
                        value={departmentId}
                        onChange={(e) => setDepartmentId(e.target.value)}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        Academic Session ID
                    </label>
                    <input
                        type="number"
                        value={academicSessionId}
                        onChange={(e) => setAcademicSessionId(e.target.value)}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">
                        CSV File
                    </label>
                    <input
                        type="file"
                        accept=".csv"
                        onChange={(e) => setFile(e.target.files[0])}
                        className="w-full text-sm"
                    />
                </div>

                <button
                    type="submit"
                    disabled={submitting}
                    className="w-full bg-blue-600 text-white rounded-md py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {submitting ? 'Uploading…' : 'Upload Students'}
                </button>
            </form>
        </div>
    )
}

export default BulkUploadStudents