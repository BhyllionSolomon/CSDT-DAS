import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api'
import { getAllCourses } from '../services/courseService'

const LEVEL_CODES = ['100', '200', '300', '400']
const SEMESTERS = [['FIRST', 'First semester'], ['SECOND', 'Second semester']]
const msg = (err) => err.response?.data?.message || err.message

function CurriculumPlanner() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [courses, setCourses] = useState([])
    const [programmeId, setProgrammeId] = useState('')
    const [limits, setLimits] = useState({})
    const [adding, setAdding] = useState({})
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    useEffect(() => {
        Promise.all([api.get('/programmes'), api.get('/levels'), getAllCourses()])
            .then(([p, l, c]) => {
                setProgrammes(p.data)
                setLevels(l.data.filter((x) => LEVEL_CODES.includes(x.code)).sort((a, b) => a.code.localeCompare(b.code)))
                setCourses(c)
                if (p.data.length) setProgrammeId(String(p.data[0].id))
            })
            .catch((e) => setError(msg(e)))
            .finally(() => setLoading(false))
    }, [])

    useEffect(() => {
        if (!programmeId || levels.length === 0) return undefined
        let cancelled = false

        Promise.all(levels.flatMap((l) => SEMESTERS.map(([s]) =>
            api.get(`/unit-limits/programme/${programmeId}/level/${l.id}/semester/${s}`)
                .then((r) => [`${l.id}-${s}`, r.data?.requiredUnits ?? r.data?.required ?? null])
                .catch(() => [`${l.id}-${s}`, null])
        ))).then((pairs) => { if (!cancelled) setLimits(Object.fromEntries(pairs)) })

        return () => { cancelled = true }
    }, [programmeId, levels])

    const pid = Number(programmeId)
    const linked = (c) => (c.programmes || []).some((p) => p.id === pid)

    async function relink(course, add) {
        const current = (course.programmes || []).map((p) => p.id)
        const ids = add ? [...new Set([...current, pid])] : current.filter((id) => id !== pid)

        setError(null); setMessage(null); setBusy(true)
        try {
            await api.put(`/courses/${course.id}/programmes`, { programmeIds: ids })
            setCourses(await getAllCourses())
            setMessage(add ? `${course.code} added.` : `${course.code} removed from this programme.`)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function handleRemove(course) {
        if (window.confirm(`Remove ${course.code} from this programme? Students in it will no longer see the course.`)) {
            relink(course, false)
        }
    }

    async function handleAdd(key, level, semester) {
        const course = courses.find((c) => c.id === Number(adding[key]))
        if (!course) return
        await relink(course, true)
        setAdding({ ...adding, [key]: '' })
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Curriculum by Level</h2>
            <p className="theme-text-muted text-sm mb-4">
                Choose the courses for each programme, level and semester. Students see only the courses for their own programme, level and the
                semester they pick when registering. Totals are compared with the required units.{' '}
                <Link to="/set-unit-requirements" className="font-medium" style={{ color: 'var(--accent)' }}>Set required units</Link>
                {' · '}
                <Link to="/create-course" className="font-medium" style={{ color: 'var(--accent)' }}>Create a new course</Link>
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="flex flex-wrap gap-2 mb-6">
                {programmes.map((p) => (
                    <button key={p.id} onClick={() => setProgrammeId(String(p.id))}
                            className={`px-4 py-2 text-sm font-medium rounded-lg border theme-border btn-press ${
                                String(p.id) === programmeId ? 'accent-gradient text-white border-transparent' : ''}`}>
                        {p.name}
                    </button>
                ))}
            </div>

            <div className="space-y-6">
                {levels.map((level) => (
                    <div key={level.id} className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{level.name}</div>
                        <div className="grid md:grid-cols-2 divide-y md:divide-y-0 md:divide-x theme-border">
                            {SEMESTERS.map(([sem, label]) => {
                                const key = `${level.id}-${sem}`
                                const inPlan = courses.filter((c) => c.active !== false && c.levelId === level.id && c.semester === sem && linked(c))
                                const available = courses.filter((c) => c.active !== false && c.levelId === level.id && c.semester === sem && !linked(c))
                                const total = inPlan.reduce((sum, c) => sum + (c.creditUnit || 0), 0)
                                const required = limits[key]
                                const off = required != null && total !== required

                                return (
                                    <div key={key} className="p-4">
                                        <div className="flex justify-between items-baseline mb-2">
                                            <p className="font-semibold">{label}</p>
                                            <p className={`text-xs ${off ? 'text-amber-700 font-medium' : 'theme-text-muted'}`}>
                                                {total} unit(s){required != null ? ` · required ${required}` : ' · required units not set'}
                                            </p>
                                        </div>

                                        {inPlan.length === 0 ? (
                                            <p className="text-sm theme-text-muted mb-3">No courses yet.</p>
                                        ) : (
                                            <ul className="divide-y theme-border mb-3">
                                                {inPlan.map((c) => (
                                                    <li key={c.id} className="py-1.5 flex items-center gap-3 text-sm">
                                                        <span className="font-medium w-20 shrink-0">{c.code}</span>
                                                        <span className="flex-1">{c.title}</span>
                                                        <span className="theme-text-muted text-xs">{c.creditUnit}u · {c.status || 'C'}</span>
                                                        <button onClick={() => handleRemove(c)} disabled={busy} className="text-xs text-red-600 hover:underline">Remove</button>
                                                    </li>
                                                ))}
                                            </ul>
                                        )}

                                        <div className="flex gap-2">
                                            <select value={adding[key] || ''} onChange={(e) => setAdding({ ...adding, [key]: e.target.value })}
                                                    className="flex-1 border theme-border rounded-lg px-2 py-1.5 text-sm">
                                                <option value="">Add a course…</option>
                                                {available.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}
                                            </select>
                                            <button onClick={() => handleAdd(key, level, sem)} disabled={busy || !adding[key]}
                                                    className="accent-gradient text-white rounded-lg px-3 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                                                Add
                                            </button>
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}

export default CurriculumPlanner