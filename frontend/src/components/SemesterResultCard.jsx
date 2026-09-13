function SemesterResultCard({ result }) {
    const { session, semester, courses, semesterSummary, previousCumulative, currentCumulative, remark } = result

    return (
        <div className="bg-white rounded-lg shadow mb-6 overflow-hidden">
            <div className="bg-slate-800 text-white px-4 py-3 flex justify-between items-center">
                <h3 className="font-semibold">
                    {session} — Semester {semester}
                </h3>
                <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                        remark.startsWith('GS')
                            ? 'bg-green-500 text-white'
                            : 'bg-red-500 text-white'
                    }`}
                >
          {remark}
        </span>
            </div>

            <table className="w-full text-sm text-left">
                <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                <tr>
                    <th className="px-4 py-2">Code</th>
                    <th className="px-4 py-2">Title</th>
                    <th className="px-4 py-2">Unit</th>
                    <th className="px-4 py-2">Score</th>
                    <th className="px-4 py-2">Grade</th>
                    <th className="px-4 py-2">GP</th>
                    <th className="px-4 py-2">WGP</th>
                    <th className="px-4 py-2">Status</th>
                </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                {courses.map((course) => (
                    <tr key={course.resultId}>
                        <td className="px-4 py-2 font-medium">{course.courseCode}</td>
                        <td className="px-4 py-2">{course.courseTitle}</td>
                        <td className="px-4 py-2">{course.creditUnit}</td>
                        <td className="px-4 py-2">{course.score}</td>
                        <td className="px-4 py-2">{course.letterGrade}</td>
                        <td className="px-4 py-2">{course.gradePoint}</td>
                        <td className="px-4 py-2">{course.weightedGradePoint}</td>
                        <td className="px-4 py-2">
                <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                        course.passed
                            ? 'bg-green-100 text-green-700'
                            : 'bg-red-100 text-red-700'
                    }`}
                >
                  {course.passed ? 'PASS' : 'FAIL'}
                </span>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            <div className="grid grid-cols-2 gap-4 p-4 bg-slate-50 text-sm">
                <div>
                    <p className="font-semibold text-slate-700 mb-1">
                        Semester Summary
                    </p>
                    <p>TUO: {semesterSummary.tuo} | TUP: {semesterSummary.tup} | TUF: {semesterSummary.tuf}</p>
                    <p>TWGP: {semesterSummary.twgp} | GPA: {semesterSummary.gpa}</p>
                </div>
                <div>
                    <p className="font-semibold text-slate-700 mb-1">
                        Cumulative (After This Semester)
                    </p>
                    <p>CTU: {currentCumulative.ctu} | TWGP: {currentCumulative.cumulativeTwgp}</p>
                    <p>
                        CGPA: <span className="font-bold">{currentCumulative.cgpa}</span>{' '}
                        — {currentCumulative.degreeClass}
                    </p>
                </div>
            </div>
        </div>
    )
}

export default SemesterResultCard