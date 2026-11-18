package com.kdu.csdtdas.backend.service;
import com.kdu.csdtdas.backend.entity.*;import com.kdu.csdtdas.backend.repository.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service @Transactional
public class ExamKeyRotationService{
 private final ExamCryptoService crypto;private final ExamPaperRepository papers;private final UserRepository users;private final ExamAuditEventRepository audit;
 public ExamKeyRotationService(ExamCryptoService c,ExamPaperRepository p,UserRepository u,ExamAuditEventRepository a){crypto=c;papers=p;users=u;audit=a;}
 public Map<String,Object> rewrapAll(String username){User actor=users.findByUsername(username).orElseThrow(()->new IllegalArgumentException("Account not found."));if(!Set.of("HOD","ADMIN").contains(actor.getRole().toUpperCase()))throw new IllegalArgumentException("Only HOD/ADMIN can authorise master-key rotation.");if(!crypto.configured())throw new IllegalStateException("Active examination master key is not configured.");
 int count=0;for(ExamPaper paper:papers.findAll()){paper.setWrappedDataKey(crypto.rewrap(paper.getWrappedDataKey()));count++;}
 ExamAuditEvent e=new ExamAuditEvent();e.setActor(actor);e.setOccurredAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Africa/Lagos")));e.setAction("EXAM_MASTER_KEY_ROTATED");e.setTargetType("EXAM_PAPER_COLLECTION");e.setTargetId(0L);e.setDetails("Rewrapped "+count+" paper data keys using active master key.");audit.save(e);return Map.of("rewrappedPapers",count,"message","Rewrap completed. Remove APP_EXAM_PREVIOUS_MASTER_KEY from Render and restart the backend.");}
}