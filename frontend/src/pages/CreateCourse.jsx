import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createCourse, setCourseStatus } from '../services/courseService'
import api from '../services/api'

async function getAllProgrammes() { return (await api.get('/programmes')).data }
async function getAllLevels() { return (await api.get('/levels')).data }

const LEVEL_CODES = ['100', '200', '300', '400']
const STATUS_LABEL = { C: 'C – Compulsory', R: 'R – Required', E: 'E – Elective' }

function CreateCourse() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [loading, setLoading] = useState(true)

    const [selectedProgramme, setSelectedProgramme] = useState(null)
    const [selectedLevel, setSelectedLevel] = useState(null)

    const [code, setCode] = useState('')
    const [title, setTitle] = useState('')
    const [creditUnit, setCreditUnit] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [status, setStatus] = useState('C')

    const [confirming, setConfirming] = useState(false)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels()])
            .then(([p, l]) => { setProgrammes(p); setLevels(l) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    function resetToLevels() { setSelectedLevel(null); setResult(null); setError(null); setConfirming(false) }
    function resetToProgrammes() { setSelectedProgramme(null); resetToLevels() }

    function handleReview(e) {
        e.preventDefault()
        setError(null)
        if (!code.trim() || !title.trim() || !creditUnit) {
            setError('Fill in all fields before continuing.')
            return
        }
        setConfirming(true)
    }

    async function handleConfirm() {
        setSubmitting(true)
        setError(null)
        try {
            const created = await createCourse({
                code: code.trim().toUpperCase(), title: title.trim(),
                creditUnit: Number(creditUnit), departmentId: 1,
                levelId: selectedLevel.id, semester,
                programmeIds: [selectedProgramme.id],
            })

            if (status !== 'C') {
                await setCourseStatus(created.id, status)
            }

            setResult(created)
            setCode(''); setTitle(''); setCreditUnit(''); setStatus('C')
            setConfirming(false)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
            setConfirming(false)
        } finally { setSubmitting(false) }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <div className="flex justify-between items-center mb-2">
                <h2 className="text-2xl font-bold">Create Course</h2>
                <Link to="/courses" className="text-sm font-medium" style={{ color: 'var(--accent)' }}>
                    View / Edit All Courses →
                </Link>
            </div>
            <p className="theme-text-muted text-sm mb-6">
                Choose a programme, then a level, then fill in the course details.
            </p>

            <div className="flex items-center gap-2 text-sm mb-6">
                <button onClick={resetToProgrammes} className="font-medium" style={{ color: 'var(--accent)' }}>Programmes</button>
                {selectedProgramme && (<><span className="theme-text-muted">/</span><button onClick={resetToLevels} className="font-medium" style={{ color: 'var(--accent)' }}>{selectedProgramme.name}</button></>)}
                {selectedLevel && (<><span className="theme-text-muted">/</span><span className="font-medium">{selectedLevel.name}</span></>)}
            </div>

            {!selectedProgramme && (
                <div className="grid grid-cols-4 gap-4">
                    {programmes.map((p) => (
                        <button key={p.id} onClick={() => setSelectedProgramme(p)} className="card card-hover p-6 text-left">
                            <p className="font-semibold">{p.name}</p>
                            <p className="text-sm theme-text-muted">{p.code}</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && !selectedLevel && (
                <div className="grid grid-cols-4 gap-4">
                    {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => (
                        <button key={l.id} onClick={() => setSelectedLevel(l)} className="card card-hover p-6 text-center">
                            <p className="text-2xl font-bold">{l.code}</p>
                            <p className="text-sm theme-text-muted">Level</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && selectedLevel && (
                <div className="max-w-xl">
                    {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                    {result && (
                        <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                            Created: {result.code} for {selectedProgramme.name}, {selectedLevel.name}
                        </div>
                    )}

                    {!confirming ? (
                        <form onSubmit={handleReview} className="card p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium mb-1">Course Code</label>
                                <input value={code} onChange={(e) => setCode(e.target.value)} required
                                       className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Title</label>
                                <input value={title} onChange={(e) => setTitle(e.target.value)} required
                                       className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                            </div>
                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">Credit Unit</label>
                                    <input type="number" value={creditUnit} onChange={(e) => setCreditUnit(e.target.value)} required
                                           className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium mb-1">Status</label>
                                    <select value={status} onChange={(e) => setStatus(e.target.value)}
                                            className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="C">C – Compulsory</option>
                                        <option value="R">R – Required</option>
                                        <option value="E">E – Elective</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium mb-1">Semester</label>
                                    <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                            className="w-full border theme-border rounded-lg px-3 py-2 text-sm">
                                        <option value="FIRST">First</option>
                                        <option value="SECOND">Second</option>
                                    </select>
                                </div>
                            </div>
                            <button type="submit"
                                    className="w-full accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold btn-press">
                                Review Before Submitting
                            </button>
                        </form>
                    ) : (
                        <div className="card p-6">
                            <p className="font-semibold mb-4">Please confirm these details:</p>
                            <dl className="space-y-2 text-sm mb-6">
                                <div className="flex justify-between"><dt className="theme-text-muted">Programme</dt><dd className="font-medium">{selectedProgramme.name}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Level</dt><dd className="font-medium">{selectedLevel.name}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Code</dt><dd className="font-medium">{code.toUpperCase()}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Title</dt><dd className="font-medium">{title}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Credit Unit</dt><dd className="font-medium">{creditUnit}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Status</dt><dd className="font-medium">{STATUS_LABEL[status]}</dd></div>
                                <div className="flex justify-between"><dt className="theme-text-muted">Semester</dt><dd className="font-medium">{semester}</dd></div>
                            </dl>
                            <div className="flex gap-3">
                                <button onClick={() => setConfirming(false)}
                                        className="flex-1 border theme-border rounded-lg py-2.5 text-sm font-medium btn-press">
                                    ← Edit
                                </button>
                                <button onClick={handleConfirm} disabled={submitting}
                                        className="flex-1 accent-gradient text-white rounded-lg py-2.5 text-sm font-semibold btn-press disabled:opacity-50">
                                    {submitting ? 'Creating…' : 'Confirm & Create'}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default CreateCourse