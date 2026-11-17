package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.ProjectSuiteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProjectSuiteController {

    private final ProjectSuiteService service;

    public ProjectSuiteController(ProjectSuiteService service) {
        this.service = service;
    }

    // ---- supervisors, coordinators and the H.O.D

    @GetMapping("/project-suite/cases")
    public List<ProjectSuiteService.Summary> cases(
            @RequestParam(required = false) Long sessionId, @RequestParam(defaultValue = "false") boolean all, Authentication a) {
        return service.cases(name(a), sessionId, all);
    }

    @GetMapping("/project-suite/alerts")
    public List<ProjectSuiteService.Summary> alerts(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.alerts(name(a), sessionId);
    }

    @GetMapping("/project-suite/cases/{id}")
    public ProjectSuiteService.CaseView caseView(@PathVariable Long id, Authentication a) {
        return service.caseView(name(a), id);
    }

    @PutMapping("/project-suite/cases/{id}/stage")
    public ProjectSuiteService.Message stage(@PathVariable Long id, @RequestBody ProjectSuiteService.StageRequest body, Authentication a) {
        return service.setStage(name(a), id, body);
    }

    @PostMapping("/project-suite/cases/{id}/meetings")
    public ProjectSuiteService.Message addMeeting(@PathVariable Long id, @RequestBody ProjectSuiteService.MeetingRequest body, Authentication a) {
        return service.addMeeting(name(a), id, body);
    }

    @DeleteMapping("/project-suite/meetings/{id}")
    public ProjectSuiteService.Message deleteMeeting(@PathVariable Long id, Authentication a) {
        return service.deleteMeeting(name(a), id);
    }

    @PutMapping("/project-suite/cases/{id}/assessment")
    public ProjectSuiteService.Message assessment(@PathVariable Long id, @RequestBody ProjectSuiteService.AssessmentRequest body, Authentication a) {
        return service.saveAssessment(name(a), id, body);
    }

    @PostMapping("/project-suite/cases/{id}/presentations")
    public ProjectSuiteService.Message schedule(@PathVariable Long id, @RequestBody ProjectSuiteService.ScheduleRequest body, Authentication a) {
        return service.schedule(name(a), id, body);
    }

    @PutMapping("/project-suite/presentations/{id}/outcome")
    public ProjectSuiteService.Message outcome(@PathVariable Long id, @RequestBody ProjectSuiteService.OutcomeRequest body, Authentication a) {
        return service.setOutcome(name(a), id, body);
    }

    @PostMapping("/project-suite/presentations/{id}/feedback")
    public ProjectSuiteService.Message feedback(@PathVariable Long id, @RequestBody ProjectSuiteService.FeedbackRequest body, Authentication a) {
        return service.addFeedback(name(a), id, body);
    }

    @PutMapping("/project-survey/window")
    public ProjectSuiteService.Message window(@RequestBody ProjectSuiteService.WindowRequest body, Authentication a) {
        return service.setSurveyWindow(name(a), body);
    }

    @GetMapping("/project-survey/results")
    public ProjectSuiteService.SurveyResults results(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.surveyResults(name(a), sessionId);
    }

    // ---- students (the safety filter lets them reach only these three)

    @GetMapping("/project-suite/mine")
    public ProjectSuiteService.StudentView mine(Authentication a) {
        return service.mine(name(a));
    }

    @GetMapping("/project-survey/state")
    public ProjectSuiteService.SurveyState state(Authentication a) {
        return service.surveyState(name(a));
    }

    @PostMapping("/project-survey/submit")
    public ProjectSuiteService.Message submit(@RequestBody ProjectSuiteService.SurveyRequest body, Authentication a) {
        return service.submitSurvey(name(a), body);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}