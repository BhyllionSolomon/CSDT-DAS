import { useEffect, useState } from 'react'
import { getAllCourses } from '../services/courseService'

function Courses() {
    const [courses, setCourses] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        getAllCourses()
            .then((data) => setCourses(data))
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <p className="text-slate-500">Loading courses…</p>
    if (error) return <p className="text-red-600">Error: {error}</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-4">Courses</h2>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                    <tr>
                        <th className="px-4 py-3">Code</th>
                        <th className="px-4 py-3">Title</th>
                        <th className="px-4 py-3">Unit</th>
                        <th className="px-4 py-3">Department</th>
                        <th className="px-4 py-3">Level</th>
                        <th className="px-4 py-3">Semester</th>
                        <th className="px-4 py-3">Programmes</th>
                        <th className="px-4 py-3">Status</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                    {courses.map((course) => (
                        <tr key={course.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-medium text-slate-800">
                                {course.code}
                            </td>
                            <td className="px-4 py-3">{course.title}</td>
                            <td className="px-4 py-3">{course.creditUnit}</td>
                            <td className="px-4 py-3">{course.departmentCode}</td>
                            <td className="px-4 py-3">{course.levelCode}</td>
                            <td className="px-4 py-3">{course.semester}</td>
                            <td className="px-4 py-3">
                                <div className="flex flex-wrap gap-1">
                                    {course.programmes && course.programmes.length > 0 ? (
                                        course.programmes.map((programme) => (
                                            <span
                                                key={programme.id}
                                                className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-700"
                                            >
                          {programme.code}
                        </span>
                                        ))
                                    ) : (
                                        <span className="text-slate-400 text-xs">
                        Unassigned
                      </span>
                                    )}
                                </div>
                            </td>
                            <td className="px-4 py-3">
                  <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                          course.active
                              ? 'bg-green-100 text-green-700'
                              : 'bg-slate-100 text-slate-600'
                      }`}
                  >
                    {course.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                {courses.length === 0 && (
                    <p className="text-center text-slate-400 py-8">No courses found.</p>
                )}
            </div>
        </div>
    )
}

export default Courses