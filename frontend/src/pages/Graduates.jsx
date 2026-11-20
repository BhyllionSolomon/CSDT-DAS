import { useEffect, useMemo, useState } from 'react'
import { Download } from 'lucide-react'
import { getGraduates } from '../services/graduatesService'

const msg = (err) => err.response?.data?.message || err.message

const kindOf = (label) => {
    const t = String(label || '').toUpperCase()
    return t.includes('FIRST') ? 'FIRST' : t.includes('UPPER') ? 'UPPER' : t.includes('LOWER') ? 'LOWER' : t.includes('THIRD') ? 'THIRD' : 'OTHER'
}
const KINDS = [['FIRST', 'First Class', '#16a34a'], ['UPPER', 'Second Upper', '#84cc16'], ['LOWER', 'Second Lower', '#eab308'], ['THIRD', 'Third Class', '#f97316'], ['OTHER', 'Other', '#94a3b8']]

function Graduates() {
    const [rows, setRows] = useState(null)
    const [classOf, setClassOf] = useState('ALL')
    const [kind, setKind] = useState('ALL')
    const [query, setQuery] = useState('')
    const [error, setError] = useState(null)

    useEffect(() => {
        getGraduates().then(setRows).catch((e) => setError(msg(e)))
    }, [])

    const classes = useMemo(() => (rows ? [...new Set(rows.map((r) => r.classOf))] : []), [rows])

    const summary = useMemo(() => classes.map((c) => {
        const list = rows.filter((r) => r.classOf === c)
        const counts = Object.fromEntries(KINDS.map(([k]) => [k, list.filter((r) => kindOf(r.degreeClass) === k).length]))
        return { name: c, total: list.length, counts }
    }), [rows, classes])

    const shown = (rows || []).filter((r) =>
        (classOf === 'ALL' || r.classOf === classOf) &&
        (kind === 'ALL' || kindOf(r.degreeClass) === kind) &&
        (!query.trim() || `${r.matricNumber} ${r.fullName}`.toLowerCase().includes(query.trim().toLowerCase())))

    function download() {
        const head = ['Matric number', 'Name', 'Programme', 'Graduating class', 'CGPA', 'Degree class', 'Outstanding courses']
        const body = shown.map((r) => [r.matricNumber, r.fullName, r.programme, r.classOf, r.cgpa ?? '', r.degreeClass ?? '', r.outstanding])
        const csv = [head, ...body].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'graduates.csv'
        a.click()
    }

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Graduates</h2>
            <p className="theme-text-muted text-sm mb-6">
                Students who have finished 400 level. They move here when you run <b>Start New Session</b>, and their records and results stay available.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {!rows && !error && <p className="theme-text-muted">Loading…</p>}

            {rows && rows.length === 0 && (
                <div className="card p-6 text-sm theme-text-muted">
                    There are no graduates yet. 400-level students become graduates when you start the next academic session.
                </div>
            )}

            {rows && rows.length > 0 && (
                <>
                    <div className="grid md:grid-cols-2 gap-4 mb-6">
                        {summary.map((s) => (
                            <div key={s.name} className="card p-4">
                                <div className="flex justify-between items-baseline mb-2">
                                    <p className="font-semibold">Class of {s.name}</p>
                                    <p className="text-sm theme-text-muted">{s.total} graduate{s.total === 1 ? '' : 's'}</p>
                                </div>
                                <div className="flex h-3 rounded-full overflow-hidden mb-2" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                    {KINDS.map(([k, label, color]) => s.counts[k] > 0 && (
                                        <div key={k} title={`${label}: ${s.counts[k]}`} style={{ width: `${(s.counts[k] / s.total) * 100}%`, backgroundColor: color }} />
                                    ))}
                                </div>
                                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs theme-text-muted">
                                    {KINDS.filter(([k]) => s.counts[k] > 0).map(([k, label, color]) => (
                                        <span key={k} className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded inline-block" style={{ backgroundColor: color }} /> {label}: {s.counts[k]}</span>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="card p-4 mb-4 flex gap-3 flex-wrap items-end">
                        <select value={classOf} onChange={(e) => setClassOf(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="ALL">All graduating classes</option>
                            {classes.map((c) => <option key={c} value={c}>Class of {c}</option>)}
                        </select>
                        <select value={kind} onChange={(e) => setKind(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="ALL">All degree classes</option>
                            {KINDS.map(([k, label]) => <option key={k} value={k}>{label}</option>)}
                        </select>
                        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search name or matric number"
                               className="border theme-border rounded-lg px-3 py-2 text-sm w-64" />
                        <button onClick={download} className="flex items-center gap-1.5 border theme-border rounded-lg px-4 py-2 text-sm font-medium btn-press ml-auto">
                            <Download size={14} /> Download CSV
                        </button>
                    </div>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{shown.length} graduate{shown.length === 1 ? '' : 's'}</div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                                <tr>
                                    <th className="px-4 py-2">Matric</th><th className="px-4 py-2">Name</th><th className="px-4 py-2">Programme</th>
                                    <th className="px-4 py-2">Class of</th><th className="px-4 py-2">CGPA</th><th className="px-4 py-2">Degree class</th><th className="px-4 py-2">Outstanding</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y theme-border">
                                {shown.map((r) => (
                                    <tr key={r.studentId}>
                                        <td className="px-4 py-2">{r.matricNumber}</td>
                                        <td className="px-4 py-2 font-medium">{r.fullName}</td>
                                        <td className="px-4 py-2">{r.programme}</td>
                                        <td className="px-4 py-2">{r.classOf}</td>
                                        <td className="px-4 py-2 font-semibold">{r.cgpa ?? '—'}</td>
                                        <td className="px-4 py-2">{r.degreeClass || '—'}</td>
                                        <td className="px-4 py-2">
                                            {r.outstanding > 0
                                                ? <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 font-medium">{r.outstanding} course(s)</span>
                                                : <span className="theme-text-muted">None</span>}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}

export default Graduates