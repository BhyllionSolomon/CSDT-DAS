package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.service.ExamTimetableImportService;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/exams/timetable-import") @CrossOrigin(origins="*")
public class ExamTimetableImportController{
 private final ExamTimetableImportService service;public ExamTimetableImportController(ExamTimetableImportService s){service=s;}
 private String name(Authentication a){if(a==null||!a.isAuthenticated())throw new IllegalArgumentException("Sign in required.");return a.getName();}
 @PostMapping(value="/{cycleId}",consumes="multipart/form-data") public Object upload(@PathVariable Long cycleId,@RequestParam("file") MultipartFile file,Authentication a){return service.upload(name(a),cycleId,file);}
 @PostMapping("/{rowId}/confirm") public Object confirm(@PathVariable Long rowId,@RequestBody ConfirmRequest r,Authentication a){return service.confirm(name(a),rowId,r.venue());}
 public record ConfirmRequest(String venue){}
}