import { useEffect, useMemo, useState } from 'react'
import { Download } from 'lucide-react'
import { getCarryOverMonitor } from '../services/carryOverService'

const msg = (err) => err.response?.data?.message || err.message

const STATUS = {
    REGISTERED: ['Registered', 'bg-green-100 text-green-700'],
    NOT_REGISTERED: ['Not registered', 'bg-red-100 text-red-700'],
    LATER: ['Later this session', 'bg-slate-200 text-slate-600'],
}

function CarryOverMonitor() {
    const [data, setData] = useState(null)
    const [level, setLevel] = useState('ALL')
    const [onlyAttention, setOnlyAttention] = useState(false)
    const [query, setQuery] = useState('')
    const [open, setOpen] = useState(new Set())
    const [error, setError] = useState(null)

    useEffect(() => {
        getCarryOverMonitor().then(setData).catch((e) => setError(msg(e)))
    }, [])

    const levels = useMemo(() => (data ? [...new Set(data.students.map((s) => s.level))].sort() : []), [data])

    const shown = (data?.students || []).filter((s) =>
        (level === 'ALL' || s.level === level) &&
        (!onlyAttention || s.notRegisteredNow > 0) &&
        (!query.trim() || `${s.matricNumber} ${s.fullName}`.toLowerCase().includes(query.trim().toLowerCase())))

    function toggle(id) {
        const next = new Set(open)
        next.has(id) ? next.delete(id) : next.add(id)
        setOpen(next)
    }

    function download() {
        const head = ['Matric number', 'Name', 'Programme', 'Level', 'Course', 'Semester', 'Status']
        const rows = shown.flatMap((s) => s.courses.map((c) => [s.matricNumber, s.fullName, s.programme, s.level, c.code, c.semester, STATUS[c.status][0]]))
        const csv = [head, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'carry-overs.csv'
        a.click()
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Carry-Over Monitor</h2>
            <p className="theme-text-muted text-sm mb-6">
                Students with carry-over courses, and whether they have registered for them this session. A carry-over for the current semester that is not yet registered is shown in red.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {!data && !error && <p className="theme-text-muted">Checking every student… this can take a moment.</p>}

            {data && (
                <>
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="card p-4"><p className="text-2xl font-bold">{data.checked}</p><p className="text-xs theme-text-muted">active students checked</p></div>
                        <div className="card p-4"><p className="text-2xl font-bold">{data.withCarryOvers}</p><p className="text-xs theme-text-muted">have carry-overs</p></div>
                        <div className="card p-4"><p className={`text-2xl font-bold ${data.needAttention > 0 ? 'text-red-600' : ''}`}>{data.needAttention}</p><p className="text-xs theme-text-muted">not yet registered for a current-semester carry-over</p></div>
                    </div>

                    <div className="card p-4 mb-4 flex gap-3 flex-wrap items-center">
                        <select value={level} onChange={(e) => setLevel(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="ALL">All levels</option>
                            {levels.map((l) => <option key={l} value={l}>{l}</option>)}
                        </select>
                        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search name or matric number"
                               className="border theme-border rounded-lg px-3 py-2 text-sm w-64" />
                        <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={onlyAttention} onChange={(e) => setOnlyAttention(e.target.checked)} /> Needs attention only</label>
                        <button onClick={download} className="flex items-center gap-1.5 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press ml-auto">
                            <Download size={14} /> Download CSV
                        </button>
                    </div>

                    {shown.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">
                            {data.withCarryOvers === 0
                                ? 'No student has a carry-over yet. Carry-overs appear as soon as failed results are in the system.'
                                : 'No student matches this filter.'}
                        </div>
                    ) : (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{shown.length} student{shown.length === 1 ? '' : 's'}</div>
                            <ul className="divide-y theme-border">
                                {shown.map((s) => (
                                    <li key={s.studentId}>
                                        <button onClick={() => toggle(s.studentId)} className="w-full px-4 py-3 flex items-center gap-3 text-left text-sm flex-wrap">
                                            <span className="w-36 shrink-0">{s.matricNumber}</span>
                                            <span className="flex-1 min-w-[160px] font-medium">{s.fullName} <span className="font-normal theme-text-muted">· {s.level}</span></span>
                                            <span className="theme-text-muted">{s.outstanding} course(s) · {s.units} unit(s)</span>
                                            {s.notRegisteredNow > 0
                                                ? <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">{s.notRegisteredNow} not registered</span>
                                                : <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-medium">Up to date</span>}
                                        </button>
                                        {open.has(s.studentId) && (
                                            <div className="px-4 pb-3 flex flex-wrap gap-2">
                                                {s.courses.map((c) => (
                                                    <span key={c.code} title={c.title} className={`text-xs px-2.5 py-1 rounded-full font-medium ${STATUS[c.status][1]}`}>
                            {c.code} · {c.semester === 'FIRST' ? '1st' : '2nd'} sem · {STATUS[c.status][0]}
                          </span>
                                                ))}
                                            </div>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </>
            )}
        </div>
    )
}

export default CarryOverMonitor