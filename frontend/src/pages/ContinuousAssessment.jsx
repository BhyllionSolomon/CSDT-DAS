import { useEffect, useRef, useState } from 'react'
import { Download, Eye, EyeOff, Trash2, FileDown, Users } from 'lucide-react'
import SessionSemesterBar from '../components/SessionSemesterBar'
import {
    getCaCourses, getCaSheet, createCaItem, updateCaItem, deleteCaItem, saveCaScores,
    getCaSubmissions, gradeCaSubmission, downloadCaSubmission, aggregateCa, CA_CATEGORIES, caLabel, uploadAssignmentBrief, downloadAssignmentBrief,
} from '../services/caService'

const msg = (err) => err.response?.data?.message || err.message
const num = (v) => (v === '' || v == null ? null : Number(v))
const round = (v) => Math.round(v * 100) / 100
const dateText = (v) => v ? new Date(v).toLocaleString() : 'No deadline'

function ContinuousAssessment() {
    const [sel, setSel] = useState(null)
    const [courses, setCourses] = useState([])
    const [courseId, setCourseId] = useState('')
    const [sheet, setSheet] = useState(null)
    const [edits, setEdits] = useState({})
    const [form, setForm] = useState({ category: 'ASSIGNMENT', title: '', maxMark: '', instructions: '', dueAt: '', publish: true, assignmentFile: null })
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)
    const [selectedItem, setSelectedItem] = useState(null)
    const [submissions, setSubmissions] = useState([])
    const [gradeEdits, setGradeEdits] = useState({})
    const lastSubmissionCount = useRef(0)

    useEffect(() => {
        if (!sel) return
        setCourses([]); setCourseId(''); setSheet(null); setError(null)
        getCaCourses(sel.sessionId, sel.semester).then(setCourses).catch((e) => setError(msg(e)))
    }, [sel])

    async function load() {
        if (!sel || !courseId) return
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
        try { await action(); await load(); if (success) setNotice(success) }
        catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    const cell = (itemId, student) => edits[itemId]?.[student.studentId] !== undefined
        ? edits[itemId][student.studentId] : (student.scores[itemId] ?? '')
    const setCell = (itemId, studentId, value) =>
        setEdits({ ...edits, [itemId]: { ...(edits[itemId] || {}), [studentId]: value } })
    const rawTotal = (student) => round(sheet.items.reduce((t, i) => t + (num(cell(i.id, student)) || 0), 0))
    const total = (student) => sheet.allocated > 0 ? round(rawTotal(student) * 30 / sheet.allocated) : 0
    const dirty = Object.keys(edits).length > 0

    function saveAll() {
        act(async () => {
            for (const [itemId, changes] of Object.entries(edits)) {
                const entries = Object.entries(changes).map(([studentId, value]) => ({ studentId: Number(studentId), score: num(value) }))
                if (entries.length) await saveCaScores(itemId, entries)
            }
        }, 'Scores saved and CA totals recalculated.')
    }

    function addItem(e) {
        e.preventDefault()
        act(async () => {
            const created = await createCaItem({
                courseId: Number(courseId), sessionId: Number(sel.sessionId), semester: sel.semester,
                category: form.category, title: form.title, maxMark: Number(form.maxMark),
                instructions: form.instructions, dueAt: form.dueAt || null,
            })
            if (form.assignmentFile) await uploadAssignmentBrief(created.id, form.assignmentFile)
            if (form.publish) await updateCaItem(created.id, { released: true })
            setForm({ ...form, title: '', maxMark: '', instructions: '', dueAt: '', assignmentFile: null })
        }, form.publish ? 'Assessment created and posted to students.' : 'Assessment saved as a draft.')
    }

    function removeItem(item) {
        if (!window.confirm(`Delete "${item.title}" and all its scores and submissions?`)) return
        act(() => deleteCaItem(item.id), 'Assessment deleted.')
    }

    async function openSubmissions(item) {
        setSelectedItem(item); setSubmissions([]); setError(null); lastSubmissionCount.current = 0
        try { const rows = await getCaSubmissions(item.id); setSubmissions(rows); lastSubmissionCount.current = rows.filter(x => x.id).length }
        catch (e) { setError(msg(e)) }
    }

    useEffect(() => {
        if (!selectedItem) return
        const timer = window.setInterval(async () => {
            try {
                const rows = await getCaSubmissions(selectedItem.id)
                const count = rows.filter(x => x.id).length
                if (count > lastSubmissionCount.current) setNotice(`New student submission received for ${selectedItem.title}.`)
                lastSubmissionCount.current = count
                setSubmissions(rows)
            } catch { /* The next refresh will retry. */ }
        }, 30000)
        return () => window.clearInterval(timer)
    }, [selectedItem])

    async function grade(submission) {
        const value = gradeEdits[submission.id] || {}
        setBusy(true); setError(null); setNotice(null)
        try {
            await gradeCaSubmission(submission.id, { score: Number(value.score), feedback: value.feedback || '' })
            setSubmissions(await getCaSubmissions(selectedItem.id))
            await load()
            setNotice(`Marked ${submission.fullName}.`)
        } catch (e) { setError(msg(e)) } finally { setBusy(false) }
    }

    async function downloadBrief(item) {
        try {
            const blob = await downloadAssignmentBrief(item.id)
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a'); a.href = url; a.download = item.assignmentFileName || 'assignment-brief'; a.click()
            URL.revokeObjectURL(url)
        } catch (e) { setError(msg(e)) }
    }

    async function download(submission) {
        try {
            const blob = await downloadCaSubmission(submission.id)
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a'); a.href = url; a.download = submission.fileName; a.click()
            URL.revokeObjectURL(url)
        } catch (e) { setError(msg(e)) }
    }

    async function finaliseCa() {
        if (!window.confirm('Aggregate all recorded marks for this course into final CA scores out of 30? Students with no recorded mark will receive zero for that activity.')) return
        setBusy(true); setError(null); setNotice(null)
        try {
            const rows = await aggregateCa({ courseId: Number(courseId), sessionId: Number(sel.sessionId), semester: sel.semester })
            setNotice(`CA aggregated for ${rows.length} students. Final scores are now available out of 30.`)
            await load()
        } catch (e) { setError(msg(e)) } finally { setBusy(false) }
    }

    function exportCsv() {
        const head = ['Matric number', 'Name', ...sheet.items.map((i) => `${i.title} (/${i.max})`), 'Raw total', 'CA total (/30)']
        const rows = sheet.students.map((s) => [s.matricNumber, s.fullName, ...sheet.items.map((i) => s.scores[i.id] ?? ''), rawTotal(s), total(s)])
        const csv = [head, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'continuous-assessment.csv'; a.click()
    }

    return (
        <div className="max-w-6xl">
            <h2 className="text-2xl font-bold mb-2">Continuous Assessment</h2>
            <p className="theme-text-muted text-sm mb-6">
                Create and post assignments, practicals, presentations and tests. Set deadlines, review submissions,
                mark students and calculate the final CA proportionally out of 30.
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

            {sel && courses.length === 0 && !error && <div className="card p-6 text-sm theme-text-muted">You have no allocated course for this session and semester.</div>}

            {sheet && <>
                <div className="card p-4 mb-5">
                    <div className="flex justify-between gap-2 flex-wrap mb-2 text-sm">
                        <b>{sheet.allocated} raw marks allocated across activities</b>
                        <span className="theme-text-muted">All activities are converted proportionally to a final CA score out of 30.</span>
                    </div>
                    <form onSubmit={addItem} className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
                        <label className="text-sm">Activity type
                            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="block w-full border theme-border rounded-lg px-3 py-2 mt-1">
                                {CA_CATEGORIES.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                            </select>
                        </label>
                        <label className="text-sm">Title
                            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={80} placeholder="e.g. Assignment 1" className="block w-full border theme-border rounded-lg px-3 py-2 mt-1" />
                        </label>
                        <label className="text-sm">Maximum raw marks
                            <input type="number" min="0.01" step="0.01" value={form.maxMark} onChange={(e) => setForm({ ...form, maxMark: e.target.value })} required placeholder="e.g. 10" className="block w-full border theme-border rounded-lg px-3 py-2 mt-1" />
                        </label>
                        <label className="text-sm">Submission deadline (date and time)
                            <input type="datetime-local" value={form.dueAt} onChange={(e) => setForm({ ...form, dueAt: e.target.value })} className="block w-full border theme-border rounded-lg px-3 py-2 mt-1" />
                        </label>
                        <label className="text-sm md:col-span-2">Attach assignment brief (PDF or Word, max 15 MB)<input type="file" accept=".pdf,.doc,.docx" onChange={(e) => setForm({ ...form, assignmentFile: e.target.files?.[0] || null })} className="block w-full mt-1" /></label>
                        <label className="text-sm md:col-span-2">Instructions / assignment details
                            <textarea rows="3" maxLength="3000" value={form.instructions} onChange={(e) => setForm({ ...form, instructions: e.target.value })} placeholder="Describe the task, format, practical requirements and submission instructions." className="block w-full border theme-border rounded-lg px-3 py-2 mt-1" />
                        </label>
                        <label className="md:col-span-2 flex items-center gap-2 text-sm"><input type="checkbox" checked={form.publish} onChange={(e) => setForm({ ...form, publish: e.target.checked })} /> Post to students immediately</label>
                        <div className="md:col-span-2"><button disabled={busy} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium disabled:opacity-50">{busy ? 'Saving…' : form.publish ? 'Create and post assessment' : 'Save as draft'}</button></div>
                    </form>
                </div>

                <div className="card overflow-hidden mb-5">
                    <div className="theme-sidebar text-white px-4 py-3 flex justify-between items-center flex-wrap gap-2 text-sm">
                        <b>{sheet.students.length} enrolled students</b>
                        <div className="flex gap-3 items-center"><button onClick={exportCsv} className="flex items-center gap-1 underline"><Download size={14}/> Export CA scores</button><button onClick={finaliseCa} disabled={busy || sheet.items.length === 0} className="bg-white text-slate-900 rounded-lg px-3 py-1.5 text-xs font-semibold disabled:opacity-50">Aggregate final CA /30</button></div>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }}><tr>
                                <th className="px-3 py-2">Student</th>
                                {sheet.items.map((i) => <th key={i.id} className="px-2 py-2 min-w-[135px] align-top">
                                    <b className="block">{i.title}</b><span className="block text-xs theme-text-muted">{caLabel(i.category)} · /{i.max}</span>
                                    {i.dueAt && <span className="block text-[11px] theme-text-muted">Due {dateText(i.dueAt)}</span>}{i.assignmentFileName && <span className="block text-[11px] theme-text-muted">Brief: {i.assignmentFileName}</span>}
                                    <div className="flex gap-2 mt-1 flex-wrap">
                                        <button onClick={() => act(() => updateCaItem(i.id, { released: !i.released }), i.released ? 'Hidden from students.' : 'Posted to students.')} disabled={busy} className="text-xs underline">{i.released ? <><Eye size={12} className="inline"/> Posted</> : <><EyeOff size={12} className="inline"/> Draft</>}</button>
                                        <button onClick={() => openSubmissions(i)} className="text-xs underline"><Users size={12} className="inline"/> Submissions ({i.submissionCount})</button>
                                        <button onClick={() => removeItem(i)} className="text-red-500"><Trash2 size={12}/></button>
                                    </div>
                                </th>)}
                                <th className="px-3 py-2">Raw total</th><th className="px-3 py-2">CA /30</th>
                            </tr></thead>
                            <tbody className="divide-y theme-border">
                                {sheet.students.map((s) => <tr key={s.studentId}>
                                    <td className="px-3 py-2"><b>{s.fullName}</b><span className="block text-xs theme-text-muted">{s.matricNumber}</span></td>
                                    {sheet.items.map((i) => <td key={i.id} className="px-2 py-2"><input type="number" min="0" max={i.max} step="0.01" value={cell(i.id,s)} onChange={(e) => setCell(i.id,s.studentId,e.target.value)} className="w-20 border theme-border rounded px-2 py-1"/></td>)}
                                    <td className="px-3 py-2">{rawTotal(s)}</td><td className="px-3 py-2 font-bold">{total(s)} / 30</td>
                                </tr>)}
                            </tbody>
                        </table>
                    </div>
                    <div className="p-4 flex gap-3 items-center">
                        <button onClick={saveAll} disabled={busy || !dirty} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm disabled:opacity-50">{busy ? 'Saving…' : 'Save scores'}</button>
                        {dirty && <span className="text-xs text-amber-600">Unsaved score changes</span>}
                    </div>
                </div>
            </>}

            {selectedItem && <div className="card p-4">
                <div className="flex justify-between items-center gap-2 mb-3">
                    <div><h3 className="font-bold text-lg">Submissions: {selectedItem.title}</h3><p className="text-xs theme-text-muted">Maximum score: {selectedItem.max}</p></div>
                    <button onClick={() => setSelectedItem(null)} className="underline text-sm">Close</button>
                </div>
                {submissions.length === 0 ? <p className="text-sm theme-text-muted">No students have submitted this assessment yet.</p> :
                    <div className="overflow-x-auto"><table className="w-full text-sm text-left"><thead><tr><th className="p-2">Student</th><th className="p-2">Submitted</th><th className="p-2">File</th><th className="p-2">Score</th><th className="p-2">Feedback</th><th className="p-2">Action</th></tr></thead>
                    <tbody className="divide-y theme-border">{submissions.map(s => <tr key={s.studentId}>
                        <td className="p-2"><b>{s.fullName}</b><span className="block text-xs theme-text-muted">{s.matricNumber}</span></td>
                        <td className="p-2">{dateText(s.submittedAt)}</td>
                        <td className="p-2">{s.id ? <button onClick={() => download(s)} className="underline"><FileDown size={13} className="inline"/> {s.fileName}</button> : <span className="text-amber-700">Not submitted</span>}</td>
                        <td className="p-2"><input type="number" min="0" max={selectedItem.max} step="0.01" value={gradeEdits[s.id]?.score ?? s.score ?? ''} onChange={e => setGradeEdits({...gradeEdits,[s.id]:{...gradeEdits[s.id],score:e.target.value}})} className="w-20 border theme-border rounded px-2 py-1"/></td>
                        <td className="p-2"><input value={gradeEdits[s.id]?.feedback ?? s.feedback ?? ''} onChange={e => setGradeEdits({...gradeEdits,[s.id]:{...gradeEdits[s.id],feedback:e.target.value}})} className="w-40 border theme-border rounded px-2 py-1" placeholder="Feedback"/></td>
                        <td className="p-2"><button disabled={busy || !s.id || (gradeEdits[s.id]?.score ?? s.score) == null} onClick={() => grade(s)} className="accent-gradient text-white rounded px-3 py-1 text-xs">Save mark</button></td>
                    </tr>)}</tbody></table></div>}
            </div>}
        </div>
    )
}

export default ContinuousAssessment
