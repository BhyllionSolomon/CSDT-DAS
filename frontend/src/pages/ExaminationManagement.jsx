import { useState } from 'react'
import { configureExamTasks, getExamBoard, appointExamCommittee, uploadExamPaper, getExamPapers, reviewExamPaper, downloadExamPaper, saveQuestionPolicy, validateExamPaper, grantPaperAccess, rotateExamMasterKey, getExamSubmissionReport } from '../services/examService'

const blankTask = () => ({ courseId: '', examDate: '', deadline: '', vettingDate: '', correctionDeadline: '', markingDeadline: '', noticeDays: 49 })

export default function ExaminationManagement() {
    const [sessionId, setSessionId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [cycleId, setCycleId] = useState('')
    const [tasks, setTasks] = useState([blankTask()])
    const [board, setBoard] = useState(null)
    const [committeeUserId, setCommitteeUserId] = useState('')
    const [paperRows, setPaperRows] = useState({})
    const [notes, setNotes] = useState({})
    const [message, setMessage] = useState('')
    const [busy, setBusy] = useState(false)
    const [accessUsers, setAccessUsers] = useState({})
    const [accessExpires, setAccessExpires] = useState({})
    const [policies, setPolicies] = useState({})
    const [validationResults, setValidationResults] = useState({})
    const [submissionReport, setSubmissionReport] = useState([])

    const changeTask = (index, key, value) => setTasks(old => old.map((row, i) => i === index ? { ...row, [key]: value } : row))
    const loadBoard = async (id = cycleId) => {
        if (!id) return
        setBusy(true); setMessage('')
        try { const data = await getExamBoard(id); setBoard(data); setCycleId(String(id)) }
        catch (e) { setMessage(e.response?.data?.message || 'Unable to load examination cycle.') }
        finally { setBusy(false) }
    }
    const saveSchedule = async () => {
        setBusy(true); setMessage('')
        try {
            const payload = tasks.map(t => ({
                courseId: Number(t.courseId), examDate: t.examDate,
                deadline: t.deadline, vettingDate: t.vettingDate,
                correctionDeadline: t.correctionDeadline, markingDeadline: t.markingDeadline || null, noticeDays: Number(t.noticeDays || 49),
            }))
            const data = await configureExamTasks(Number(sessionId), semester, payload)
            setBoard(data); setCycleId(String(data.cycleId)); setMessage('Examination schedule saved.')
        } catch (e) { setMessage(e.response?.data?.message || e.response?.data?.error || 'Could not save schedule.') }
        finally { setBusy(false) }
    }
    const appoint = async () => {
        try { await appointExamCommittee(Number(cycleId), Number(committeeUserId)); setMessage('Committee appointment saved.'); setCommitteeUserId('') }
        catch (e) { setMessage(e.response?.data?.message || 'Could not appoint committee member.') }
    }
    const loadPapers = async taskId => {
        try { const rows = await getExamPapers(taskId); setPaperRows(old => ({ ...old, [taskId]: rows })) }
        catch (e) { setMessage(e.response?.data?.message || 'Could not load submissions.') }
    }
    const submitPaper = async (taskId, file) => {
        if (!file) return
        try { await uploadExamPaper(taskId, file); setMessage('Question paper uploaded and encrypted.'); await loadPapers(taskId) }
        catch (e) { setMessage(e.response?.data?.message || 'Upload failed.') }
    }
    const review = async paperId => {
        try { await reviewExamPaper(paperId, { decision: notes[paperId]?.decision || 'CORRECTION_REQUIRED', notes: notes[paperId]?.notes || '' }); setMessage('Review recorded.') }
        catch (e) { setMessage(e.response?.data?.message || 'Review could not be saved.') }
    }
    const rotateKeys = async () => { if (!window.confirm("Have you set APP_EXAM_PREVIOUS_MASTER_KEY to the old key and APP_EXAM_MASTER_KEY to the new key in Render, then restarted the backend?")) return; try { const r = await rotateExamMasterKey(); setMessage(r.message + " Rewrapped: " + r.rewrappedPapers) } catch(e) { setMessage(e.response?.data?.message || "Key rewrap failed. Keep the previous key configured.") } }
    const loadSubmissionReport = async () => { try { const r = await getExamSubmissionReport(); setSubmissionReport(r.submissions || []) } catch(e) { setMessage(e.response?.data?.message || "Could not load submission report.") } }
    const savePolicy = async taskId => { try { const p = policies[taskId] || {}; await saveQuestionPolicy(taskId, { questionCount: Number(p.questionCount), answerCount: Number(p.answerCount), totalMarks: Number(p.totalMarks), marksCsv: p.marksCsv || '', compulsoryCsv: p.compulsoryCsv || '', duration: p.duration || '' }); setMessage('Question policy saved.') } catch(e) { setMessage(e.response?.data?.message || 'Could not save policy.') } }
    const validate = async (taskId, paperId) => { try { const result = await validateExamPaper(taskId, paperId); setValidationResults(old => ({ ...old, [paperId]: result })); setMessage(result.passed ? 'Automated checks passed; committee review is still required.' : 'Validation found issues.') } catch(e) { setMessage(e.response?.data?.message || 'Validation failed.') } }
    const grant = async paperId => { try { await grantPaperAccess(paperId, Number(accessUsers[paperId]), accessExpires[paperId] || null); setMessage('Paper access authorised.') } catch(e) { setMessage(e.response?.data?.message || 'Could not grant access.') } }
    const download = async paper => {
        try { const blob = await downloadExamPaper(paper.paperId); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `confidential-paper-${paper.paperId}`; a.click(); URL.revokeObjectURL(url) }
        catch (e) { setMessage(e.response?.data?.message || 'Download denied or failed.') }
    }

    return <div className="max-w-6xl mx-auto p-4 space-y-6">
        <header><h1 className="text-2xl font-bold">Examination Management</h1><p className="text-sm opacity-70">Confidential question-paper submission, vetting and examination deadlines.</p></header>
        {message && <div className="rounded-lg border p-3 text-sm" role="status">{message}</div>}
        <section className="rounded-xl border p-4 space-y-3">
            <h2 className="font-semibold">HOD: Configure examination schedule</h2>
            <div className="grid md:grid-cols-2 gap-3">
                <label className="text-sm">Academic session ID<input className="mt-1 w-full rounded border p-2" type="number" value={sessionId} onChange={e => setSessionId(e.target.value)} /></label>
                <label className="text-sm">Semester<select className="mt-1 w-full rounded border p-2" value={semester} onChange={e => setSemester(e.target.value)}><option value="FIRST">First</option><option value="SECOND">Second</option></select></label>
            </div>
            {tasks.map((t, i) => <div key={i} className="grid md:grid-cols-3 gap-2 border-t pt-3">
                <label className="text-sm">Course ID<input className="mt-1 w-full rounded border p-2" type="number" value={t.courseId} onChange={e => changeTask(i, 'courseId', e.target.value)} /></label>
                <label className="text-sm">Exam date<input className="mt-1 w-full rounded border p-2" type="date" value={t.examDate} onChange={e => changeTask(i, 'examDate', e.target.value)} /></label>
                <label className="text-sm">Submission deadline<input className="mt-1 w-full rounded border p-2" type="datetime-local" value={t.deadline} onChange={e => changeTask(i, 'deadline', e.target.value)} /></label>
                <label className="text-sm">Committee vetting date<input className="mt-1 w-full rounded border p-2" type="datetime-local" value={t.vettingDate} onChange={e => changeTask(i, 'vettingDate', e.target.value)} /></label>
                <label className="text-sm">Correction deadline<input className="mt-1 w-full rounded border p-2" type="datetime-local" value={t.correctionDeadline} onChange={e => changeTask(i, 'correctionDeadline', e.target.value)} /></label>
                <label className="text-sm">Marked-script return deadline<input className="mt-1 w-full rounded border p-2" type="datetime-local" value={t.markingDeadline} onChange={e => changeTask(i, 'markingDeadline', e.target.value)} /></label>
                <label className="text-sm">Notice days<input className="mt-1 w-full rounded border p-2" type="number" min="1" max="180" value={t.noticeDays} onChange={e => changeTask(i, 'noticeDays', e.target.value)} /></label>
            </div>)}
            <div className="flex flex-wrap gap-2"><button className="rounded-lg border px-4 py-2" onClick={() => setTasks(old => [...old, blankTask()])}>Add course</button><button disabled={busy || !sessionId} className="rounded-lg bg-blue-700 text-white px-4 py-2 disabled:opacity-50" onClick={saveSchedule}>{busy ? 'Saving…' : 'Save schedule'}</button></div>
            <p className="text-xs opacity-60">Enter course IDs from the department course catalogue. The default lecturer task opening is 49 days before the exam date.</p>
            <div className="flex flex-wrap gap-2"><button className="rounded border px-3 py-2 text-sm" onClick={loadSubmissionReport}>HOD: Submission and overdue report</button><button className="rounded border px-3 py-2 text-sm" onClick={rotateKeys}>HOD: Rewrap all paper keys</button></div>
            {submissionReport.length > 0 && <div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr><th className="p-2 text-left">Course</th><th className="p-2 text-left">Lecturer</th><th className="p-2 text-left">Status</th><th className="p-2 text-left">Overdue</th></tr></thead><tbody>{submissionReport.map((r,i)=><tr key={i} className="border-t"><td className="p-2">{r.course}</td><td className="p-2">{r.lecturer}</td><td className="p-2">{r.status}</td><td className="p-2">{r.overdue ? "Yes" : "No"}</td></tr>)}</tbody></table></div>}
        </section>
        <section className="rounded-xl border p-4 space-y-3">
            <h2 className="font-semibold">Open an examination cycle</h2>
            <div className="flex flex-wrap gap-2"><input className="rounded border p-2" type="number" placeholder="Cycle ID" value={cycleId} onChange={e => setCycleId(e.target.value)} /><button className="rounded-lg border px-4 py-2" disabled={busy || !cycleId} onClick={() => loadBoard()}>Load cycle</button></div>
        </section>
        {board && <section className="space-y-4">
            <div className="rounded-xl border p-4"><h2 className="font-semibold">{board.session} · {board.semester} Semester</h2>
                <div className="mt-3 flex flex-wrap gap-2"><input className="rounded border p-2" type="number" placeholder="Staff user ID" value={committeeUserId} onChange={e => setCommitteeUserId(e.target.value)} /><button className="rounded-lg border px-4 py-2" disabled={!committeeUserId} onClick={appoint}>Appoint committee member</button></div>
            </div>
            {board.tasks.map(task => <article key={task.taskId} className="rounded-xl border p-4 space-y-3">
                <div><h3 className="font-semibold">{task.courseCode} — {task.courseTitle}</h3><p className="text-sm opacity-70">Exam: {task.examDate} · Submission deadline: {new Date(task.submissionDeadline).toLocaleString()}</p><p className="text-xs opacity-70">Task opens: {task.taskOpensOn}{task.vettingDate ? ` · Vetting: ${new Date(task.vettingDate).toLocaleString()}` : ''}</p></div>
                {task.canSubmit && <label className="block text-sm">Upload question paper (PDF/DOCX, max 15 MB)<input className="block mt-1" type="file" accept=".pdf,.docx" onChange={e => submitPaper(task.taskId, e.target.files?.[0])} /></label>}
                <details className="rounded border p-3"><summary className="cursor-pointer text-sm font-medium">HOD: Set question and marks policy</summary><div className="mt-3 grid md:grid-cols-3 gap-2">
                    {['questionCount','answerCount','totalMarks'].map((k) => <label key={k} className="text-xs">{k}<input className="mt-1 w-full rounded border p-2" type="number" value={policies[task.taskId]?.[k] || ''} onChange={e => setPolicies(old => ({...old,[task.taskId]:{...old[task.taskId],[k]:e.target.value}}))}/></label>)}
                    <label className="text-xs md:col-span-2">Marks per question (comma-separated)<input className="mt-1 w-full rounded border p-2" value={policies[task.taskId]?.marksCsv || ''} onChange={e => setPolicies(old => ({...old,[task.taskId]:{...old[task.taskId],marksCsv:e.target.value}}))}/></label>
                    <label className="text-xs">Compulsory question numbers<input className="mt-1 w-full rounded border p-2" value={policies[task.taskId]?.compulsoryCsv || ''} onChange={e => setPolicies(old => ({...old,[task.taskId]:{...old[task.taskId],compulsoryCsv:e.target.value}}))}/></label>
                    <label className="text-xs">Duration as printed<input className="mt-1 w-full rounded border p-2" value={policies[task.taskId]?.duration || ''} onChange={e => setPolicies(old => ({...old,[task.taskId]:{...old[task.taskId],duration:e.target.value}}))}/></label>
                    <button className="rounded bg-slate-800 px-3 py-2 text-white md:col-span-3" onClick={() => savePolicy(task.taskId)}>Save validation policy</button>
                </div></details>
                <button className="rounded-lg border px-3 py-1.5 text-sm" onClick={() => loadPapers(task.taskId)}>View submissions</button>
                {(paperRows[task.taskId] || []).map(p => <div key={p.paperId} className="border-t pt-3 space-y-2">
                    <p className="text-sm">{p.lecturer} · v{p.version} · {p.status} · {new Date(p.submittedAt).toLocaleString()}</p>
                    <button className="rounded border px-3 py-1 text-sm" onClick={() => download(p)}>Open/download securely</button>
                    <button className="rounded border px-3 py-1 text-sm" onClick={() => validate(task.taskId,p.paperId)}>Run document checks</button>
                    {validationResults[p.paperId] && <div className="rounded border p-2 text-xs"><strong>{validationResults[p.paperId].passed ? 'Automated checks passed' : 'Validation issues'}</strong>{validationResults[p.paperId].errors.map((x,i)=><p key={i}>{x}</p>)}{validationResults[p.paperId].warnings.map((x,i)=><p key={'w'+i}>Note: {x}</p>)}</div>}
                    <div className="flex flex-wrap gap-2"><input className="rounded border p-2 text-sm" type="datetime-local" value={accessExpires[p.paperId] || ""} onChange={e => setAccessExpires(old => ({...old,[p.paperId]:e.target.value}))}/><input className="rounded border p-2 text-sm" type="number" placeholder="Committee member user ID" value={accessUsers[p.paperId] || ''} onChange={e => setAccessUsers(old => ({...old,[p.paperId]:e.target.value}))}/><button className="rounded border px-3 py-1 text-sm" onClick={() => grant(p.paperId)}>HOD: Authorise this paper</button></div>

                    {p.reviewNotes && <p className="text-sm opacity-70">Feedback: {p.reviewNotes}</p>}
                    {task.vettingDate && <div className="grid md:grid-cols-3 gap-2">
                        <select className="rounded border p-2" value={notes[p.paperId]?.decision || 'CORRECTION_REQUIRED'} onChange={e => setNotes(old => ({ ...old, [p.paperId]: { ...old[p.paperId], decision: e.target.value } }))}><option value="CORRECTION_REQUIRED">Correction required</option><option value="ACCEPTED">Accepted</option></select>
                        <input className="rounded border p-2 md:col-span-2" placeholder="Committee feedback (required)" value={notes[p.paperId]?.notes || ''} onChange={e => setNotes(old => ({ ...old, [p.paperId]: { ...old[p.paperId], notes: e.target.value } }))} />
                        <button className="rounded-lg bg-slate-800 text-white px-3 py-2" onClick={() => review(p.paperId)}>Submit review</button>
                    </div>}
                </div>)}
            </article>)}
        </section>}
    </div>
}
