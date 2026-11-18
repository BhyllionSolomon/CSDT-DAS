package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.service.ExamManagementService;
import com.kdu.csdtdas.backend.service.ExamQuestionValidationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins="*")
public class ExamManagementController {
 private final ExamManagementService service; private final ExamQuestionValidationService validation;
 public ExamManagementController(ExamManagementService s,ExamQuestionValidationService v){service=s;validation=v;}
 @PostMapping("/cycles/{sessionId}/{semester}/tasks")
 public Object configure(@PathVariable Long sessionId,@PathVariable String semester,@RequestBody List<ExamManagementService.TaskInput> tasks,Authentication a){return service.configure(name(a),sessionId,semester,tasks);}
 @PostMapping("/tasks/{taskId}/question-policy")
 public Object policy(@PathVariable Long taskId,@RequestBody ExamQuestionValidationService.PolicyInput body,Authentication a){return validation.savePolicy(name(a),taskId,body);}
 @PostMapping("/tasks/{taskId}/papers/{paperId}/validate")
 public Object validate(@PathVariable Long taskId,@PathVariable Long paperId,Authentication a){return validation.validate(name(a),taskId,paperId);}
 @PostMapping("/cycles/{cycleId}/committee/{userId}")
 public ResponseEntity<Void> appoint(@PathVariable Long cycleId,@PathVariable Long userId,Authentication a){service.appoint(name(a),cycleId,userId);return ResponseEntity.noContent().build();}
 @GetMapping("/reports/submissions") public Object submissionsReport(Authentication a){return service.submissionReport(name(a));}
 @GetMapping("/cycles/{cycleId}") public Object board(@PathVariable Long cycleId,Authentication a){return service.board(name(a),cycleId);}
 @PostMapping(value="/tasks/{taskId}/papers",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object submit(@PathVariable Long taskId,@RequestParam("file") MultipartFile file,Authentication a){return service.submit(name(a),taskId,file);}
 @GetMapping("/tasks/{taskId}/papers") public Object papers(@PathVariable Long taskId,Authentication a){return service.papers(name(a),taskId);}
 @PostMapping("/papers/{paperId}/access/{userId}")
 public ResponseEntity<Void> grant(@PathVariable Long paperId,@PathVariable Long userId,@RequestBody AccessRequest body,Authentication a){service.grantPaperAccess(name(a),paperId,userId,body.expiresAt());return ResponseEntity.noContent().build();}
 @DeleteMapping("/papers/{paperId}/access/{userId}")
 public ResponseEntity<Void> revoke(@PathVariable Long paperId,@PathVariable Long userId,Authentication a){service.revokePaperAccess(name(a),paperId,userId);return ResponseEntity.noContent().build();}
 @PutMapping("/papers/{paperId}/review")
 public ResponseEntity<Void> review(@PathVariable Long paperId,@RequestBody ReviewRequest b,Authentication a){service.review(name(a),paperId,b.decision(),b.notes());return ResponseEntity.noContent().build();}
 @GetMapping("/papers/{paperId}/file")
 public ResponseEntity<ByteArrayResource> download(@PathVariable Long paperId,Authentication a){
  var f=service.download(name(a),paperId);
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+f.fileName()+"\"").contentLength(f.data().length).body(new ByteArrayResource(f.data()));
 }
 private String name(Authentication a){if(a==null||!a.isAuthenticated()||"anonymousUser".equals(a.getPrincipal()))throw new IllegalArgumentException("Sign in required.");return a.getName();}
 public record ReviewRequest(String decision,String notes){}
 public record AccessRequest(LocalDateTime expiresAt){}
}