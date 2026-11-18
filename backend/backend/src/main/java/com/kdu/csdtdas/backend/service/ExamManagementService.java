package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.security.MessageDigest;

@Service
@Transactional
public class ExamManagementService {
    private final ExamCycleRepository cycles; private final ExamCourseTaskRepository tasks;
    private final ExamCommitteeMemberRepository committee; private final ExamPaperRepository papers;
    private final AcademicSessionRepository sessions; private final CourseRepository courses;
    private final UserRepository users; private final CourseAllocationRepository allocations;
    private final ExamCryptoService crypto; private final ExamAuditEventRepository audit; private final ExamPaperAccessRepository paperAccess; private final ExamNotificationRepository notifications;
    public ExamManagementService(ExamCycleRepository c,ExamCourseTaskRepository t,ExamCommitteeMemberRepository m,
      ExamPaperRepository p,AcademicSessionRepository s,CourseRepository co,UserRepository u,
      CourseAllocationRepository a,ExamCryptoService x,ExamAuditEventRepository audit,ExamPaperAccessRepository paperAccess,ExamNotificationRepository notifications){cycles=c;tasks=t;committee=m;papers=p;sessions=s;courses=co;users=u;allocations=a;crypto=x;this.audit=audit;this.paperAccess=paperAccess;this.notifications=notifications;}

