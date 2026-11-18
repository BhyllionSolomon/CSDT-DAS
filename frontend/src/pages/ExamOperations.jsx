import { useEffect, useState } from 'react'
import {
 importOfficialTimetable, confirmImportedTimetableRow, getMyExamTimetable,
 assignExamDispatch, updateExamCustody, getExamDispatchBoard,
 collectExamScripts, returnMarkedScripts, getMyExamScripts,
 getExamMarkingReport, getExamDispatchReport, getExamNotifications, markExamNotificationRead,
 confirmExamSitting, publishExamSitting, notifyScriptsReady,
} from '../services/examService'

export default function ExamOperations() {
 const [cycleId,setCycleId]=useState('');const [taskId,setTaskId]=useState('');const [file,setFile]=useState(null)
 const [importResult,setImportResult]=useState(null);const [venueByRow,setVenueByRow]=useState({})
 const [dispatch,setDispatch]=useState([]);const [dispatchUser,setDispatchUser]=useState('');const [dispatchVenue,setDispatchVenue]=useState('')
 const [custody,setCustody]=useState({});const [scripts,setScripts]=useState([]);const [myTimetable,setMyTimetable]=useState([])
 const [myBatches,setMyBatches]=useState([]);const [lecturerId,setLecturerId]=useState('');const [quantity,setQuantity]=useState('')
 const [markingReport,setMarkingReport]=useState(null);const [dispatchReport,setDispatchReport]=useState(null);const [notifications,setNotifications]=useState([])
 const [sitting,setSitting]=useState({venue:'',startTime:'',endTime:''});const [message,setMessage]=useState('');const [busy,setBusy]=useState(false)
 const fail=e=>setMessage(e.response?.data?.message||e.response?.data?.error||'The request could not be completed.')
 const refresh=async()=>{try{setDispatch(await getExamDispatchBoard())}catch{}try{setMyBatches(await getMyExamScripts())}catch{}try{setNotifications(await getExamNotifications())}catch{}try{setMyTimetable(await getMyExamTimetable())}catch{}}
 useEffect(()=>{refresh()},[])
 const importTimetable=async()=>{if(!file||!cycleId)return;setBusy(true);try{setImportResult(await importOfficialTimetable(cycleId,file));setMessage('Timetable extracted. Confirm each matched row; nothing is published yet.')}catch(e){fail(e)}finally{setBusy(false)}}
 const confirmRow=async row=>{try{await confirmImportedTimetableRow(row.rowId,venueByRow[row.rowId]||'');setMessage(`${row.courseCode} confirmed. Publish it separately when ready.`)}catch(e){fail(e)}}
 const assign=async()=>{try{await assignExamDispatch(Number(taskId),Number(dispatchUser),dispatchVenue);setMessage('Dispatch assignment saved.');await refresh()}catch(e){fail(e)}}
 const action=async(id,kind)=>{const extra=custody[id]||{};try{await updateExamCustody(id,{action:kind,scriptsCount:Number(extra.scriptsCount||0),returnedByName:extra.name||'',returnedByUnit:extra.unit||'',returnedByRole:extra.role||'',notes:extra.notes||''});setMessage('Custody event recorded.');await refresh()}catch(e){fail(e)}}
 const notifyReady=async()=>{try{const r=await notifyScriptsReady(Number(taskId));setMessage("Notified "+r.notifiedLecturers+" allocated lecturers.");await refresh()}catch(e){fail(e)}}
 const collect=async()=>{try{await collectExamScripts(Number(taskId),Number(lecturerId),Number(quantity), 'HOD recorded script collection');setMessage('Script collection recorded.');await refresh()}catch(e){fail(e)}}
 const returnBatch=async b=>{const q=window.prompt(`How many marked scripts are being returned? Maximum ${b.quantityCollected}`);if(q===null)return;try{await returnMarkedScripts(b.id,Number(q),'Lecturer recorded return');setMessage('Marked-script return recorded.');await refresh()}catch(e){fail(e)}}
 const loadReports=async()=>{try{setMarkingReport(await getExamMarkingReport());setDispatchReport(await getExamDispatchReport())}catch(e){fail(e)}}
 const loadMyTimetable=async()=>{try{setMyTimetable(await getMyExamTimetable())}catch(e){fail(e)}}
 const publish=async()=>{try{await confirmExamSitting(Number(taskId),sitting);await publishExamSitting(Number(taskId));setMessage('Confirmed sitting published.');await refresh()}catch(e){fail(e)}}
 return <main className="mx-auto max-w-6xl space-y-5 p-4">
  <header><h1 className="text-2xl font-bold">Examination Operations</h1><p className="text-sm opacity-70">Official timetable review, exam-day custody, script marking and personal exam notices.</p></header>
  {message&&<div role="status" className="rounded border p-3 text-sm">{message}</div>}
  <section className="space-y-3 rounded-xl border p-4">
   <h2 className="font-semibold">Import official school examination timetable</h2>
   <p className="text-sm opacity-70">Upload PDF or DOCX. The system matches CSDT course codes and extracts possible dates/times. Every row requires HOD confirmation before publication.</p>
   <div className="flex flex-wrap gap-2"><input className="rounded border p-2" type="number" placeholder="Exam cycle ID" value={cycleId} onChange={e=>setCycleId(e.target.value)}/><input type="file" accept=".pdf,.docx" onChange={e=>setFile(e.target.files?.[0]||null)}/><button disabled={busy||!cycleId||!file} className="rounded bg-slate-800 px-4 py-2 text-white disabled:opacity-40" onClick={importTimetable}>{busy?'Extracting…':'Extract timetable'}</button></div>
   {importResult&&<div className="space-y-3"><p className="text-sm">{importResult.message}</p>
    {(importResult.matchedRows||[]).map(row=><div key={row.rowId} className="rounded border p-3 text-sm"><strong>{row.courseCode} — {row.courseTitle}</strong><p>{row.sourceLine}</p><p>Detected: {row.date||'Date not detected'} {row.start||''} {row.end||''}</p><div className="mt-2 flex gap-2"><input className="rounded border p-2" placeholder="Venue (optional)" value={venueByRow[row.rowId]||''} onChange={e=>setVenueByRow(old=>({...old,[row.rowId]:e.target.value}))}/><button className="rounded border px-3 py-2" disabled={!row.date} onClick={()=>confirmRow(row)}>Confirm row</button></div></div>)}
    {(importResult.unmatchedRows||[]).length>0&&<div><h3 className="font-medium">Unmatched timetable lines — manual review needed</h3>{importResult.unmatchedRows.map((r,i)=><p key={i} className="text-sm">{r.line}</p>)}</div>}
   </div>}
  </section>
  <section className="space-y-3 rounded-xl border p-4">
   <h2 className="font-semibold">Confirm and publish a sitting</h2>
   <p className="text-sm opacity-70">Use after confirming the official timetable row. Publishing makes the sitting visible to registered students.</p>
   <div className="grid gap-2 md:grid-cols-4"><input className="rounded border p-2" type="number" placeholder="Course task ID" value={taskId} onChange={e=>setTaskId(e.target.value)}/><input className="rounded border p-2" placeholder="Venue" value={sitting.venue} onChange={e=>setSitting({...sitting,venue:e.target.value})}/><input className="rounded border p-2" type="time" value={sitting.startTime} onChange={e=>setSitting({...sitting,startTime:e.target.value})}/><input className="rounded border p-2" type="time" value={sitting.endTime} onChange={e=>setSitting({...sitting,endTime:e.target.value})}/></div>
   <button className="rounded bg-slate-800 px-4 py-2 text-white" onClick={publish}>Confirm and publish</button>
  </section>
  <section className="space-y-3 rounded-xl border p-4">
   <h2 className="font-semibold">Assign examination dispatch</h2>
   <div className="grid gap-2 md:grid-cols-3"><input className="rounded border p-2" type="number" placeholder="Course task ID" value={taskId} onChange={e=>setTaskId(e.target.value)}/><input className="rounded border p-2" type="number" placeholder="Dispatch staff user ID" value={dispatchUser} onChange={e=>setDispatchUser(e.target.value)}/><input className="rounded border p-2" placeholder="Exam venue" value={dispatchVenue} onChange={e=>setDispatchVenue(e.target.value)}/></div>
   <button className="rounded bg-slate-800 px-4 py-2 text-white" onClick={assign}>Assign dispatch staff</button>
   <button className="ml-2 rounded border px-4 py-2" onClick={refresh}>Refresh dispatch register</button>
   {dispatch.map(d=><div key={d.id} className="space-y-2 border-t pt-3 text-sm"><strong>{d.course} · {d.examDate} · {d.assignedTo}</strong><p>Collected: {d.collectedAt||'Not yet'} · Delivered: {d.deliveredAt||'Not yet'} · Scripts received: {d.scriptsReceivedAt||'Not yet'}</p>
    <div className="grid gap-2 md:grid-cols-4"><input className="rounded border p-2" placeholder="Returned by name" value={custody[d.id]?.name||''} onChange={e=>setCustody(old=>({...old,[d.id]:{...old[d.id],name:e.target.value}}))}/><input className="rounded border p-2" placeholder="Unit/department" value={custody[d.id]?.unit||''} onChange={e=>setCustody(old=>({...old,[d.id]:{...old[d.id],unit:e.target.value}}))}/><input className="rounded border p-2" placeholder="Role" value={custody[d.id]?.role||''} onChange={e=>setCustody(old=>({...old,[d.id]:{...old[d.id],role:e.target.value}}))}/><input className="rounded border p-2" type="number" placeholder="Scripts count" value={custody[d.id]?.scriptsCount||''} onChange={e=>setCustody(old=>({...old,[d.id]:{...old[d.id],scriptsCount:e.target.value}}))}/></div>
    <div className="flex flex-wrap gap-2"><button className="rounded border px-3 py-1" disabled={!!d.collectedAt} onClick={()=>action(d.id,'COLLECT')}>Record collection</button><button className="rounded border px-3 py-1" disabled={!d.collectedAt||!!d.deliveredAt} onClick={()=>action(d.id,'DELIVER')}>Record delivery</button><button className="rounded border px-3 py-1" disabled={!d.deliveredAt||!!d.scriptsReceivedAt} onClick={()=>action(d.id,'RECEIVE_SCRIPTS')}>Receive answer scripts</button></div>
   </div>)}
  </section>
  <section className="space-y-3 rounded-xl border p-4">
   <h2 className="font-semibold">Lecturer script collection and marking return</h2>
   <div className="grid gap-2 md:grid-cols-3"><input className="rounded border p-2" type="number" placeholder="Course task ID" value={taskId} onChange={e=>setTaskId(e.target.value)}/><input className="rounded border p-2" type="number" placeholder="Lecturer user ID" value={lecturerId} onChange={e=>setLecturerId(e.target.value)}/><input className="rounded border p-2" type="number" placeholder="Scripts collected" value={quantity} onChange={e=>setQuantity(e.target.value)}/></div>
   <div className="flex flex-wrap gap-2"><button className="rounded border px-4 py-2" onClick={notifyReady}>HOD: Notify lecturers to collect scripts</button><button className="rounded bg-slate-800 px-4 py-2 text-white" onClick={collect}>Record script collection</button></div>
   <h3 className="font-medium">My script batches</h3>{myBatches.map(b=><div key={b.id} className="flex flex-wrap items-center justify-between gap-2 border-t py-2 text-sm"><span>{b.course}: {b.quantityCollected} collected · {b.quantityReturned??0} returned · {b.outstanding} outstanding</span><button disabled={b.outstanding===0} className="rounded border px-3 py-1" onClick={()=>returnBatch(b)}>Return marked scripts</button></div>)}
  </section>
  <section className="space-y-3 rounded-xl border p-4">
   <div className="flex items-center justify-between"><h2 className="font-semibold">My examination timetable</h2><button className="rounded border px-3 py-1" onClick={loadMyTimetable}>Refresh</button></div>
   {myTimetable.length===0?<p className="text-sm opacity-70">No published examinations were found for your registered courses.</p>:myTimetable.map((x,i)=><div key={i} className="border-t py-2 text-sm"><strong>{x.courseCode} — {x.courseTitle}</strong><p>{x.examDate} · {x.startTime||'Time TBA'} · {x.venue||'Venue TBA'}</p></div>)}
  </section>
  <section className="space-y-3 rounded-xl border p-4">
   <div className="flex items-center justify-between"><h2 className="font-semibold">Management reports</h2><button className="rounded border px-3 py-1" onClick={loadReports}>Load reports</button></div>
   {markingReport?.courses?.map((r,i)=><p key={i} className="text-sm">{r.course}: {r.collected} collected · {r.returned} returned · {r.outstanding} outstanding</p>)}
   {dispatchReport?.dispatches?.length>0&&<p className="text-sm">{dispatchReport.dispatches.length} dispatch records in the register.</p>}
  </section>
  <section className="space-y-3 rounded-xl border p-4"><h2 className="font-semibold">My examination notifications</h2>
   {notifications.map(n=><div key={n.id} className="flex items-start justify-between gap-3 border-t py-2 text-sm"><div><strong>{n.title}</strong><p>{n.message}</p><span className="opacity-60">{new Date(n.createdAt).toLocaleString()}</span></div>{!n.read&&<button className="rounded border px-2 py-1" onClick={async()=>{try{await markExamNotificationRead(n.id);await refresh()}catch(e){fail(e)}}}>Mark read</button>}</div>)}
  </section>
 </main>
}
