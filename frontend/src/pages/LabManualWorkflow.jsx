import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { listLabCourses } from '../services/labService'
import api from '../services/api'
import {
 uploadManualSubmission,getMyManualSubmissions,getManualApprovalQueue,getLabManualQueue,
 approveManualSubmission,returnManualSubmission,dispatchManualSubmission,receiveManualSubmission,getManualBuyers,
} from '../services/labManualWorkflowService'

const LABS = [
 ['COMPUTER_SCIENCE','Computer Science Laboratory'],
 ['SOFTWARE_ENGINEERING','Software Engineering Laboratory'],
 ['CYBERSECURITY','Cybersecurity Laboratory'],
]
const money = n => '₦' + Number(n || 0).toLocaleString('en-NG')
const errorText = e => e.response?.data?.message || e.response?.data?.detail || e.message || 'Request failed.'

export default function LabManualWorkflow() {
 const { user } = useAuth()
 const role = user?.role || ''
 const lecturer = ['LECTURER','ADJUNCT'].includes(role)
 const hod = ['HOD','ADMIN'].includes(role)
 const lab = ['LAB_OFFICER','TECHNICAL_STAFF'].includes(role)
 const [courses,setCourses] = useState([])
 const [mine,setMine] = useState([])
 const [queue,setQueue] = useState([])
 const [labQueue,setLabQueue] = useState([])
 const [buyers,setBuyers] = useState({})
 const [form,setForm] = useState({title:'',courseId:'',pageCount:'',suggestedLevy:'1500',file:null})
 const [levies,setLevies] = useState({})
 const [comments,setComments] = useState({})
 const [labs,setLabs] = useState({})
 const [busy,setBusy] = useState(false)
 const [notice,setNotice] = useState('')
 const [error,setError] = useState('')

 async function refresh() {
  const tasks = []
  if(lecturer) tasks.push(getMyManualSubmissions().then(setMine))
  if(hod) tasks.push(getManualApprovalQueue().then(setQueue))
  if(lab || hod) tasks.push(getLabManualQueue().then(setLabQueue))
  await Promise.all(tasks)
 }
 useEffect(()=>{ listLabCourses().then(setCourses).catch(()=>{}); refresh().catch(e=>setError(errorText(e))) },[])
 async function act(fn,message) {
  setBusy(true);setError('');setNotice('')
  try { await fn(); await refresh(); setNotice(message || 'Updated successfully.') }
  catch(e){setError(errorText(e))}
  finally{setBusy(false)}
 }
 async function upload(e) {
  e.preventDefault()
  if(!form.file){setError('Choose the PDF manual to upload.');return}
  await act(async()=>{
   await uploadManualSubmission(form)
   setForm({title:'',courseId:'',pageCount:'',suggestedLevy:'1500',file:null})
   e.target.reset()
  },'Manual submitted to the HOD for approval.')
 }
 async function openFile(id) {
  try {
   const response=await api.get('/lab/manual-workflow/'+id+'/file',{responseType:'blob'})
   const url=URL.createObjectURL(response.data)
   window.open(url,'_blank','noopener,noreferrer')
   setTimeout(()=>URL.revokeObjectURL(url),60000)
  } catch(e){setError(errorText(e))}
 }
 async function showBuyers(id) {
  try { setBuyers(v=>({...v,[id]:v[id] ? null : []})); if(!buyers[id]) setBuyers(v=>({...v,[id]:[] , [id+'_loading']:true})); const data=await getManualBuyers(id);setBuyers(v=>({...v,[id]:data,[id+'_loading']:false})) }
  catch(e){setError(errorText(e))}
 }
 const status = s => ({SUBMITTED_TO_HOD:'Awaiting HOD approval',APPROVED:'Approved — awaiting lecturer dispatch',RETURNED:'Returned for correction',SENT_TO_LAB:'Sent to laboratory',RECEIVED:'Received by laboratory'})[s] || s

 return <div className="max-w-6xl space-y-6">
  <div><h2 className="text-2xl font-bold">Course Lab Manuals</h2><p className="theme-text-muted text-sm mt-1">Upload, approve, dispatch and receive course manuals for laboratory printing.</p></div>
  {error && <div role="alert" className="rounded-lg bg-red-50 text-red-700 p-3 text-sm">{error}</div>}
  {notice && <div role="status" className="rounded-lg bg-green-50 text-green-700 p-3 text-sm">{notice}</div>}

  {lecturer && <section className="card p-5 space-y-4">
   <h3 className="font-semibold text-lg">Upload a course manual</h3>
   <p className="text-sm theme-text-muted">Upload the final PDF. The HOD sets the official student levy; your suggested amount is only a recommendation.</p>
   <form onSubmit={upload} className="grid grid-cols-1 md:grid-cols-2 gap-3">
    <label className="text-sm">Manual title<input required maxLength="150" value={form.title} onChange={e=>setForm({...form,title:e.target.value})} className="block w-full mt-1 border theme-border rounded-lg p-2 bg-transparent"/></label>
    <label className="text-sm">Course<select required value={form.courseId} onChange={e=>setForm({...form,courseId:e.target.value})} className="block w-full mt-1 border theme-border rounded-lg p-2 bg-transparent"><option value="">Select course</option>{courses.map(c=><option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}</select></label>
    <label className="text-sm">Number of pages<input required type="number" min="1" max="2000" value={form.pageCount} onChange={e=>setForm({...form,pageCount:e.target.value})} className="block w-full mt-1 border theme-border rounded-lg p-2 bg-transparent"/></label>
    <label className="text-sm">Suggested levy (₦)<input type="number" min="1" step="50" value={form.suggestedLevy} onChange={e=>setForm({...form,suggestedLevy:e.target.value})} className="block w-full mt-1 border theme-border rounded-lg p-2 bg-transparent"/></label>
    <label className="text-sm md:col-span-2">PDF manual (maximum 15 MB)<input required type="file" accept="application/pdf,.pdf" onChange={e=>setForm({...form,file:e.target.files?.[0] || null})} className="block w-full mt-1 border theme-border rounded-lg p-2"/></label>
    <button disabled={busy} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-semibold w-fit disabled:opacity-50">{busy?'Submitting…':'Submit to HOD'}</button>
   </form>
  </section>}

  {hod && <section className="card overflow-hidden">
   <div className="theme-sidebar text-white p-3 font-semibold">HOD approval queue</div>
   {!queue.length ? <p className="p-5 text-sm theme-text-muted">No manuals are awaiting approval.</p> : <div className="divide-y theme-border">{queue.map(s=><div key={s.id} className="p-4 space-y-3">
    <div className="flex flex-wrap justify-between gap-2"><div><b>{s.title}</b><p className="text-sm theme-text-muted">{s.courseCode} · {s.courseTitle} · {s.pageCount} pages · Lecturer: {s.uploadedBy}</p><p className="text-xs theme-text-muted">Suggested levy: {money(s.suggestedLevy)}</p></div><button onClick={()=>openFile(s.id)} className="underline text-sm">View PDF</button></div>
    <div className="grid grid-cols-1 md:grid-cols-3 gap-2"><label className="text-sm">Official levy (₦)<input type="number" min="1" step="50" value={levies[s.id] ?? '1500'} onChange={e=>setLevies({...levies,[s.id]:e.target.value})} className="block w-full border theme-border rounded-lg p-2 bg-transparent"/></label><label className="text-sm md:col-span-2">HOD comment<input value={comments[s.id]||''} onChange={e=>setComments({...comments,[s.id]:e.target.value})} className="block w-full border theme-border rounded-lg p-2 bg-transparent"/></label></div>
    <div className="flex gap-3"><button disabled={busy} onClick={()=>act(()=>approveManualSubmission(s.id,Number(levies[s.id]||1500),comments[s.id]),'Manual approved and official levy recorded.')} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm">Approve & set levy</button><button disabled={busy} onClick={()=>act(()=>returnManualSubmission(s.id,comments[s.id]),'Manual returned to lecturer.')} className="border theme-border rounded-lg px-4 py-2 text-sm">Return to lecturer</button></div>
   </div>)}</div>}
  </section>}

  {lecturer && <section className="card overflow-hidden">
   <div className="theme-sidebar text-white p-3 font-semibold">My manual submissions</div>
   {!mine.length?<p className="p-5 text-sm theme-text-muted">You have not submitted a manual yet.</p>:<div className="divide-y theme-border">{mine.map(s=><div key={s.id} className="p-4 space-y-3">
    <div className="flex flex-wrap items-center justify-between gap-2"><div><b>{s.title}</b><p className="text-sm theme-text-muted">{s.courseCode} · {s.pageCount} pages · {status(s.status)}</p>{s.approvedLevy && <p className="text-sm">Approved levy: {money(s.approvedLevy)}</p>}{s.hodComment&&<p className="text-xs theme-text-muted">HOD: {s.hodComment}</p>}</div><button onClick={()=>openFile(s.id)} className="underline text-sm">View PDF</button></div>
    {s.status==='APPROVED'&&<div className="flex flex-wrap gap-2"><select value={labs[s.id]||''} onChange={e=>setLabs({...labs,[s.id]:e.target.value})} className="border theme-border rounded-lg p-2 bg-transparent text-sm"><option value="">Choose lab to receive</option>{LABS.map(([v,l])=><option key={v} value={v}>{l}</option>)}</select><button disabled={busy||!labs[s.id]} onClick={()=>act(()=>dispatchManualSubmission(s.id,labs[s.id]),'Approved manual sent to laboratory.')} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm">Send to lab staff</button></div>}
    {s.status==='RECEIVED'&&<div><p className="text-sm text-green-700">Received by {s.receivedBy}. Official levy: {money(s.approvedLevy)}.</p><button onClick={()=>showBuyers(s.id)} className="underline text-sm mt-2">View student purchase status</button>{buyers[s.id]&&<div className="mt-3 overflow-x-auto"><table className="w-full text-sm"><thead><tr><th className="text-left p-2">Matric number</th><th className="text-left p-2">Student</th><th className="text-left p-2">Amount</th><th className="text-left p-2">Status</th></tr></thead><tbody>{buyers[s.id].map(b=><tr key={b.orderId} className="border-t theme-border"><td className="p-2">{b.matricNumber}</td><td className="p-2">{b.studentName}</td><td className="p-2">{money(b.amount)}</td><td className="p-2">{b.status}</td></tr>)}</tbody></table></div>}</div>}
   </div>)}</div>}
  </section>}

  {lab && <section className="card overflow-hidden">
   <div className="theme-sidebar text-white p-3 font-semibold">Manuals sent to laboratory</div>
   {!labQueue.length?<p className="p-5 text-sm theme-text-muted">No manuals are awaiting laboratory receipt.</p>:<div className="divide-y theme-border">{labQueue.map(s=><div key={s.id} className="p-4 flex flex-wrap items-center justify-between gap-3"><div><b>{s.title}</b><p className="text-sm theme-text-muted">{s.courseCode} · {s.pageCount} pages · {s.targetLab?.replaceAll('_',' ')} · {status(s.status)}</p><p className="text-sm">Official student levy: {money(s.approvedLevy)}</p><button onClick={()=>openFile(s.id)} className="underline text-sm">Open print-ready PDF</button></div>{s.status==='SENT_TO_LAB'&&<button disabled={busy} onClick={()=>act(()=>receiveManualSubmission(s.id),'Manual receipt recorded and catalogue listing published.')} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm">Confirm receipt</button>}</div>)}</div>}
  </section>}

  {hod && <section className="card overflow-hidden"><div className="theme-sidebar text-white p-3 font-semibold">Laboratory hand-offs</div>{!labQueue.length?<p className="p-4 text-sm theme-text-muted">No manuals currently with laboratory staff.</p>:labQueue.map(s=><div key={s.id} className="p-3 border-b theme-border text-sm">{s.title} · {s.courseCode} · {s.targetLab?.replaceAll('_',' ')} · {status(s.status)}</div>)}</section>}
 </div>
}
