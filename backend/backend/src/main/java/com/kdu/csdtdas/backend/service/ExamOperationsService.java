package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;

@Service @Transactional
public class ExamOperationsService {
 private static final ZoneId LAGOS=ZoneId.of("Africa/Lagos");
 private final UserRepository users;private final ExamCourseTaskRepository tasks;private final ExamSittingRepository sittings;
 private final ExamDispatchRecordRepository dispatches;private final ExamScriptBatchRepository batches;
 private final CourseRegistrationRepository registrations;private final ExamAuditEventRepository audit;
 public ExamOperationsService(UserRepository u,ExamCourseTaskRepository t,ExamSittingRepository s,ExamDispatchRecordRepository d,ExamScriptBatchRepository b,CourseRegistrationRepository r,ExamAuditEventRepository a,CourseAllocationRepository alloc){users=u;tasks=t;sittings=s;dispatches=d;batches=b;registrations=r;audit=a;allocationRepository=alloc;}

 public Map<String,Object> confirmSitting(String actor,Long taskId,String venue,LocalTime start,LocalTime end){
  User hod=hod(actor);ExamCourseTask task=task(taskId);if(start==null||end==null||!end.isAfter(start))throw bad("Valid examination start and end times are required.");
  ExamSitting sitting=sittings.findByTaskId(taskId).orElseGet(ExamSitting::new);sitting.setTask(task);sitting.setVenue(venue);sitting.setStartTime(start);sitting.setEndTime(end);sitting.setConfirmed(true);sitting.setPublished(false);sitting.setConfirmedBy(hod);sitting.setConfirmedAt(LocalDateTime.now(LAGOS));sittings.save(sitting);log(hod,"SITTING_CONFIRMED", "EXAM_TASK",taskId,venue);return sittingMap(sitting);
 }
 public Map<String,Object> publishSitting(String actor,Long taskId){User hod=hod(actor);ExamSitting s=sittings.findByTaskId(taskId).orElseThrow(()->bad("Sitting not found."));
  if(!s.isConfirmed())throw bad("Confirm timetable entry before publication.");s.setPublished(true);log(hod,"SITTING_PUBLISHED","EXAM_TASK",taskId,"Published confirmed exam sitting");return sittingMap(s);}
 public List<Map<String,Object>> studentTimetable(String username){User u=user(username);if(u.getStudent()==null)throw bad("Student account is not linked to a student record.");
  Student st=u.getStudent();List<CourseRegistration> regs=registrations.findByStudentIdAndAcademicSessionId(st.getId(),st.getAcademicSession().getId());
  Set<Long> courseIds=new HashSet<>();for(CourseRegistration cr:regs)if("REGISTERED".equalsIgnoreCase(cr.getStatus()))courseIds.add(cr.getCourse().getId());
  List<Map<String,Object>> out=new ArrayList<>();for(ExamSitting s:sittings.findByPublishedTrue())if(courseIds.contains(s.getTask().getCourse().getId())&&s.getTask().getCycle().getAcademicSession().getId().equals(st.getAcademicSession().getId())){
   Map<String,Object> m=sittingMap(s);m.put("semester",s.getTask().getCycle().getSemester());out.add(m);}return out;
 }
 public Map<String,Object> assignDispatch(String actor,Long taskId,Long dispatchUserId,String venue){User hod=hod(actor);ExamCourseTask t=task(taskId);User assigned=userById(dispatchUserId);
  if(!Set.of("LECTURER","ADJUNCT","LEVEL_ADVISER","HOD","ADMIN").contains(assigned.getRole().toUpperCase()))throw bad("Dispatch assignee must be a staff account.");
  ExamDispatchRecord d=new ExamDispatchRecord();d.setTask(t);d.setDispatchUser(assigned);d.setAssignedBy(hod);d.setVenue(venue);dispatches.save(d);log(hod,"DISPATCH_ASSIGNED","EXAM_TASK",taskId,"Assigned "+assigned.getUsername());return dispatchMap(d);
 }
 public Map<String,Object> custody(String actor,Long dispatchId,String action,Integer scripts,String externalName,String unit,String role,String notes){
  User u=user(actor);ExamDispatchRecord d=dispatches.findById(dispatchId).orElseThrow(()->bad("Dispatch record not found."));
  if(!isHod(u)&&!Objects.equals(d.getDispatchUser().getId(),u.getId()))throw bad("Only HOD or assigned dispatch officer can update custody.");
  String a=action==null?"":action.toUpperCase();LocalDateTime now=LocalDateTime.now(LAGOS);
  switch(a){
   case "COLLECT"->{if(d.getCollectedAt()!=null)throw bad("Collection has already been recorded.");d.setCollectedAt(now);}
   case "DELIVER"->{if(d.getCollectedAt()==null||d.getDeliveredAt()!=null)throw bad("Record collection first; delivery can be recorded once.");d.setDeliveredAt(now);}
   case "RECEIVE_SCRIPTS"->{if(d.getDeliveredAt()==null||d.getScriptsReceivedAt()!=null)throw bad("Scripts can be received once after delivery.");if(scripts==null||scripts<0)throw bad("Enter the number of scripts received.");if(externalName==null||externalName.isBlank()||unit==null||unit.isBlank())throw bad("Name and unit of the person returning scripts are required.");d.setScriptsCount(scripts);d.setReturnedByName(externalName.trim());d.setReturnedByUnit(unit.trim());d.setReturnedByRole(role);d.setScriptsReceivedAt(now);d.setReceivedBy(u);}
   default->throw bad("Action must be COLLECT, DELIVER or RECEIVE_SCRIPTS.");
  }
  d.setNotes(notes);log(u,"DISPATCH_"+a,"DISPATCH_RECORD",dispatchId,"Custody updated");return dispatchMap(d);
 }
 public Map<String,Object> collectScripts(String actor,Long taskId,Long lecturerId,Integer quantity,String notes){User hod=hod(actor);ExamCourseTask t=task(taskId);User lecturer=userById(lecturerId);
  if(quantity==null||quantity<0)throw bad("Script quantity cannot be negative.");if(!assigned(lecturer,t))throw bad("Lecturer is not allocated to this course.");
  ExamScriptBatch b=new ExamScriptBatch();b.setTask(t);b.setLecturer(lecturer);b.setCollectedBy(hod);b.setQuantityCollected(quantity);b.setNotes(notes);b.setCollectedAt(LocalDateTime.now(LAGOS));batches.save(b);log(hod,"SCRIPTS_COLLECTED","SCRIPT_BATCH",b.getId(),"Quantity "+quantity);return batchMap(b);
 }
 public Map<String,Object> returnMarkedScripts(String actor,Long batchId,Integer quantity,String notes){User lecturer=user(actor);ExamScriptBatch b=batches.findById(batchId).orElseThrow(()->bad("Script batch not found."));
  if(!b.getLecturer().getId().equals(lecturer.getId())&&!isHod(lecturer))throw bad("Only the assigned lecturer can return this batch.");
  if(quantity==null||quantity<0||quantity>b.getQuantityCollected())throw bad("Returned quantity must be between zero and quantity collected.");
  if(b.getReturnedAt()!=null)throw bad("This batch has already been returned.");b.setQuantityReturned(quantity);b.setReturnedAt(LocalDateTime.now(LAGOS));b.setReturnedTo(lecturer);b.setNotes(notes);log(lecturer,"MARKED_SCRIPTS_RETURNED","SCRIPT_BATCH",batchId,"Returned "+quantity);return batchMap(b);
 }
 public List<Map<String,Object>> myScriptBatches(String actor){User u=user(actor);return batches.findByLecturerIdOrderByCollectedAtDesc(u.getId()).stream().map(this::batchMap).toList();}
 public List<Map<String,Object>> dispatchBoard(String actor){User u=user(actor);List<ExamDispatchRecord> rows=isHod(u)?dispatches.findAll():dispatches.findByDispatchUserIdOrderByAssignedAtDesc(u.getId());return rows.stream().map(this::dispatchMap).toList();}
 public Map<String,Object> markingReport(String actor){hod(actor);List<Map<String,Object>> rows=new ArrayList<>();for(ExamCourseTask t:tasks.findAll()){
  List<ExamScriptBatch> bs=batches.findByTaskIdOrderByCollectedAtDesc(t.getId());int collected=bs.stream().mapToInt(x->x.getQuantityCollected()==null?0:x.getQuantityCollected()).sum();int returned=bs.stream().mapToInt(x->x.getQuantityReturned()==null?0:x.getQuantityReturned()).sum();
  if(!bs.isEmpty())rows.add(Map.of("course",t.getCourse().getCode(),"lecturers",bs.stream().map(x->x.getLecturer().getFullName()).distinct().toList(),"collected",collected,"returned",returned,"outstanding",collected-returned));}
  return Map.of("courses",rows);}
 public Map<String,Object> dispatchSummary(String actor){hod(actor);List<Map<String,Object>> out=new ArrayList<>();for(ExamDispatchRecord d:dispatches.findAll())out.add(dispatchMap(d));return Map.of("dispatches",out);}
 private boolean assigned(User u,ExamCourseTask t){return t.getCourse()!=null&&u!=null&&allocationsFor(t).contains(u.getId());}
 private Set<Long> allocationsFor(ExamCourseTask t){return allocationRepository.findForCourse(t.getCourse().getId(),t.getCycle().getAcademicSession().getId(),t.getCycle().getSemester()).stream().filter(a->a.getLecturer()!=null).map(a->a.getLecturer().getId()).collect(java.util.stream.Collectors.toSet());}
 private final CourseAllocationRepository allocationRepository;
 private ExamCourseTask task(Long id){return tasks.findById(id).orElseThrow(()->bad("Exam task not found."));}
 private User user(String n){return users.findByUsername(n).orElseThrow(()->bad("Account not found."));}private User userById(Long id){return users.findById(id).orElseThrow(()->bad("Staff account not found."));}
 private User hod(String n){User u=user(n);if(!isHod(u))throw bad("Only HOD or ADMIN can perform this action.");return u;}private boolean isHod(User u){return Set.of("HOD","ADMIN").contains(u.getRole().toUpperCase());}
 private void log(User u,String a,String t,Long id,String details){ExamAuditEvent e=new ExamAuditEvent();e.setActor(u);e.setAction(a);e.setTargetType(t);e.setTargetId(id);e.setDetails(details);audit.save(e);}
 private Map<String,Object> sittingMap(ExamSitting s){Map<String,Object> m=new LinkedHashMap<>();m.put("taskId",s.getTask().getId());m.put("courseCode",s.getTask().getCourse().getCode());m.put("courseTitle",s.getTask().getCourse().getTitle());m.put("examDate",s.getTask().getExamDate());m.put("venue",s.getVenue());m.put("startTime",s.getStartTime());m.put("endTime",s.getEndTime());m.put("published",s.isPublished());return m;}
 private Map<String,Object> dispatchMap(ExamDispatchRecord d){Map<String,Object> m=new LinkedHashMap<>();m.put("id",d.getId());m.put("course",d.getTask().getCourse().getCode());m.put("examDate",d.getTask().getExamDate());m.put("venue",d.getVenue());m.put("assignedTo",d.getDispatchUser().getFullName());m.put("collectedAt",d.getCollectedAt());m.put("deliveredAt",d.getDeliveredAt());m.put("scriptsReceivedAt",d.getScriptsReceivedAt());m.put("scriptsCount",d.getScriptsCount());m.put("returnedBy",d.getReturnedByName());m.put("returnedByUnit",d.getReturnedByUnit());return m;}
 private Map<String,Object> batchMap(ExamScriptBatch b){Map<String,Object> m=new LinkedHashMap<>();m.put("id",b.getId());m.put("course",b.getTask().getCourse().getCode());m.put("lecturer",b.getLecturer().getFullName());m.put("quantityCollected",b.getQuantityCollected());m.put("quantityReturned",b.getQuantityReturned());m.put("outstanding",b.getQuantityCollected()-(b.getQuantityReturned()==null?0:b.getQuantityReturned()));m.put("collectedAt",b.getCollectedAt());m.put("returnedAt",b.getReturnedAt());m.put("notes",b.getNotes());return m;}
 private IllegalArgumentException bad(String m){return new IllegalArgumentException(m);}
}