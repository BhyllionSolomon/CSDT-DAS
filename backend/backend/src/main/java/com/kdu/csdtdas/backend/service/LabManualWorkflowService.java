package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LabManualWorkflowDTO;
import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service @Transactional
public class LabManualWorkflowService {
 private static final Set<String> LABS=Set.of("COMPUTER_SCIENCE","SOFTWARE_ENGINEERING","CYBERSECURITY");
 private final LabManualSubmissionRepository submissions;
 private final CourseRepository courses;
 private final LabManualRepository manuals;
 private final LabManualOrderRepository orders;
 public LabManualWorkflowService(LabManualSubmissionRepository s,CourseRepository c,LabManualRepository m,LabManualOrderRepository o){submissions=s;courses=c;manuals=m;orders=o;}

 public LabManualWorkflowDTO.Row upload(String username,String role,String title,Long courseId,Integer pages,BigDecimal suggested,MultipartFile file){
  if(!Set.of("LECTURER","ADJUNCT").contains(role)) throw denied();
  if(title==null||title.isBlank()||title.length()>150) throw bad("Enter a manual title.");
  if(pages==null||pages<1||pages>2000) throw bad("Enter a valid page count.");
  if(file==null||file.isEmpty()||file.getSize()>15L*1024*1024) throw bad("Upload a PDF not larger than 15 MB.");
  try{
   byte[] bytes=file.getBytes();
   if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-') throw bad("The uploaded file is not a valid PDF.");
   LabManualSubmission s=new LabManualSubmission(); s.setTitle(title.trim());
   s.setCourse(courses.findById(courseId).orElseThrow(()->bad("Course not found.")));
   s.setPageCount(pages); s.setSuggestedLevy(suggested); s.setUploadedBy(username);
   s.setFileName(file.getOriginalFilename()==null?"course-manual.pdf":file.getOriginalFilename());
   s.setFileData(bytes); s.setContentType("application/pdf");
   return row(submissions.save(s));
  }catch(java.io.IOException e){throw bad("Could not read the uploaded PDF.");}
 }
 @Transactional(readOnly=true) public List<LabManualWorkflowDTO.Row> mine(String username){return submissions.findAllByUploadedByOrderBySubmittedAtDesc(username).stream().map(this::row).toList();}
 @Transactional(readOnly=true) public List<LabManualWorkflowDTO.Row> hodQueue(String role){if(!Set.of("HOD","ADMIN").contains(role))throw denied();return submissions.findAllByStatusOrderBySubmittedAtAsc("SUBMITTED_TO_HOD").stream().map(this::row).toList();}
 @Transactional(readOnly=true) public List<LabManualWorkflowDTO.Buyer> buyers(String username,String role,Long submissionId){
  LabManualSubmission s=get(submissionId);
  if(!Set.of("HOD","ADMIN").contains(role)&&!s.getUploadedBy().equals(username))throw denied();
  LabManual manual=manuals.findAllWithCourse().stream().filter(m->m.getCourse()!=null&&m.getCourse().getId().equals(s.getCourse().getId())&&m.getTitle().equals(s.getTitle())).findFirst().orElse(null);
  if(manual==null)return List.of();
  return orders.findAllByManualIdOrderByCreatedAtDesc(manual.getId()).stream().map(o->new LabManualWorkflowDTO.Buyer(
   o.getId(),o.getStudent().getMatricNumber(),o.getStudent().getFullName(),
   o.getStudent().getProgramme()==null?"":o.getStudent().getProgramme().getName(),
   o.getStudent().getLevel()==null?"":o.getStudent().getLevel().getName(),o.getAmount(),o.getStatus(),o.getPaidAt()
  )).toList();
 }
 @Transactional(readOnly=true) public List<LabManualWorkflowDTO.Row> labQueue(String role){if(!Set.of("LAB_OFFICER","TECHNICAL_STAFF","HOD","ADMIN").contains(role))throw denied();return submissions.findAll().stream().filter(s->"SENT_TO_LAB".equals(s.getStatus())||"RECEIVED".equals(s.getStatus())).map(this::row).toList();}
 public LabManualWorkflowDTO.Row review(String username,String role,Long id,LabManualWorkflowDTO.Review req,boolean approve){
  if(!Set.of("HOD","ADMIN").contains(role))throw denied();
  LabManualSubmission s=get(id); if(!"SUBMITTED_TO_HOD".equals(s.getStatus()))throw bad("This submission has already been reviewed.");
  s.setHodReviewedBy(username);s.setHodComment(req==null?null:req.comment());
  if(approve){if(req.approvedLevy()==null||req.approvedLevy().compareTo(BigDecimal.ZERO)<=0)throw bad("HOD must set a levy greater than zero.");s.setApprovedLevy(req.approvedLevy().setScale(2,java.math.RoundingMode.HALF_UP));s.setStatus("APPROVED");s.setApprovedAt(LocalDateTime.now());}
  else{s.setStatus("RETURNED");}
  return row(s);
 }
 public LabManualWorkflowDTO.Row dispatch(String username,String role,Long id,String lab){
  if(!Set.of("LECTURER","ADJUNCT").contains(role))throw denied(); LabManualSubmission s=get(id);
  if(!s.getUploadedBy().equals(username))throw denied();
  if(!"APPROVED".equals(s.getStatus()))throw bad("Only an HOD-approved manual can be sent to a laboratory.");
  if(!LABS.contains(lab))throw bad("Choose Computer Science, Software Engineering or Cybersecurity.");
  s.setTargetLab(lab);s.setSentToLabBy(username);s.setSentToLabAt(LocalDateTime.now());s.setStatus("SENT_TO_LAB");return row(s);
 }
 public LabManualWorkflowDTO.Row receive(String username,String role,Long id){
  if(!Set.of("LAB_OFFICER","TECHNICAL_STAFF","HOD","ADMIN").contains(role))throw denied();LabManualSubmission s=get(id);
  if(!"SENT_TO_LAB".equals(s.getStatus()))throw bad("This manual is not awaiting receipt.");
  s.setReceivedBy(username);s.setReceivedAt(LocalDateTime.now());s.setStatus("RECEIVED");
  // Publish the HOD-priced manual into the existing student catalogue, without allowing a lecturer to set its price.
  if(manuals.findAllWithCourse().stream().noneMatch(m->m.getCourse()!=null&&m.getCourse().getId().equals(s.getCourse().getId())&&m.getTitle().equals(s.getTitle()))){
   LabManual m=new LabManual();m.setTitle(s.getTitle());m.setCourse(s.getCourse());m.setUnitPrice(s.getApprovedLevy());m.setActive(true);manuals.save(m);
  } else {
   LabManual m=manuals.findAllWithCourse().stream().filter(x->x.getCourse()!=null&&x.getCourse().getId().equals(s.getCourse().getId())&&x.getTitle().equals(s.getTitle())).findFirst().orElseThrow();
   m.setUnitPrice(s.getApprovedLevy());m.setActive(true);
  }
  return row(s);
 }
 @Transactional(readOnly=true) public byte[] download(String username,String role,Long id){
  LabManualSubmission s=get(id);
  boolean allowed=Set.of("HOD","ADMIN","LAB_OFFICER","TECHNICAL_STAFF").contains(role)|| (Set.of("LECTURER","ADJUNCT").contains(role)&&s.getUploadedBy().equals(username));
  if(!allowed)throw denied();return s.getFileData();
 }
 private LabManualSubmission get(Long id){return submissions.findById(id).orElseThrow(()->bad("Manual submission not found."));
 }
 private LabManualWorkflowDTO.Row row(LabManualSubmission s){return new LabManualWorkflowDTO.Row(s.getId(),s.getTitle(),s.getCourse().getCode(),s.getCourse().getTitle(),s.getPageCount(),s.getSuggestedLevy(),s.getApprovedLevy(),s.getStatus(),s.getTargetLab(),s.getUploadedBy(),s.getHodReviewedBy(),s.getHodComment(),s.getReceivedBy(),s.getSubmittedAt(),s.getApprovedAt(),s.getReceivedAt());
 }
 private RuntimeException denied(){return new org.springframework.security.access.AccessDeniedException("You are not authorised to perform this action.");
 }
 private RuntimeException bad(String m){return new IllegalArgumentException(m);
 }
}
  }
  return row(s);
 }
 @Transactional(readOnly=true) public byte[] download(String username,String role,Long id){
  LabManualSubmission s=get(id);
  boolean allowed=Set.of("HOD","ADMIN","LAB_OFFICER","TECHNICAL_STAFF").contains(role)|| (Set.of("LECTURER","ADJUNCT").contains(role)&&s.getUploadedBy().equals(username));
  if(!allowed)throw denied();return s.getFileData();
 }
 private LabManualSubmission get(Long id){return submissions.findById(id).orElseThrow(()->bad("Manual submission not found."));}
 private LabManualWorkflowDTO.Row row(LabManualSubmission s){return new LabManualWorkflowDTO.Row(s.getId(),s.getTitle(),s.getCourse().getCode(),s.getCourse().getTitle(),s.getPageCount(),s.getSuggestedLevy(),s.getApprovedLevy(),s.getStatus(),s.getTargetLab(),s.getUploadedBy(),s.getHodReviewedBy(),s.getHodComment(),s.getReceivedBy(),s.getSubmittedAt(),s.getApprovedAt(),s.getReceivedAt());}
 private RuntimeException denied(){return new org.springframework.security.access.AccessDeniedException("You are not authorised to perform this action.");}
 private RuntimeException bad(String m){return new IllegalArgumentException(m);}
}