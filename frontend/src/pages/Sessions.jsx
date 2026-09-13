import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'

function Sessions() {
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllSessions()
            .then((data) => setSessions(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="text-slate-500">Loading academic sessions…</p>
    if (error) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-4">
                Academic Sessions
            </h2>

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