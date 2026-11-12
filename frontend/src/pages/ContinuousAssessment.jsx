import { useEffect, useState } from 'react'
import { Download, Eye, EyeOff, Trash2 } from 'lucide-react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import {
    getCaCourses, getCaSheet, createCaItem, updateCaItem, deleteCaItem, saveCaScores, CA_CATEGORIES, caLabel,
} from '../services/caService'

const msg = (err) => err.response?.data?.message || err.message
const num = (v) => (v === '' || v == null ? null : Number(v))
const round = (v) => Math.round(v * 100) / 100

function ContinuousAssessment() {
    const [sel, setSel] = useState(null)
    const [courses, setCourses] = useState([])
    const [courseId, setCourseId] = useState('')
    const [sheet, setSheet] = useState(null)
    const [edits, setEdits] = useState({})
    const [form, setForm] = useState({ category: 'ASSIGNMENT', title: '', maxMark: '' })
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        if (!sel) return
        setCourses([]); setCourseId(''); setSheet(null); setError(null)
        getCaCourses(sel.sessionId, sel.semester).then(setCourses).catch((e) => setError(msg(e)))
    }, [sel])

    async function load() {
        setSheet(await getCaSheet(courseId, sel.sessionId, sel.semester))
        setEdits({})
    }

    useEffect(() => {
        if (!sel || !courseId) return
        setSheet(null); setError(null); setNotice(null)
        load().catch((e) => setError(msg(e)))
    }, [courseId, sel])

    async function act(action, success) {
        setError(null); setNotice(null); setBusy(true)
        try {
            await action()
            await load()
            if (success) setNotice(success)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const cell = (itemId, student) => {
        const edited = edits[itemId]?.[student.studentId]
        if (edited !== undefined) return edited
        const saved = student.scores[itemId]
        return saved === undefined ? '' : saved
    }

    const setCell = (itemId, studentId, value) =>
        setEdits({ ...edits, [itemId]: { ...(edits[itemId] || {}), [studentId]: value } })

    const total = (student) => round(sheet.items.reduce((t, i) => t + (num(cell(i.id, student)) || 0), 0))
    const dirty = Object.keys(edits).length > 0

    function saveAll() {
        act(async () => {
            for (const [itemId, changes] of Object.entries(edits)) {
                const entries = Object.entries(changes).map(([studentId, value]) => ({ studentId: Number(studentId), score: num(value) }))
                if (entries.length) await saveCaScores(itemId, entries)
            }
        }, 'Scores saved.')
    }

    function addItem(e) {
        e.preventDefault()
        act(async () => {
            await createCaItem({
                courseId: Number(courseId), sessionId: Number(sel.sessionId), semester: sel.semester,
                category: form.category, title: form.title, maxMark: Number(form.maxMark),
            })
            setForm({ ...form, title: '', maxMark: '' })
        }, 'Assessment added.')
    }

    function removeItem(item) {
        if (!window.confirm(`Delete "${item.title}" and all its scores?`)) return
        act(() => deleteCaItem(item.id), 'Assessment deleted.')
    }

    function download() {
        const head = ['Matric number', 'Name', ...sheet.items.map((i) => `${i.title} (/${i.max})`), 'C.A. total (/30)']
        const rows = sheet.students.map((s) => [s.matricNumber, s.fullName, ...sheet.items.map((i) => s.scores[i.id] ?? ''), s.total])
        const csv = [head, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'continuous-assessment.csv'
        a.click()
    }

    const left = sheet ? round(sheet.cap - sheet.allocated) : 0

    return (
        <div className="max-w-5xl">
            <h2 className="text-2xl font-bold mb-2">Continuous Assessment</h2>
            <p className="theme-text-muted text-sm mb-6">
                Record practicals, assignments, class presentations and tests. Together they make up the 30 marks of continuous assessment.
                Students see an assessment only after you release it.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <SessionSemesterBar onChange={setSel}>
                <div>
                    <label className="block text-sm font-medium mb-1">Course</label>
                    <select value={courseId} onChange={(e) => setCourseId(e.target.value)} className="border theme-border rounded-lg px-3 py-2 text-sm">
                        <option value="">Choose a course…</option>
                        {courses.map((c) => <option key={c.courseId} value={c.courseId}>{c.code} · {c.title} ({c.rosterSize})</option>)}
                    </select>
                </div>
            </SessionSemesterBar>

            {sel && courses.length === 0 && !error && (
                <div className="card p-6 text-sm theme-text-muted">You have no course on the allocation list for this session and semester.</div>
            )}

            {sheet && (
                <div className="space-y-6">
                    <div className="card p-4">
                        <div className="flex justify-between items-center mb-2 text-sm flex-wrap gap-2">
                            <span className="font-semibold">{sheet.allocated} of {sheet.cap} marks allocated</span>
                            <span className={left > 0 ? 'text-amber-600' : 'text-green-600'}>
                {left > 0 ? `${left} mark(s) still unallocated` : 'All 30 marks allocated'}
              </span>
                        </div>
                        <div className="h-2.5 rounded-full overflow-hidden" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            <div className="h-full accent-gradient" style={{ width: `${(sheet.allocated / sheet.cap) * 100}%` }} />
                        </div>

                        <form onSubmit={addItem} className="flex gap-2 flex-wrap items-end mt-4">
                            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                {CA_CATEGORIES.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                            </select>
                            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={80}
                                   placeholder="Title, e.g. Assignment 1" className="border theme-border rounded-lg px-3 py-2 text-sm w-56" />
                            <input type="number" min="0.5" max="30" step="0.5" value={form.maxMark} onChange={(e) => setForm({ ...form, maxMark: e.target.value })} required
                                   placeholder="Marks" className="border theme-border rounded-lg px-3 py-2 text-sm w-24" />
                            <button type="submit" disabled={busy || left <= 0}
                                    className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">Add assessment</button>
                        </form>
                    </div>

                    {sheet.items.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">Add the first assessment above, then enter the scores here.</div>
                    ) : sheet.students.length === 0 ? (
                        <div className="card p-6 text-sm theme-text-muted">No student has registered for this course this semester.</div>
                    ) : (
                        <div className="card overflow-hidden">
                            <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 flex-wrap text-sm">
                                <span className="font-semibold">{sheet.students.length} student(s)</span>
                                <button onClick={download} className="flex items-center gap-1 text-xs underline"><Download size={13} /> Download CSV</button>
                            </div>

                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="text-xs">
                                    <tr>
                                        <th className="px-3 py-2 uppercase theme-text-muted">Student</th>
                                        {sheet.items.map((i) => (
                                            <th key={i.id} className="px-2 py-2 min-w-[110px] align-top">
                                                <span className="block text-[10px] uppercase theme-text-muted">{caLabel(i.category)}</span>
                                                <span className="block font-semibold">{i.title}</span>
                                                <span className="block theme-text-muted font-normal">out of {i.max}</span>
                                                <span className="flex gap-2 mt-1">
                            <button title={i.released ? 'Hide from students' : 'Release to students'} disabled={busy}
                                    onClick={() => act(() => updateCaItem(i.id, { released: !i.released }), i.released ? 'Hidden from students.' : 'Released to students.')}
                                    className={`flex items-center gap-1 text-[11px] font-medium ${i.released ? 'text-green-600' : 'theme-text-muted'}`}>
                              {i.released ? <Eye size={13} /> : <EyeOff size={13} />} {i.released ? 'Released' : 'Hidden'}
                            </button>
                            <button title="Delete" onClick={() => removeItem(i)} disabled={busy} className="text-red-500"><Trash2 size={13} /></button>
                          </span>
                                            </th>
                                        ))}
                                        <th className="px-3 py-2 uppercase theme-text-muted">Total</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y theme-border">
                                    {sheet.students.map((s) => (
                                        <tr key={s.studentId}>
                                            <td className="px-3 py-1.5">
                                                <span className="block font-medium">{s.fullName}</span>
                                                <span className="block text-xs theme-text-muted">{s.matricNumber}</span>
                                            </td>
                                            {sheet.items.map((i) => (
                                                <td key={i.id} className="px-2 py-1.5">
                                                    <input type="number" min="0" max={i.max} step="0.5" value={cell(i.id, s)}
                                                           onChange={(e) => setCell(i.id, s.studentId, e.target.value)}
                                                           className="w-20 border theme-border rounded-lg px-2 py-1 text-sm" />
                                                </td>
                                            ))}
                                            <td className="px-3 py-1.5 font-semibold">{total(s)} <span className="font-normal theme-text-muted">/ 30</span></td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            <div className="p-4 flex items-center gap-3">
                                <button onClick={saveAll} disabled={busy || !dirty} className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                    {busy ? 'Saving…' : 'Save scores'}
                                </button>
                                {dirty && <span className="text-xs text-amber-600">You have unsaved changes.</span>}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

export default ContinuousAssessment