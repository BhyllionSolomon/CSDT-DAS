import { useState } from 'react'
import { createCourse } from '../services/courseService'

function CreateCourse() {
    const [code, setCode] = useState('')
    const [title, setTitle] = useState('')
    const [creditUnit, setCreditUnit] = useState('')
    const [departmentId, setDepartmentId] = useState('1')
    const [levelId, setLevelId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)
    const [submitting, setSubmitting] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError(null)
        setResult(null)
        setSubmitting(true)

        try {
            const created = await createCourse({
                code,
                title,
                creditUnit: Number(creditUnit),
                departmentId: Number(departmentId),
                levelId: Number(levelId),
                semester,
            })
            setResult(created)
            setCode('')
            setTitle('')
            setCreditUnit('')
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="max-w-xl">
            <h2 className="text-2xl font-bold text-slate-800 mb-6">Create Course</h2>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            {result && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    Created: {result.code} (id: {result.id})
                </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Course Code</label>
                    <input
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Title</label>
                    <input
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        required
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    />
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Credit Unit</label>
                        <input
                            type="number"
                            value={creditUnit}
                            onChange={(e) => setCreditUnit(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Level ID</label>
                        <input
                            type="number"
                            value={levelId}
                            onChange={(e) => setLevelId(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                            placeholder="e.g. 3 = 300L, 4 = 400L"
                        />
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Department ID</label>
                        <input
                            type="number"
                            value={departmentId}
                            onChange={(e) => setDepartmentId(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
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
                </div>

                <button
                    type="submit"
                    disabled={submitting}
                    className="w-full bg-blue-600 text-white rounded-md py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                    {submitting ? 'Creating…' : 'Create Course'}
                </button>
            </form>
        </div>
    )
}

export default CreateCourse