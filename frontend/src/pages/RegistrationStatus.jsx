import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAllSessions } from '../services/sessionService'
import { getRegistrationStatus } from '../services/registrationStatusService'
import api from '../services/api'

async function getAllProgrammes() { return (await api.get('/programmes')).data }
async function getAllLevels() { return (await api.get('/levels')).data }
async function getAllStudentsForScope(programmeId, levelId) {
    const response = await api.get('/students')
    return response.data.filter((s) => s.programmeId === programmeId && s.levelId === levelId)
}

const LEVEL_CODES = ['100', '200', '300', '400']

function RegistrationStatus() {
    const { user } = useAuth()
    const isAdviser = user.role === 'LEVEL_ADVISER'

    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    const [programmeId, setProgrammeId] = useState(isAdviser ? user.programmeId : '')
    const [levelId, setLevelId] = useState('')
    const [academicSessionId, setAcademicSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')

    const [registered, setRegistered] = useState(null)
    const [allStudents, setAllStudents] = useState([])
    const [loadingStatus, setLoadingStatus] = useState(false)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels(), getAllSessions()])
            .then(([p, l, s]) => { setProgrammes(p); setLevels(l); setSessions(s) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    async function handleLoad() {
        if (!programmeId || !levelId || !academicSessionId) return
        setError(null)
        setLoadingStatus(true)
        try {
            const [status, students] = await Promise.all([
                getRegistrationStatus(programmeId, levelId, academicSessionId, semester),
                getAllStudentsForScope(Number(programmeId), Number(levelId)),
            ])
            setRegistered(status)
            setAllStudents(students)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setLoadingStatus(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const registeredIds = new Set((registered || []).map((r) => r.studentId))
    const notRegistered = allStudents.filter((s) => !registeredIds.has(s.id))

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Registration Status</h2>
            <p className="theme-text-muted text-sm mb-6">
                {isAdviser
                    ? `See which ${user.programmeName} students have completed course registration.`
                    : 'See which students across any programme have completed course registration.'}
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                {!isAdviser && (
                    <div>
                        <label className="block text-sm font-medium mb-1">Programme</label>
                        <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)}
                                className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Select…</option>
                            {programmes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </select>
                    </div>
                )}
                <div>
                    <label className="block text-sm font-medium mb-1">Level</label>
                    <select value={levelId} onChange={(e) => setLevelId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Session</label>
                    <select value={academicSessionId} onChange={(e) => setAcademicSessionId(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Select…</option>
                        {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium mb-1">Semester</label>
                    <select value={semester} onChange={(e) => setSemester(e.target.value)}
                            className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="FIRST">First</option>
                        <option value="SECOND">Second</option>
                    </select>
                </div>
                <button onClick={handleLoad} disabled={!programmeId || !levelId || !academicSessionId || loadingStatus}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {loadingStatus ? 'Loading…' : 'Check Status'}
                </button>
            </div>

            {registered !== null && (
                <div className="grid grid-cols-2 gap-4">
                    <div className="card overflow-hidden">
                        <div className="bg-green-600 text-white px-4 py-2 text-sm font-semibold">
                            Registered ({registered.length})
                        </div>
                        <table className="w-full text-sm text-left">
                            <tbody className="divide-y theme-border">
                            {registered.map((r) => (
                                <tr key={r.studentId}>
                                    <td className="px-4 py-2 font-medium">
                                        <Link to={`/students/${r.studentId}/results`} className="hover:underline" style={{ color: 'var(--accent)' }}>
                                            {r.matricNumber}
                                        </Link>
                                    </td>
                                    <td className="px-4 py-2">{r.fullName}</td>
                                    <td className="px-4 py-2 text-right theme-text-muted">{r.coursesRegistered} courses · {r.totalUnits} units</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {registered.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">No one has registered yet.</p>}
                    </div>

                    <div className="card overflow-hidden">
                        <div className="bg-amber-600 text-white px-4 py-2 text-sm font-semibold">
                            Not Yet Registered ({notRegistered.length})
                        </div>
                        <table className="w-full text-sm text-left">
                            <tbody className="divide-y theme-border">
                            {notRegistered.map((s) => (
                                <tr key={s.id}>
                                    <td className="px-4 py-2 font-medium">{s.matricNumber}</td>
                                    <td className="px-4 py-2">{s.fullName}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {notRegistered.length === 0 && <p className="text-center theme-text-muted py-6 text-sm">Everyone has registered.</p>}
                    </div>
                </div>
            )}
        </div>
    )
}
export default RegistrationStatus