    public Map<String,Object> configure(String username,Long sessionId,String semester,List<TaskInput> input){
      User hod=hod(username); AcademicSession session=sessions.findById(sessionId).orElseThrow(()->bad("Academic session not found."));
      String sem=semester(semester);
      ExamCycle cycle=cycles.findByAcademicSessionIdAndSemester(sessionId,sem).orElseGet(()->{ExamCycle c=new ExamCycle();c.setAcademicSession(session);c.setSemester(sem);c.setCreatedBy(hod);return cycles.save(c);});
      Set<Long> seen=new HashSet<>();
      for(TaskInput row:input){
        if(row.courseId()==null||!seen.add(row.courseId()))throw bad("Each course must appear once.");
        Course course=courses.findById(row.courseId()).orElseThrow(()->bad("Course not found."));
        if(row.examDate()==null||row.deadline()==null||row.vettingDate()==null||row.correctionDeadline()==null)throw bad("Exam date, submission deadline, vetting date and correction deadline are required.");
        if(row.deadline().isAfter(row.vettingDate())||row.vettingDate().isAfter(row.correctionDeadline()))throw bad("Dates must follow submission deadline, vetting date, correction deadline.");
        ExamCourseTask task=tasks.findByCycleIdAndCourseId(cycle.getId(),course.getId()).orElseGet(ExamCourseTask::new);
        task.setCycle(cycle);task.setCourse(course);task.setExamDate(row.examDate());task.setSubmissionDeadline(row.deadline());
        task.setVettingDate(row.vettingDate());task.setCorrectionDeadline(row.correctionDeadline());task.setMarkingDeadline(row.markingDeadline());
        int days=row.noticeDays()==null?49:row.noticeDays();if(days<1||days>180)throw bad("Notice window must be between 1 and 180 days.");
        task.setNoticeDays(days);tasks.save(task); audit(hod,"SCHEDULE_CONFIGURED","EXAM_TASK",task.getId(),"Course "+course.getCode());
      }
      return board(username,cycle.getId());
    }
    public void appoint(String username,Long cycleId,Long memberId){
      User hod=hod(username);ExamCycle cycle=cycles.findById(cycleId).orElseThrow(()->bad("Exam cycle not found."));
      User member=users.findById(memberId).orElseThrow(()->bad("Staff member not found."));
      if(!Set.of("LECTURER","ADJUNCT","LEVEL_ADVISER","HOD","ADMIN").contains(member.getRole().toUpperCase()))throw bad("Only academic staff can be appointed.");
      if(!committee.existsByCycleIdAndUserId(cycleId,memberId)){ExamCommitteeMember m=new ExamCommitteeMember();m.setCycle(cycle);m.setUser(member);m.setAppointedBy(hod);committee.save(m);audit(hod,"COMMITTEE_APPOINTED","EXAM_CYCLE",cycleId,"Appointed user ID "+memberId);}
    }
    public void removeCommitteeMember(String username,Long cycleId,Long memberId){User hod=hod(username);if(!committee.existsByCycleIdAndUserId(cycleId,memberId))throw bad("Committee appointment not found.");committee.deleteByCycleIdAndUserId(cycleId,memberId);audit(hod,"COMMITTEE_REVOKED","EXAM_CYCLE",cycleId,"Revoked user ID "+memberId);}
    @Transactional(readOnly=true)
    public Map<String,Object> board(String username,Long cycleId){
      User viewer=user(username);ExamCycle cycle=cycles.findById(cycleId).orElseThrow(()->bad("Exam cycle not found."));
      boolean privileged=isHod(viewer),member=committee.existsByCycleIdAndUserId(cycleId,viewer.getId());
      List<ExamCourseTask> visible=tasks.findByCycleIdOrderByExamDateAsc(cycleId);
      if(!privileged&&!member)visible=visible.stream().filter(t->assigned(viewer,t)&&!LocalDate.now(ZoneId.of("Africa/Lagos")).isBefore(t.getExamDate().minusDays(t.getNoticeDays()))).toList();
      List<Map<String,Object>> rows=new ArrayList<>();
      for(ExamCourseTask t:visible){Map<String,Object> r=new LinkedHashMap<>();r.put("taskId",t.getId());r.put("courseId",t.getCourse().getId());
        r.put("courseCode",t.getCourse().getCode());r.put("courseTitle",t.getCourse().getTitle());r.put("creditUnit",t.getCourse().getCreditUnit());
        r.put("examDate",t.getExamDate());r.put("submissionDeadline",t.getSubmissionDeadline());r.put("taskOpensOn",t.getExamDate().minusDays(t.getNoticeDays()));
        r.put("daysUntilExam",ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Africa/Lagos")),t.getExamDate()));r.put("canSubmit",assigned(viewer,t)&&canSubmit(viewer,t));
        if(privileged||member)r.put("vettingDate",t.getVettingDate());rows.add(r);}
      Map<String,Object> out=new LinkedHashMap<>();out.put("cycleId",cycleId);out.put("session",cycle.getAcademicSession().getName());out.put("semester",cycle.getSemester());out.put("tasks",rows);return out;
    }
    public Map<String,Object> submit(String username,Long taskId,MultipartFile file){
      User lecturer=user(username);if(!Set.of("LECTURER","ADJUNCT","LEVEL_ADVISER").contains(lecturer.getRole().toUpperCase()))throw bad("Only an assigned lecturer can submit.");
      ExamCourseTask task=tasks.findById(taskId).orElseThrow(()->bad("Exam task not found."));
      if(!assigned(lecturer,task))throw bad("You are not assigned to this course.");
      if(LocalDate.now(ZoneId.of("Africa/Lagos")).isBefore(task.getExamDate().minusDays(task.getNoticeDays())))throw bad("This task opens "+task.getNoticeDays()+" days before the examination.");
      LocalDateTime now=LocalDateTime.now(ZoneId.of("Africa/Lagos"));
      ExamPaper latest=papers.findByTaskIdOrderByVersionDesc(taskId).stream().filter(p->p.getLecturer().getId().equals(lecturer.getId())).max(Comparator.comparingInt(ExamPaper::getVersion)).orElse(null);
      if(latest!=null && "ACCEPTED".equals(latest.getStatus()))throw bad("The latest paper has been accepted; contact the HOD if a further change is required.");
      if(now.isAfter(task.getSubmissionDeadline()) && (latest==null || !"CORRECTION_REQUIRED".equals(latest.getStatus()) || task.getCorrectionDeadline()==null || now.isAfter(task.getCorrectionDeadline())))throw bad("The original submission deadline has passed. Resubmission is allowed only when corrections were requested and before the correction deadline.");
      if(file==null||file.isEmpty()||file.getSize()>15L*1024*1024)throw bad("Upload a non-empty PDF or DOCX file not exceeding 15 MB.");
      String type=file.getContentType()==null?"":file.getContentType().toLowerCase(),name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase();
      if(!(name.endsWith(".pdf")||name.endsWith(".docx"))||!(type.contains("pdf")||type.contains("wordprocessingml")||type.equals("application/octet-stream")))throw bad("Only PDF and DOCX files are accepted.");
      if(!crypto.configured())throw bad("Secure examination storage is not configured. Contact the HOD.");
      try{byte[] plain=file.getBytes();ExamCryptoService.Encrypted encrypted=crypto.encrypt(plain);
        int version=papers.findByTaskIdOrderByVersionDesc(taskId).stream().filter(p->p.getLecturer().getId().equals(lecturer.getId())).mapToInt(ExamPaper::getVersion).max().orElse(0)+1;
        ExamPaper paper=new ExamPaper();paper.setTask(task);paper.setLecturer(lecturer);paper.setFileName(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]","_"));
        paper.setContentType(type.contains("pdf")?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        paper.setEncryptedFile(encrypted.ciphertext());paper.setWrappedDataKey(encrypted.wrappedKey());paper.setContentSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(plain)));paper.setVersion(version);paper.setStatus("SUBMITTED");papers.save(paper);audit(lecturer,"PAPER_SUBMITTED","EXAM_PAPER",paper.getId(),"Task "+taskId+" version "+version);
        return Map.of("paperId",paper.getId(),"version",version,"status",paper.getStatus(),"submittedAt",paper.getSubmittedAt());
      }catch(Exception e){throw bad("Uploaded file could not be encrypted or read.");}
    }
    public List<Map<String,Object>> papers(String username,Long taskId){
      User viewer=user(username);ExamCourseTask task=tasks.findById(taskId).orElseThrow(()->bad("Exam task not found."));
      boolean privileged=isHod(viewer),member=committee.existsByCycleIdAndUserId(task.getCycle().getId(),viewer.getId());
      if(!privileged&&!member&&!assigned(viewer,task))throw bad("Not authorised to view submissions.");
      audit(viewer,"SUBMISSION_LIST_VIEWED","EXAM_TASK",taskId,"Viewed submission metadata");
      List<ExamPaper> list=papers.findByTaskIdOrderByVersionDesc(taskId);
      if(!privileged)list=list.stream().filter(p->p.getLecturer().getId().equals(viewer.getId()) || (member && hasPaperAccess(p.getId(),viewer.getId()))).toList();
      return list.stream().map(p->{Map<String,Object> r=new LinkedHashMap<>();r.put("paperId",p.getId());r.put("lecturer",p.getLecturer().getFullName());r.put("version",p.getVersion());r.put("status",p.getStatus());r.put("submittedAt",p.getSubmittedAt());r.put("reviewNotes",privileged||p.getLecturer().getId().equals(viewer.getId())||hasPaperAccess(p.getId(),viewer.getId())?p.getReviewNotes():null);return r;}).toList();
    }
    public void review(String username,Long paperId,String decision,String notes){
      User reviewer=user(username);ExamPaper paper=papers.findById(paperId).orElseThrow(()->bad("Paper not found."));
      boolean member=committee.existsByCycleIdAndUserId(paper.getTask().getCycle().getId(),reviewer.getId());
      if(!isHod(reviewer)&&(!member||!hasPaperAccess(paperId,reviewer.getId())))throw bad("HOD must grant you access to this paper before review.");
      String d=decision==null?"":decision.trim().toUpperCase();if(!Set.of("ACCEPTED","CORRECTION_REQUIRED").contains(d))throw bad("Choose ACCEPTED or CORRECTION_REQUIRED.");
      if(notes==null||notes.isBlank())throw bad("Review notes are required.");
      paper.setStatus(d);paper.setReviewNotes(notes.trim());paper.setReviewedBy(reviewer);paper.setReviewedAt(LocalDateTime.now(ZoneId.of("Africa/Lagos")));audit(reviewer,"PAPER_REVIEWED","EXAM_PAPER",paperId,d); notifyLecturer(paper.getLecturer(),paper.getTask(),"REVIEW_"+paperId+"_"+paper.getVersion(),"Question paper review completed", "Your "+paper.getTask().getCourse().getCode()+" question paper v"+paper.getVersion()+" was reviewed: "+d+". Feedback: "+notes.trim());
    }
    public Download download(String username,Long paperId){
      User viewer=user(username);ExamPaper paper=papers.findById(paperId).orElseThrow(()->bad("Paper not found."));
      boolean member=committee.existsByCycleIdAndUserId(paper.getTask().getCycle().getId(),viewer.getId()),own=paper.getLecturer().getId().equals(viewer.getId());
      if(!isHod(viewer)&&!own&&(!member||!hasPaperAccess(paperId,viewer.getId())))throw bad("HOD has not authorised access to this paper.");
      audit(viewer,"PAPER_DOWNLOADED","EXAM_PAPER",paperId,"Confidential paper decrypted for authorised user");
      return new Download(crypto.decrypt(paper.getEncryptedFile(),paper.getWrappedDataKey()),paper.getFileName(),paper.getContentType());
    }
    public Map<String,Object> submissionReport(String username){
      hod(username);List<Map<String,Object>> result=new ArrayList<>();
      for(ExamCourseTask task:tasks.findAll()){
        for(CourseAllocation allocation:allocations.findForCourse(task.getCourse().getId(),task.getCycle().getAcademicSession().getId(),task.getCycle().getSemester())){
          User lecturer=allocation.getLecturer();if(lecturer==null)continue;
          ExamPaper latest=papers.findByTaskIdOrderByVersionDesc(task.getId()).stream().filter(p->p.getLecturer().getId().equals(lecturer.getId())).max(Comparator.comparingInt(ExamPaper::getVersion)).orElse(null);
          Map<String,Object> row=new LinkedHashMap<>();row.put("course",task.getCourse().getCode());row.put("lecturer",lecturer.getFullName());row.put("examDate",task.getExamDate());row.put("deadline",task.getSubmissionDeadline());row.put("submitted",latest!=null);row.put("status",latest==null?"NOT_SUBMITTED":latest.getStatus());row.put("latestVersion",latest==null?0:latest.getVersion());row.put("overdue",latest==null&&LocalDateTime.now().isAfter(task.getSubmissionDeadline()));result.add(row);
        }
      }
      return Map.of("submissions",result);
    }
    private void notifyLecturer(User recipient,ExamCourseTask task,String key,String title,String message){
      if(notifications.existsByRecipientIdAndTaskIdAndNotificationKey(recipient.getId(),task.getId(),key))return;
      ExamNotification n=new ExamNotification();n.setRecipient(recipient);n.setTask(task);n.setNotificationKey(key);n.setTitle(title);n.setMessage(message);notifications.save(n);
    }
    public void grantPaperAccess(String username,Long paperId,Long memberId,LocalDateTime expiresAt){
      User hod=hod(username);ExamPaper paper=papers.findById(paperId).orElseThrow(()->bad("Paper not found."));
      User member=users.findById(memberId).orElseThrow(()->bad("Committee member not found."));
      if(!committee.existsByCycleIdAndUserId(paper.getTask().getCycle().getId(),memberId))throw bad("Only an appointed committee member can receive paper access.");
      if(expiresAt!=null&&!expiresAt.isAfter(LocalDateTime.now()))throw bad("Access expiry must be in the future.");
      ExamPaperAccess access=paperAccess.findByPaperIdAndUserId(paperId,memberId).orElseGet(ExamPaperAccess::new);
      access.setPaper(paper);access.setUser(member);access.setGrantedBy(hod);access.setExpiresAt(expiresAt);access.setRevokedAt(null);paperAccess.save(access);
      audit(hod,"PAPER_ACCESS_GRANTED","EXAM_PAPER",paperId,"Granted to "+member.getUsername()+" until "+expiresAt);
    }
    public void revokePaperAccess(String username,Long paperId,Long memberId){
      User hod=hod(username);ExamPaperAccess access=paperAccess.findByPaperIdAndUserId(paperId,memberId).orElseThrow(()->bad("Access grant not found."));
      access.setRevokedAt(LocalDateTime.now());audit(hod,"PAPER_ACCESS_REVOKED","EXAM_PAPER",paperId,"Revoked user ID "+memberId);
    }
    private boolean hasPaperAccess(Long paperId,Long userId){return paperAccess.findByPaperIdAndUserId(paperId,userId).filter(a->a.getRevokedAt()==null&&(a.getExpiresAt()==null||a.getExpiresAt().isAfter(LocalDateTime.now()))).isPresent();}
    private boolean canSubmit(User lecturer,ExamCourseTask task){
      LocalDateTime now=LocalDateTime.now(ZoneId.of("Africa/Lagos"));
      ExamPaper latest=papers.findByTaskIdOrderByVersionDesc(task.getId()).stream().filter(p->p.getLecturer().getId().equals(lecturer.getId())).max(Comparator.comparingInt(ExamPaper::getVersion)).orElse(null);
      if(latest!=null&&"ACCEPTED".equals(latest.getStatus()))return false;
      if(!now.isAfter(task.getSubmissionDeadline()))return true;
      return latest!=null&&"CORRECTION_REQUIRED".equals(latest.getStatus())&&task.getCorrectionDeadline()!=null&&!now.isAfter(task.getCorrectionDeadline());
    }
    private void audit(User actor,String action,String type,Long id,String details){ExamAuditEvent e=new ExamAuditEvent();e.setActor(actor);e.setAction(action);e.setTargetType(type);e.setTargetId(id);e.setDetails(details);audit.save(e);}
    private boolean assigned(User u,ExamCourseTask t){return allocations.findForCourse(t.getCourse().getId(),t.getCycle().getAcademicSession().getId(),t.getCycle().getSemester()).stream().anyMatch(a->a.getLecturer()!=null&&a.getLecturer().getId().equals(u.getId()));}
    private User user(String n){return users.findByUsername(n).orElseThrow(()->bad("Account not found."));}
    private User hod(String n){User u=user(n);if(!isHod(u))throw bad("Only HOD or ADMIN can perform this action.");return u;}
    private boolean isHod(User u){return Set.of("HOD","ADMIN").contains(u.getRole().toUpperCase());}
    private String semester(String s){String v=s==null?"":s.trim().toUpperCase();if(!Set.of("FIRST","SECOND").contains(v))throw bad("Semester must be FIRST or SECOND.");return v;}
    private IllegalArgumentException bad(String m){return new IllegalArgumentException(m);}
    public record TaskInput(Long courseId,LocalDate examDate,LocalDateTime deadline,LocalDateTime vettingDate,LocalDateTime correctionDeadline,LocalDateTime markingDeadline,Integer noticeDays){}
    public record Download(byte[] data,String fileName,String contentType){}
}