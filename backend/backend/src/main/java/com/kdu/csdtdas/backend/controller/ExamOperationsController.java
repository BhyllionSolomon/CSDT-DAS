package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.service.ExamOperationsService;
import org.springframework.http.ResponseEntity;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import java.time.LocalTime;
@RestController @RequestMapping("/api/exams") @CrossOrigin(origins="*")
public class ExamOperationsController{
 private final ExamOperationsService service;public ExamOperationsController(ExamOperationsService s){service=s;}
 private String name(Authentication a){if(a==null||!a.isAuthenticated()||"anonymousUser".equals(a.getPrincipal()))throw new IllegalArgumentException("Sign in required.");return a.getName();}
 @PostMapping("/tasks/{taskId}/sitting") public Object confirm(@PathVariable Long taskId,@RequestBody SittingRequest r,Authentication a){return service.confirmSitting(name(a),taskId,r.venue(),r.startTime(),r.endTime());}
 @PostMapping("/tasks/{taskId}/publish") public Object publish(@PathVariable Long taskId,Authentication a){return service.publishSitting(name(a),taskId);}
 @GetMapping("/my-timetable") public Object studentTimetable(Authentication a){return service.studentTimetable(name(a));}
 @PostMapping("/tasks/{taskId}/dispatch") public Object assign(@PathVariable Long taskId,@RequestBody DispatchRequest r,Authentication a){return service.assignDispatch(name(a),taskId,r.userId(),r.venue());}
 @PostMapping("/dispatch/{id}/custody") public Object custody(@PathVariable Long id,@RequestBody CustodyRequest r,Authentication a){return service.custody(name(a),id,r.action(),r.scriptsCount(),r.returnedByName(),r.returnedByUnit(),r.returnedByRole(),r.notes());}
 @GetMapping("/dispatch") public Object dispatchBoard(Authentication a){return service.dispatchBoard(name(a));}
 @GetMapping("/reports/dispatch") public Object dispatchSummary(Authentication a){return service.dispatchSummary(name(a));}
 @PostMapping("/tasks/{taskId}/script-batches") public Object collect(@PathVariable Long taskId,@RequestBody CollectRequest r,Authentication a){return service.collectScripts(name(a),taskId,r.lecturerId(),r.quantity(),r.notes());}
 @PostMapping("/script-batches/{id}/return") public Object returnScripts(@PathVariable Long id,@RequestBody ReturnRequest r,Authentication a){return service.returnMarkedScripts(name(a),id,r.quantity(),r.notes());}
 @GetMapping("/my-script-batches") public Object myBatches(Authentication a){return service.myScriptBatches(name(a));}
 @GetMapping("/reports/marking") public Object markingReport(Authentication a){return service.markingReport(name(a));}
 public record SittingRequest(String venue,LocalTime startTime,LocalTime endTime){}
 public record DispatchRequest(Long userId,String venue){}
 public record CustodyRequest(String action,Integer scriptsCount,String returnedByName,String returnedByUnit,String returnedByRole,String notes){}
 public record CollectRequest(Long lecturerId,Integer quantity,String notes){}
 public record ReturnRequest(Integer quantity,String notes){}
}