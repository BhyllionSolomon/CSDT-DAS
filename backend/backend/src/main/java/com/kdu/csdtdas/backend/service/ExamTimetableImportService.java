package com.kdu.csdtdas.backend.service;
import com.kdu.csdtdas.backend.entity.*;import com.kdu.csdtdas.backend.repository.*;
import org.apache.poi.xwpf.usermodel.*;import org.apache.pdfbox.Loader;import org.apache.pdfbox.pdmodel.PDDocument;import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.multipart.MultipartFile;
import java.io.*;import java.time.*;import java.time.format.*;import java.util.*;import java.util.regex.*;

@Service @Transactional
public class ExamTimetableImportService{
 private final UserRepository users;private final ExamCycleRepository cycles;private final ExamCourseTaskRepository tasks;private final ExamTimetableImportRowRepository rows;private final ExamSittingRepository sittings;private final ExamAuditEventRepository audit;
 private static final Pattern DATE=Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}|\\d{4}-\\d{2}-\\d{2})");
 private static final Pattern TIME=Pattern.compile("(?i)(\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)");
 public ExamTimetableImportService(UserRepository u,ExamCycleRepository c,ExamCourseTaskRepository t,ExamTimetableImportRowRepository r,ExamSittingRepository s,ExamAuditEventRepository a){users=u;cycles=c;tasks=t;rows=r;sittings=s;audit=a;}
 public Map<String,Object> upload(String username,Long cycleId,MultipartFile file){
  User hod=hod(username);cycles.findById(cycleId).orElseThrow(()->bad("Exam cycle not found."));
  if(file==null||file.isEmpty()||file.getSize()>20L*1024*1024)throw bad("Upload a non-empty PDF or DOCX timetable not exceeding 20 MB.");
  String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase();if(!name.endsWith(".pdf")&&!name.endsWith(".docx"))throw bad("Only PDF and DOCX timetables are accepted.");
  try{String text=name.endsWith(".docx")?docx(file.getBytes()):pdf(file.getBytes());List<ExamCourseTask> courseTasks=tasks.findByCycleIdOrderByExamDateAsc(cycleId);List<Map<String,Object>> matched=new ArrayList<>(),unmatched=new ArrayList<>();
   for(String line:text.split("\\R")){String normalized=line.toUpperCase(Locale.ROOT).trim();if(normalized.isBlank())continue;
    ExamCourseTask match=courseTasks.stream().filter(t->normalized.matches(".*(?<![A-Z0-9])"+Pattern.quote(t.getCourse().getCode().toUpperCase(Locale.ROOT))+"(?![A-Z0-9]).*")).findFirst().orElse(null);
    if(match==null){if(DATE.matcher(line).find())unmatched.add(Map.of("line",line));continue;}
    Matcher dm=DATE.matcher(line);LocalDate date=dm.find()?parseDate(dm.group(1)):null;Matcher tm=TIME.matcher(line);List<LocalTime> times=new ArrayList<>();while(tm.find())times.add(parseTime(tm.group(1)));
    ExamTimetableImportRow row=new ExamTimetableImportRow();row.setTask(match);row.setSourceLine(line);row.setProposedDate(date);if(!times.isEmpty())row.setProposedStart(times.get(0));if(times.size()>1)row.setProposedEnd(times.get(1));rows.save(row);
    Map<String,Object> m=new LinkedHashMap<>();m.put("rowId",row.getId());m.put("courseCode",match.getCourse().getCode());m.put("courseTitle",match.getCourse().getTitle());m.put("date",date);m.put("start",row.getProposedStart());m.put("end",row.getProposedEnd());m.put("sourceLine",line);m.put("needsReview",date==null);matched.add(m);
   }
   audit(hod,"TIMETABLE_IMPORTED","EXAM_CYCLE",cycleId,"Matched rows "+matched.size()+"; unmatched "+unmatched.size());
   Map<String,Object> out=new LinkedHashMap<>();out.put("matchedRows",matched);out.put("unmatchedRows",unmatched);out.put("message","Extraction is provisional. Confirm each row before publication.");return out;
  }catch(Exception e){if(e instanceof IllegalArgumentException)throw (IllegalArgumentException)e;throw bad("Could not read timetable. Check that the PDF/DOCX file is valid.");}
 }
 public Map<String,Object> confirm(String username,Long rowId,String venue){
  User hod=hod(username);ExamTimetableImportRow row=rows.findById(rowId).orElseThrow(()->bad("Imported row not found."));
  if(row.getProposedDate()==null)throw bad("This row has no detected date. It must be corrected manually before confirmation.");
  ExamCourseTask task=row.getTask();task.setExamDate(row.getProposedDate());
  ExamSitting sitting=sittings.findByTaskId(task.getId()).orElseGet(ExamSitting::new);sitting.setTask(task);sitting.setVenue(venue);sitting.setStartTime(row.getProposedStart());sitting.setEndTime(row.getProposedEnd());sitting.setConfirmed(true);sitting.setPublished(false);sitting.setConfirmedBy(hod);sitting.setConfirmedAt(LocalDateTime.now(ZoneId.of("Africa/Lagos")));sittings.save(sitting);
  row.setConfirmed(true);row.setConfirmedBy(hod);row.setConfirmedAt(LocalDateTime.now(ZoneId.of("Africa/Lagos")));audit(hod,"TIMETABLE_ROW_CONFIRMED","EXAM_TIMETABLE_ROW",rowId,"Course "+task.getCourse().getCode());return Map.of("rowId",rowId,"courseCode",task.getCourse().getCode(),"examDate",task.getExamDate(),"confirmed",true);
 }
 private String docx(byte[] b)throws Exception{try(XWPFDocument d=new XWPFDocument(new ByteArrayInputStream(b))){StringBuilder s=new StringBuilder();for(XWPFParagraph p:d.getParagraphs())s.append(p.getText()).append('\n');for(XWPFTable t:d.getTables())for(XWPFTableRow r:t.getRows()){for(XWPFTableCell c:r.getTableCells())s.append(c.getText()).append(" | ");s.append('\n');}return s.toString();}}
 private String pdf(byte[] b)throws Exception{try(PDDocument d=Loader.loadPDF(b)){return new PDFTextStripper().getText(d);}}
 private LocalDate parseDate(String s){for(DateTimeFormatter f:List.of(DateTimeFormatter.ofPattern("d/M/uuuu"),DateTimeFormatter.ofPattern("d-M-uuuu"),DateTimeFormatter.ISO_LOCAL_DATE)){try{return LocalDate.parse(s,f);}catch(Exception ignored){}}return null;}
 private LocalTime parseTime(String s){String v=s.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+","");for(DateTimeFormatter f:List.of(DateTimeFormatter.ofPattern("h:mma"),DateTimeFormatter.ofPattern("H:mm"))){try{return LocalTime.parse(v,f);}catch(Exception ignored){}}return null;}
 private User hod(String n){User u=users.findByUsername(n).orElseThrow(()->bad("Account not found."));if(!Set.of("HOD","ADMIN").contains(u.getRole().toUpperCase()))throw bad("Only HOD/ADMIN can import and confirm the timetable.");return u;}
 private void audit(User u,String a,String t,Long id,String d){ExamAuditEvent e=new ExamAuditEvent();e.setActor(u);e.setAction(a);e.setTargetType(t);e.setTargetId(id);e.setDetails(d);audit.save(e);}
 private IllegalArgumentException bad(String m){return new IllegalArgumentException(m);}
}