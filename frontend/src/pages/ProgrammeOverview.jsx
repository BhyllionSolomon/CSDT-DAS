import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllStudents } from '../services/studentService'
import { calculateFullHistory } from '../services/resultService'
import api from '../services/api'

async function getAllProgrammes() {
    const response = await api.get('/programmes')
    return response.data
}

const LEVELS = ['100', '200', '300', '400']

function ProgrammeOverview() {
    const [programmes, setProgrammes] = useState([])
    const [students, setStudents] = useState([])
    const [programmeId, setProgrammeId] = useState('')
    const [level, setLevel] = useState('100')

    const [standing, setStanding] = useState(null)
    const [checking, setChecking] = useState(false)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllStudents()])
            .then(([p, s]) => {
                setProgrammes(p)
                setStudents(s)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    const filteredStudents = students.filter(
        (s) => String(s.programmeId) === String(programmeId) && s.levelCode === level
    )

    async function handleCheckStanding() {
        setError(null)
        setChecking(true)
        setStanding(null)

        const goodStanding = []
        const notGoodStanding = []
        const noResults = []

        for (const student of filteredStudents) {
            try {
                const history = await calculateFullHistory(student.id)
                const last = history[history.length - 1]
                if (last.remark.startsWith('GS')) {
                    goodStanding.push({ student, remark: last.remark, cgpa: last.currentCumulative.cgpa })
                } else {
                    notGoodStanding.push({ student, remark: last.remark, cgpa: last.currentCumulative.cgpa })
                }
            } catch {
                noResults.push(student)
            }
        }

        setStanding({ goodStanding, notGoodStanding, noResults })
        setChecking(false)
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Programme Overview</h2>
            <p className="text-sm text-slate-500 mb-6">
                Select a programme and level to monitor students, courses and academic
                standing. Level Advisers see this for their own programme.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}

            <div className="flex gap-2 mb-4 flex-wrap">
                {programmes.map((p) => (
                    <button
                        key={p.id}
                        onClick={() => { setProgrammeId(p.id); setStanding(null) }}
                        className={`px-4 py-2 rounded-md text-sm font-medium ${
                            String(programmeId) === String(p.id)
                                ? 'bg-blue-600 text-white'
                                : 'bg-white text-slate-600 border border-slate-300'
                        }`}
                    >
                        {p.name}
                    </button>
                ))}
            </div>

            {programmeId && (
                <>
                    <div className="flex gap-2 mb-6">
                        {LEVELS.map((l) => (
                            <button
                                key={l}
                                onClick={() => { setLevel(l); setStanding(null) }}
                                className={`px-4 py-2 rounded-md text-sm font-medium ${
                                    level === l
                                        ? 'bg-slate-800 text-white'
                                        : 'bg-white text-slate-600 border border-slate-300'
                                }`}
                            >
                                {l} Level
                            </button>
                        ))}
                    </div>

                    <div className="bg-white rounded-lg shadow p-4 mb-6 flex justify-between items-center">
            <span className="text-sm text-slate-600">
              {filteredStudents.length} student(s) in this programme/level
            </span>
                        <button
                            onClick={handleCheckStanding}
                            disabled={checking || filteredStudents.length === 0}
                            className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                        >
                            {checking ? 'Checking…' : 'Check Academic Standing'}
                        </button>
                    </div>

                    {standing && (
                        <div className="grid grid-cols-3 gap-4">
                            <div className="bg-white rounded-lg shadow overflow-hidden">
                                <div className="bg-green-600 text-white px-4 py-2 text-sm font-semibold">
                                    Good Standing ({standing.goodStanding.length})
                                </div>
                                <ul className="divide-y divide-slate-100">
                                    {standing.goodStanding.map(({ student, cgpa }) => (
                                        <li key={student.id} className="px-4 py-2 text-sm">
                                            <Link to={`/students/${student.id}/results`} className="text-blue-600 hover:underline">
                                                {student.matricNumber}
                                            </Link>
                                            <span className="text-slate-500 ml-2">CGPA {cgpa}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>

                            <div className="bg-white rounded-lg shadow overflow-hidden">
                                <div className="bg-red-600 text-white px-4 py-2 text-sm font-semibold">
                                    Not in Good Standing ({standing.notGoodStanding.length})
                                </div>
                                <ul className="divide-y divide-slate-100">
                                    {standing.notGoodStanding.map(({ student, remark, cgpa }) => (
                                        <li key={student.id} className="px-4 py-2 text-sm">
                                            <Link to={`/students/${student.id}/results`} className="text-blue-600 hover:underline">
                                                {student.matricNumber}
                                            </Link>
                                            <span className="text-slate-500 ml-2">CGPA {cgpa}</span>
                                            <p className="text-xs text-red-600">{remark}</p>
                                        </li>
                                    ))}
                                </ul>
                            </div>

                            <div className="bg-white rounded-lg shadow overflow-hidden">
                                <div className="bg-slate-500 text-white px-4 py-2 text-sm font-semibold">
                                    No Results Yet ({standing.noResults.length})
                                </div>
                                <ul className="divide-y divide-slate-100">
                                    {standing.noResults.map((student) => (
                                        <li key={student.id} className="px-4 py-2 text-sm">
                                            <Link to={`/students/${student.id}/results`} className="text-blue-600 hover:underline">
                                                {student.matricNumber}
                                            </Link>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default ProgrammeOverview