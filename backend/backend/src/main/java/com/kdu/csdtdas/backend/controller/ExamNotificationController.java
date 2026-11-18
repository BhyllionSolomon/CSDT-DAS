package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.entity.ExamNotification;import com.kdu.csdtdas.backend.repository.*;
import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.time.LocalDateTime;import java.util.*;
@RestController @RequestMapping("/api/exams/notifications") @CrossOrigin(origins="*")
public class ExamNotificationController{
 private final UserRepository users;private final ExamNotificationRepository repo;
 public ExamNotificationController(UserRepository u,ExamNotificationRepository r){users=u;repo=r;}
 private Long userId(Authentication a){if(a==null||!a.isAuthenticated())throw new IllegalArgumentException("Sign in required.");return users.findByUsername(a.getName()).orElseThrow().getId();}
 @GetMapping public Object list(Authentication a){return repo.findByRecipientIdOrderByCreatedAtDesc(userId(a)).stream().map(n->Map.of("id",n.getId(),"title",n.getTitle(),"message",n.getMessage(),"createdAt",n.getCreatedAt(),"read",n.getReadAt()!=null,"taskId",n.getTask().getId())).toList();}
 @PostMapping("/{id}/read") public Object read(@PathVariable Long id,Authentication a){Long uid=userId(a);ExamNotification n=repo.findById(id).orElseThrow(()->new IllegalArgumentException("Notification not found."));if(!n.getRecipient().getId().equals(uid))throw new IllegalArgumentException("Not authorised.");if(n.getReadAt()==null)n.setReadAt(LocalDateTime.now());return Map.of("message","Marked as read.");}
}