import { useEffect, useState } from 'react'
import { getAllSessions, createSession } from '../services/sessionService'

function Sessions() {
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [name, setName] = useState('')
    const [startDate, setStartDate] = useState('')
    const [endDate, setEndDate] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [formError, setFormError] = useState(null)

    function load() {
        setLoading(true)
        getAllSessions()
            .then((data) => setSessions(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        load()
    }, [])

    async function handleCreate(e) {
        e.preventDefault()
        setFormError(null)
        setSubmitting(true)

        try {
            await createSession(name, startDate, endDate)
            setName('')
            setStartDate('')
            setEndDate('')
            load()
        } catch (err) {
            setFormError(err.response?.data?.message || err.message)
        } finally {
            setSubmitting(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading academic sessions…</p>
    if (error) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-4">
                Academic Sessions
            </h2>

            <div className="bg-white rounded-lg shadow p-4 mb-6">
                <h3 className="text-sm font-semibold text-slate-700 mb-3">Create New Session</h3>

                {formError && (
                    <div className="mb-3 px-3 py-2 rounded-md bg-red-50 text-red-700 text-sm">
                        {formError}
                    </div>
                )}

                <form onSubmit={handleCreate} className="flex gap-3 items-end flex-wrap">
                    <div>
                        <label className="block text-xs font-medium text-slate-700 mb-1">
                            Session Name
                        </label>
                        <input
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="e.g. 2026/2027"
                            required
                            className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-700 mb-1">
                            Start Date
                        </label>
                        <input
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            required
                            className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-700 mb-1">
                            End Date
                        </label>
                        <input
                            type="date"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            required
                            className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={submitting}
                        className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                    >
                        {submitting ? 'Creating…' : 'Create Session'}
                    </button>
                </form>
            </div>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                    <tr>
                        <th className="px-4 py-3">Name</th>
                        <th className="px-4 py-3">Start Date</th>
                        <th className="px-4 py-3">End Date</th>
                        <th className="px-4 py-3">Current</th>
                        <th className="px-4 py-3">Status</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                    {sessions.map((session) => (
                        <tr key={session.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-medium text-slate-800">
                                {session.name}
                            </td>
                            <td className="px-4 py-3">{session.startDate}</td>
                            <td className="px-4 py-3">{session.endDate}</td>
                            <td className="px-4 py-3">
                                {session.current && (
                                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                      CURRENT
                    </span>
                                )}
                            </td>
                            <td className="px-4 py-3">
                  <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                          session.active
                              ? 'bg-green-100 text-green-700'
                              : 'bg-slate-100 text-slate-600'
                      }`}
                  >
                    {session.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                {sessions.length === 0 && (
                    <p className="text-center text-slate-400 py-8">
                        No academic sessions found.
                    </p>
                )}
            </div>
        </div>
    )
}

export default Sessions