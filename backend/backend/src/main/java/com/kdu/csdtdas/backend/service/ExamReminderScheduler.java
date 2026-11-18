package com.kdu.csdtdas.backend.service;
import com.kdu.csdtdas.backend.entity.*;import com.kdu.csdtdas.backend.repository.*;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;
@Component
public class ExamReminderScheduler{
 private static final ZoneId LAGOS=ZoneId.of("Africa/Lagos");
 private final ExamCourseTaskRepository tasks;private final CourseAllocationRepository allocations;private final ExamNotificationRepository notifications;
 public ExamReminderScheduler(ExamCourseTaskRepository t,CourseAllocationRepository a,ExamNotificationRepository n){tasks=t;allocations=a;notifications=n;}
 @Scheduled(cron="0 0 8 * * *",zone="Africa/Lagos") @Transactional
 public void sendDailyReminders(){LocalDate today=LocalDate.now(LAGOS);
  for(ExamCourseTask task:tasks.findAll()){
   long examDays=Duration.between(today.atStartOfDay(),task.getExamDate().atStartOfDay()).toDays();
   long deadlineDays=Duration.between(today.atStartOfDay(),task.getSubmissionDeadline().toLocalDate().atStartOfDay()).toDays();
   List<CourseAllocation> assigned=allocations.findForCourse(task.getCourse().getId(),task.getCycle().getAcademicSession().getId(),task.getCycle().getSemester());
   for(CourseAllocation ca:assigned){User recipient=ca.getLecturer();if(recipient==null)continue;
    if(Set.of(49L,14L,7L,3L,1L,0L).contains(examDays)) create(recipient,task,"EXAM_D"+examDays,"Examination approaching","Your "+task.getCourse().getCode()+" examination is in "+examDays+" day(s).");
    if(Set.of(7L,3L,1L,0L).contains(deadlineDays)) create(recipient,task,"DEADLINE_D"+deadlineDays,"Question-paper deadline reminder","Submit the question paper for "+task.getCourse().getCode()+". Deadline: "+task.getSubmissionDeadline()+".");
    if(today.isAfter(task.getSubmissionDeadline().toLocalDate()))create(recipient,task,"OVERDUE_"+today,"Question paper overdue","The question-paper deadline for "+task.getCourse().getCode()+" has passed. Submit immediately and contact the HOD.");
   }
  }
 }
 private void create(User user,ExamCourseTask task,String key,String title,String message){if(notifications.existsByRecipientIdAndTaskIdAndNotificationKey(user.getId(),task.getId(),key))return;ExamNotification n=new ExamNotification();n.setRecipient(user);n.setTask(task);n.setNotificationKey(key);n.setTitle(title);n.setMessage(message);notifications.save(n);}
}