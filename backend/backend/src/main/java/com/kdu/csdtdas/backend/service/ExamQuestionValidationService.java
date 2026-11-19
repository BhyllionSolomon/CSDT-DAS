package com.kdu.csdtdas.backend.service;
import com.kdu.csdtdas.backend.entity.*;import com.kdu.csdtdas.backend.repository.*;
import org.apache.poi.xwpf.usermodel.*;import org.apache.pdfbox.Loader;import org.apache.pdfbox.cos.COSName;import org.apache.pdfbox.pdmodel.PDDocument;import org.apache.pdfbox.pdmodel.PDPage;import org.apache.pdfbox.pdmodel.PDResources;import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.io.*;import java.util.*;import java.util.regex.*;import java.math.BigDecimal;

@Service @Transactional
public class ExamQuestionValidationService{
 private final ExamQuestionPolicyRepository policies;private final ExamCourseTaskRepository tasks;private final ExamPaperRepository papers;private final UserRepository users;private final ExamCryptoService crypto;private final ExamAuditEventRepository audit;
 public ExamQuestionValidationService(ExamQuestionPolicyRepository p,ExamCourseTaskRepository t,ExamPaperRepository papers,UserRepository u,ExamCryptoService c,ExamAuditEventRepository a){policies=p;tasks=t;this.papers=papers;users=u;crypto=c;audit=a;}
 public Map<String,Object> savePolicy(String username,Long taskId,PolicyInput in){
  User hod=hod(username);ExamCourseTask task=tasks.findById(taskId).orElseThrow(()->bad("Exam task not found."));
  if(in.questionCount()==null||in.answerCount()==null||in.totalMarks()==null||in.questionCount()<1||in.questionCount()>30||in.answerCount()<1||in.answerCount()>in.questionCount()||in.totalMarks()<1)throw bad("Invalid question count, answer count or total marks.");
  List<BigDecimal> marks=parse(in.marksCsv());if(marks.size()!=in.questionCount()||marks.stream().anyMatch(x->x.signum()<=0))throw bad("Provide one positive mark value for each question.");
  Set<Integer> compulsory=parseSet(in.compulsoryCsv());if(compulsory.stream().anyMatch(x->x<1||x>in.questionCount())||compulsory.size()>in.answerCount())throw bad("Compulsory question numbers must be valid and cannot exceed answer count.");
  if(!allCombinationsTotal(marks,in.answerCount(),compulsory,in.choiceGroupsCsv(),in.totalMarks()))throw bad("The marks do not total "+in.totalMarks()+" for every permitted answer combination. Adjust marks or compulsory questions.");
  ExamQuestionPolicy p=policies.findByCycleIdAndCourseId(task.getCycle().getId(),task.getCourse().getId()).orElseGet(ExamQuestionPolicy::new);
  p.setCycle(task.getCycle());p.setCourse(task.getCourse());p.setQuestionCount(in.questionCount());p.setAnswerCount(in.answerCount());p.setTotalMarks(in.totalMarks());p.setMarksCsv(in.marksCsv());p.setCompulsoryCsv(in.compulsoryCsv());p.setChoiceGroupsCsv(in.choiceGroupsCsv());p.setDuration(in.duration());policies.save(p);
  log(hod,"QUESTION_POLICY_SET",taskId,"Configured marks and choice policy");return Map.of("questionCount",p.getQuestionCount(),"answerCount",p.getAnswerCount(),"totalMarks",p.getTotalMarks());
 }
 public Map<String,Object> validate(String username,Long taskId,Long paperId){
  User viewer=user(username);ExamCourseTask task=tasks.findById(taskId).orElseThrow(()->bad("Exam task not found."));
  if(!isHod(viewer))throw bad("Only HOD may run the formal paper validation.");
  ExamPaper paper=papers.findById(paperId).orElseThrow(()->bad("Paper not found."));if(!paper.getTask().getId().equals(taskId))throw bad("Paper does not belong to this examination task.");
  byte[] bytes=crypto.decrypt(paper.getEncryptedFile(),paper.getWrappedDataKey());String name=paper.getFileName().toLowerCase();String text;
  try{if(name.endsWith(".docx"))text=docx(bytes);else text=pdf(bytes);}catch(Exception e){throw bad("Could not extract document text. For scanned PDFs, OCR and manual review are required.");}
  ExamQuestionPolicy policy=policies.findByCycleIdAndCourseId(task.getCycle().getId(),task.getCourse().getId()).orElseThrow(()->bad("HOD must configure the question policy before validation."));
  List<String> errors=new ArrayList<>(),warnings=new ArrayList<>();
  String normalized=text.toUpperCase(Locale.ROOT);
  if(!normalized.contains("KOLA DAISI UNIVERSITY"))errors.add("Institutional heading not detected.");
  if(!normalized.contains(task.getCourse().getCode().toUpperCase(Locale.ROOT)))errors.add("Course code is missing or does not match.");
  if(!normalized.contains(task.getCourse().getTitle().toUpperCase(Locale.ROOT)))errors.add("Course title is missing or does not match.");
  if(!normalized.contains(task.getCourse().getCreditUnit()+" UNIT")&&!normalized.contains(task.getCourse().getCreditUnit()+" CREDIT"))errors.add("Course credit-unit information is missing.");
  if(policy.getDuration()!=null&&!policy.getDuration().isBlank()&&!normalized.contains(policy.getDuration().toUpperCase(Locale.ROOT)))errors.add("Configured examination duration was not detected.");
  Set<Integer> numbers=new TreeSet<>();Matcher matcher=Pattern.compile("(?im)^\\s*(?:QUESTION\\s*)?(\\d{1,2})\\s*[.) :]").matcher(text);while(matcher.find())numbers.add(Integer.parseInt(matcher.group(1)));
  if(numbers.size()!=policy.getQuestionCount())errors.add("Expected "+policy.getQuestionCount()+" numbered questions; detected "+numbers.size()+".");
  for(int i=1;i<=policy.getQuestionCount();i++)if(!numbers.contains(i))errors.add("Question number "+i+" is missing.");
  List<BigDecimal> marks=parse(policy.getMarksCsv());Set<Integer> compulsory=parseSet(policy.getCompulsoryCsv());
  if(!allCombinationsTotal(marks,policy.getAnswerCount(),compulsory,policy.getChoiceGroupsCsv(),policy.getTotalMarks()))errors.add("Configured answer combinations do not total the required marks.");
  if(!normalized.contains("ANSWER"))warnings.add("Answer-choice instructions were not clearly detected; committee must verify.");
  List<String> markChecks=checkPrintedMarks(text,marks);errors.addAll(markChecks);
  if(markChecks.isEmpty())warnings.add("Printed mark notation was not consistently machine-readable; verify the mark allocations manually.");
  if(!hasEmbeddedImage(bytes,name))warnings.add("No embedded image was detected; verify that the approved school logo is present.");
  warnings.add("Logo identity, visual formatting and scanned content require human verification.");
  if(text.isBlank())warnings.add("No extractable text was found. OCR is required for this scanned document.");
  log(viewer,"PAPER_VALIDATED",taskId,"Paper "+paperId+" validation completed");
  return Map.of("paperId",paperId,"errors",errors,"warnings",warnings,"passed",errors.isEmpty(),"humanReviewRequired",true,"sha256",paper.getContentSha256()==null?"":paper.getContentSha256());
 }
 private boolean hasEmbeddedImage(byte[] bytes,String name){
  try{if(name.endsWith(".docx")){try(XWPFDocument d=new XWPFDocument(new ByteArrayInputStream(bytes))){return !d.getAllPictures().isEmpty();}}
   try(PDDocument d=Loader.loadPDF(bytes)){for(PDPage page:d.getPages()){PDResources r=page.getResources();if(r!=null)for(COSName n:r.getXObjectNames())if(r.getXObject(n) instanceof PDImageXObject)return true;}return false;}
  }catch(Exception e){return false;}
 }
 private List<String> checkPrintedMarks(String text,List<BigDecimal> expected){
  List<String> errors=new ArrayList<>();Matcher q=Pattern.compile("(?im)^\\s*(?:QUESTION\\s*)?(\\d{1,2})\\s*[.) :]").matcher(text);List<Integer> starts=new ArrayList<>();List<Integer> nums=new ArrayList<>();while(q.find()){starts.add(q.start());nums.add(Integer.parseInt(q.group(1)));}
  Pattern markPattern=Pattern.compile("(?i)(?:\\(|\\[)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:MARKS?|MKS?)\\s*(?:\\)|\\])");
  int found=0;for(int i=0;i<starts.size();i++){int number=nums.get(i);if(number<1||number>expected.size())continue;int end=i+1<starts.size()?starts.get(i+1):text.length();Matcher m=markPattern.matcher(text.substring(starts.get(i),end));BigDecimal actual=null;while(m.find())actual=new BigDecimal(m.group(1));if(actual!=null){found++;if(actual.subtract(expected.get(number-1)).abs().compareTo(new BigDecimal("0.01"))>0)errors.add("Printed marks for question "+number+" differ from the HOD policy.");}}
  if(found==0)errors.add("No machine-readable question mark allocations were detected; verify all question marks manually.");return errors;
 }
 private String docx(byte[] b)throws Exception{try(XWPFDocument d=new XWPFDocument(new ByteArrayInputStream(b))){StringBuilder s=new StringBuilder();for(XWPFParagraph p:d.getParagraphs())s.append(p.getText()).append('\n');for(XWPFTable t:d.getTables())for(XWPFTableRow r:t.getRows())for(XWPFTableCell c:r.getTableCells())s.append(c.getText()).append('\n');return s.toString();}}
 private String pdf(byte[] b)throws Exception{try(PDDocument d=Loader.loadPDF(b)){return new PDFTextStripper().getText(d);}}
 boolean allCombinationsTotal(List<BigDecimal> marks,int answerCount,Set<Integer> compulsory,String groupsCsv,int total){
  if(marks.isEmpty()||answerCount<1||answerCount>marks.size())return false;
  Set<Integer> grouped=new HashSet<>();List<Set<Integer>> groups=new ArrayList<>();
  if(groupsCsv!=null&&!groupsCsv.isBlank()){
   try{for(String groupText:groupsCsv.split("\\|")){Set<Integer> group=new LinkedHashSet<>();for(String part:groupText.split(",")){int q=Integer.parseInt(part.trim());if(q<1||q>marks.size()||!group.add(q)||compulsory.contains(q)||!grouped.add(q))return false;}if(group.isEmpty())return false;groups.add(group);}}catch(Exception ex){return false;}
  }
  int mandatoryCount=compulsory.size()+groups.size();if(mandatoryCount>answerCount)return false;
  List<Integer> free=new ArrayList<>();for(int q=1;q<=marks.size();q++)if(!compulsory.contains(q)&&!grouped.contains(q))free.add(q);
  int freeChoose=answerCount-mandatoryCount;if(freeChoose<0||freeChoose>free.size())return false;
  Set<BigDecimal> sums=new HashSet<>();BigDecimal fixed=BigDecimal.ZERO;
  for(int q:compulsory)fixed=fixed.add(marks.get(q-1));
  sums.add(fixed.stripTrailingZeros());
  for(Set<Integer> group:groups){Set<BigDecimal> next=new HashSet<>();for(BigDecimal sum:sums)for(int q:group)next.add(sum.add(marks.get(q-1)).stripTrailingZeros());sums=next;}
  List<Set<BigDecimal>> dp=new ArrayList<>();for(int i=0;i<=freeChoose;i++)dp.add(new HashSet<>());dp.get(0).add(BigDecimal.ZERO);
  for(int q:free){BigDecimal mark=marks.get(q-1);for(int k=freeChoose;k>=1;k--)for(BigDecimal sum:new ArrayList<>(dp.get(k-1)))dp.get(k).add(sum.add(mark).stripTrailingZeros());}
  Set<BigDecimal> permitted=new HashSet<>();for(BigDecimal base:sums)for(BigDecimal extra:dp.get(freeChoose))permitted.add(base.add(extra).stripTrailingZeros());
  return !permitted.isEmpty()&&permitted.stream().allMatch(x->x.compareTo(BigDecimal.valueOf(total))==0);
 }
 private List<BigDecimal> parse(String csv){if(csv==null||csv.isBlank())return List.of();try{return Arrays.stream(csv.split(",")).map(String::trim).map(BigDecimal::new).toList();}catch(Exception e){throw bad("Marks must be comma-separated numbers.");}}
 private Set<Integer> parseSet(String csv){if(csv==null||csv.isBlank())return Set.of();try{Set<Integer>s=new HashSet<>();for(String x:csv.split(","))s.add(Integer.parseInt(x.trim()));return s;}catch(Exception e){throw bad("Compulsory question numbers must be comma-separated whole numbers.");}}
 private User user(String n){return users.findByUsername(n).orElseThrow(()->bad("Account not found."));}private User hod(String n){User u=user(n);if(!isHod(u))throw bad("Only the HOD can configure policy.");return u;}private boolean isHod(User u){return "HOD".equals(u.getRole().toUpperCase());}
 private void log(User u,String a,Long id,String detail){ExamAuditEvent e=new ExamAuditEvent();e.setActor(u);e.setOccurredAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Africa/Lagos")));e.setAction(a);e.setTargetType("EXAM_TASK");e.setTargetId(id);e.setDetails(detail);audit.save(e);}
 private IllegalArgumentException bad(String m){return new IllegalArgumentException(m);}
 public record PolicyInput(Integer questionCount,Integer answerCount,Integer totalMarks,String marksCsv,String compulsoryCsv,String choiceGroupsCsv,String duration){}
}