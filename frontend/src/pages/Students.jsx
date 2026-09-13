import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllStudents } from '../services/studentService'

function Students() {
    const [students, setStudents] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllStudents()
            .then((data) => setStudents(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="text-slate-500">Loading students…</p>
    if (error) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-4">Students</h2>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                    <tr>
                        <th className="px-4 py-3">Matric No.</th>
                        <th className="px-4 py-3">Name</th>
                        <th className="px-4 py-3">Department</th>
                        <th className="px-4 py-3">Programme</th>
                        <th className="px-4 py-3">Level</th>
                        <th className="px-4 py-3">Status</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                    {students.map((student) => (
                        <tr key={student.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-medium text-slate-800">
                                <Link
                                    to={`/students/${student.id}/results`}
                                    className="text-blue-600 hover:underline"
                                >
                                    {student.matricNumber}
                                </Link>
                            </td>
                            <td className="px-4 py-3">{student.fullName}</td>
                            <td className="px-4 py-3">{student.departmentCode}</td>
                            <td className="px-4 py-3">{student.programmeCode}</td>
                            <td className="px-4 py-3">{student.levelCode}</td>
                            <td className="px-4 py-3">
                  <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                          student.status === 'ACTIVE'
                              ? 'bg-green-100 text-green-700'
                              : 'bg-slate-100 text-slate-600'
                      }`}
                  >
                    {student.status}
                  </span>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                {students.length === 0 && (
                    <p className="text-center text-slate-400 py-8">No students found.</p>
                )}
            </div>
        </div>
    )
}

export default Students