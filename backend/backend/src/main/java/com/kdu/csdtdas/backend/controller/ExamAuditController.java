package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.entity.ExamAuditEvent;
import com.kdu.csdtdas.backend.repository.ExamAuditEventRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/exams/audit") @CrossOrigin(origins="*")
public class ExamAuditController {
 private final ExamAuditEventRepository events; private final UserRepository users;
 public ExamAuditController(ExamAuditEventRepository e,UserRepository u){events=e;users=u;}
 @GetMapping @Transactional(readOnly=true)
 public List<Map<String,Object>> list(Authentication auth){
  if(auth==null||!auth.isAuthenticated())throw new IllegalArgumentException("Sign in required.");
  var actor=users.findByUsername(auth.getName()).orElseThrow(()->new IllegalArgumentException("Account not found."));
  if(!"HOD".equalsIgnoreCase(actor.getRole()))throw new IllegalArgumentException("Only the HOD may view examination audit history.");
  return events.findTop300ByOrderByOccurredAtDesc().stream().map(e->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getId());m.put("actor",e.getActor().getFullName());m.put("action",e.getAction());m.put("targetType",e.getTargetType());m.put("targetId",e.getTargetId());m.put("details",e.getDetails());m.put("occurredAt",e.getOccurredAt());return m;}).toList();
 }
}