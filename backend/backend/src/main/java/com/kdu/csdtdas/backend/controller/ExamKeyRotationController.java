package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.service.ExamKeyRotationService;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/exams/security") @CrossOrigin(origins="*")
public class ExamKeyRotationController{private final ExamKeyRotationService service;public ExamKeyRotationController(ExamKeyRotationService s){service=s;}
 @PostMapping("/rewrap-all") public Object rotate(Authentication a){if(a==null||!a.isAuthenticated())throw new IllegalArgumentException("Sign in required.");return service.rewrapAll(a.getName());}}
