package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.PresentationFeedback;
import com.kdu.csdtdas.backend.entity.ProjectAssessment;
import com.kdu.csdtdas.backend.entity.ProjectMeeting;
import com.kdu.csdtdas.backend.entity.ProjectPresentation;
import com.kdu.csdtdas.backend.entity.ProjectStage;
import com.kdu.csdtdas.backend.entity.ProjectSupervision;
import com.kdu.csdtdas.backend.entity.ProjectSurveyReceipt;
import com.kdu.csdtdas.backend.entity.ProjectSurveyResponse;
import com.kdu.csdtdas.backend.entity.ProjectSurveyWindow;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.PresentationFeedbackRepository;
import com.kdu.csdtdas.backend.repository.ProjectAssessmentRepository;
import com.kdu.csdtdas.backend.repository.ProjectMeetingRepository;
import com.kdu.csdtdas.backend.repository.ProjectPresentationRepository;
import com.kdu.csdtdas.backend.repository.ProjectStageRepository;
import com.kdu.csdtdas.backend.repository.ProjectSupervisionRepository;
import com.kdu.csdtdas.backend.repository.ProjectSurveyReceiptRepository;
import com.kdu.csdtdas.backend.repository.ProjectSurveyResponseRepository;
import com.kdu.csdtdas.backend.repository.ProjectSurveyWindowRepository;
import com.kdu.csdtdas.backend.repository.StaffProfileRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectSuiteService {

    // ---- rules you can change
    private static final int MISSED_WARN = 2;
    private static final int MISSED_REPORT = 3;
    private static final int FIRST_MEETING_DAYS = 21;
    private static final int MEETING_GAP_DAYS = 28;
    private static final int NO_PROGRESS_DAYS = 45;
    private static final int SURVEY_MINIMUM = 5;

    private static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");
    private static final List<String[]> STAGES = List.of(
            new String[]{"TOPIC", "Topic approved"}, new String[]{"PROPOSAL", "Proposal"},
            new String[]{"CH1", "Chapter 1: Introduction"}, new String[]{"CH2", "Chapter 2: Literature review"},
            new String[]{"CH3", "Chapter 3: Methodology"}, new String[]{"CH4", "Chapter 4: Results and discussion"},
            new String[]{"CH5", "Chapter 5: Conclusion"}, new String[]{"DRAFT", "Full draft"},
            new String[]{"SIMILARITY", "Plagiarism and AI check"}, new String[]{"FINAL", "Final submission"});
    private static final Set<String> STAGE_STATUS = Set.of("NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "APPROVED");
    private static final Set<String> MODES = Set.of("IN_PERSON", "ONLINE", "PHONE");
    private static final Set<String> OUTCOMES = Set.of("PASSED", "CORRECTIONS_REQUIRED", "REPRESENT");
    private static final Set<String> STAFF = Set.of("HOD", "ADMIN", "LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    public record StageDto(String key, String label, String status, String note, String updatedAt) {}
    public record MeetingDto(Long id, String date, String mode, boolean attended, String summary, String nextSteps, String loggedBy) {}
    public record AssessmentDto(String problemStatement, String objectives, String publishable, Double similarity, Double ai,
                                String checkedOn, Integer diligence, Integer communication, Integer initiative, String comment) {}
    public record FeedbackDto(String assessor, String comments) {}
    public record PresentationDto(Long id, int round, String scheduledAt, String venue, String outcome, String outcomeNote, List<FeedbackDto> feedback) {}
    public record Summary(Long id, String student, String matric, String programme, String classification, int stagesDone, int stagesTotal,
                          int meetings, int missed, String lastMeeting, String latestOutcome, String severity, List<String> alerts) {}
    public record CaseView(Summary summary, String supervisor, String session, List<StageDto> stages, List<MeetingDto> meetings,
                           AssessmentDto assessment, List<PresentationDto> presentations, boolean canCoordinate) {}
    public record StudentView(boolean assigned, String supervisor, String session, List<StageDto> stages,
                              List<MeetingDto> meetings, List<PresentationDto> presentations) {}

    public record StageRequest(String stage, String status, String note) {}
    public record MeetingRequest(String date, String mode, Boolean attended, String summary, String nextSteps) {}
    public record AssessmentRequest(String problemStatement, String objectives, String publishable, Double similarity, Double ai,
                                    String checkedOn, Integer diligence, Integer communication, Integer initiative, String comment) {}
    public record ScheduleRequest(String scheduledAt, String venue) {}
    public record OutcomeRequest(String outcome, String note) {}
    public record FeedbackRequest(String comments) {}
    public record WindowRequest(Long sessionId, Boolean open) {}
    public record Message(String message) {}

    public record SurveyState(boolean assigned, boolean open, boolean submitted, String supervisor) {}
    public record SurveyRequest(String moneyCollected, String moneyDetails, String inappropriate, String inappropriateDetails,
                                String ghostwritten, String ghostwrittenDetails, Integer professional, Integer helpful, Integer available,
                                Integer overall, String impact, String problemSolved, String acknowledged, String acknowledgedDetails, String comment) {}
    public record SupervisorResult(Long supervisorId, String name, int responses, boolean visible, Double professional, Double helpful,
                                   Double available, Double overall, int moneyReports, int conductReports, int integrityReports) {}
    public record Concern(String supervisor, String type, String details) {}
    public record SurveyResults(int minimum, boolean open, List<SupervisorResult> supervisors, List<Concern> concerns,
                                List<String> impact, List<String> acknowledgements, List<String> comments) {}

    private final ProjectSupervisionRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectMeetingRepository meetingRepository;
    private final ProjectAssessmentRepository assessmentRepository;
    private final ProjectPresentationRepository presentationRepository;
    private final PresentationFeedbackRepository feedbackRepository;
    private final ProjectSurveyWindowRepository windowRepository;
    private final ProjectSurveyResponseRepository responseRepository;
    private final ProjectSurveyReceiptRepository receiptRepository;
    private final AcademicSessionRepository sessionRepository;
    private final StaffProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final RegistrationClearanceService clearanceService;

    public ProjectSuiteService(
            ProjectSupervisionRepository projectRepository,
            ProjectStageRepository stageRepository,
            ProjectMeetingRepository meetingRepository,
            ProjectAssessmentRepository assessmentRepository,
            ProjectPresentationRepository presentationRepository,
            PresentationFeedbackRepository feedbackRepository,
            ProjectSurveyWindowRepository windowRepository,
            ProjectSurveyResponseRepository responseRepository,
            ProjectSurveyReceiptRepository receiptRepository,
            AcademicSessionRepository sessionRepository,
            StaffProfileRepository profileRepository,
            UserRepository userRepository,
            RegistrationClearanceService clearanceService
    ) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.meetingRepository = meetingRepository;
        this.assessmentRepository = assessmentRepository;
        this.presentationRepository = presentationRepository;
        this.feedbackRepository = feedbackRepository;
        this.windowRepository = windowRepository;
        this.responseRepository = responseRepository;
        this.receiptRepository = receiptRepository;
        this.sessionRepository = sessionRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.clearanceService = clearanceService;
    }

    // ============================================================ staff: cases

    @Transactional(readOnly = true)
    public List<Summary> cases(String username, Long sessionId, boolean all) {
        User user = staff(username);
        AcademicSession session = session(sessionId);
        boolean wide = all && (isHod(user) || isCoordinator(user));

        return projectRepository.findForSession(session.getId()).stream()
                .filter(p -> wide || p.getSupervisor().getId().equals(user.getId()))
                .map(this::summaryOf)
                .sorted(Comparator.comparingInt((Summary s) -> rank(s.severity())).thenComparing(Summary::student))
                .toList();
    }

    /** Students who need the H.O.D's or a coordinator's attention. */
    @Transactional(readOnly = true)
    public List<Summary> alerts(String username, Long sessionId) {
        User user = staff(username);
        if (!isHod(user) && !isCoordinator(user)) throw bad("Only the H.O.D or a Project Coordinator can see the alerts.");
        return cases(username, sessionId, true).stream().filter(s -> !s.severity().equals("NONE")).toList();
    }

    @Transactional(readOnly = true)
    public CaseView caseView(String username, Long id) {
        User user = staff(username);
        ProjectSupervision p = supervision(id);
        authorize(user, p);

        List<ProjectStage> stages = stageRepository.findBySupervisionId(id);
        List<ProjectMeeting> meetings = meetingRepository.findBySupervisionIdOrderByMeetingDateDesc(id);
        List<ProjectPresentation> presentations = presentationRepository.findBySupervisionIdOrderByRoundNoAsc(id);
        ProjectAssessment a = assessmentRepository.findBySupervisionId(id).orElse(null);

        return new CaseView(summaryOf(p), p.getSupervisor().getFullName(), p.getAcademicSession().getName(),
                stageDtos(stages), meetingDtos(meetings), a == null ? null : assessmentDto(a),
                presentationDtos(presentations), isHod(user) || isCoordinator(user));
    }

    public Message setStage(String username, Long id, StageRequest req) {
        User user = staff(username);
        ProjectSupervision p = supervision(id);
        authorize(user, p);

        String key = req.stage() == null ? "" : req.stage().trim().toUpperCase();
        if (STAGES.stream().noneMatch(s -> s[0].equals(key))) throw bad("Unknown stage.");
        String status = req.status() == null ? "" : req.status().trim().toUpperCase();
        if (!STAGE_STATUS.contains(status)) throw bad("Choose Not started, In progress, Submitted or Approved.");

        ProjectStage s = stageRepository.findBySupervisionId(id).stream().filter(x -> x.getStage().equals(key)).findFirst().orElseGet(ProjectStage::new);
        s.setSupervision(p);
        s.setStage(key);
        s.setStatus(status);
        s.setNote(text(req.note(), 300));
        s.setUpdatedBy(user.getUsername());
        s.setUpdatedAt(LocalDateTime.now());
        stageRepository.save(s);
        return new Message("Stage updated.");
    }

    // ---------------------------------------------------------------- meetings

    public Message addMeeting(String username, Long id, MeetingRequest req) {
        User user = staff(username);
        ProjectSupervision p = supervision(id);
        authorize(user, p);

        LocalDate date;
        try {
            date = LocalDate.parse(req.date());
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Enter the meeting date.");
        }
        if (date.isAfter(LocalDate.now(LAGOS))) throw bad("A meeting cannot be in the future.");

        String mode = req.mode() == null ? "IN_PERSON" : req.mode().trim().toUpperCase();
        if (!MODES.contains(mode)) throw bad("Choose In person, Online or Phone.");

        ProjectMeeting m = new ProjectMeeting();
        m.setSupervision(p);
        m.setMeetingDate(date);
        m.setMode(mode);
        m.setAttended(req.attended() == null || req.attended());
        m.setSummary(text(req.summary(), 600));
        m.setNextSteps(text(req.nextSteps(), 400));
        m.setLoggedBy(user.getUsername());
        meetingRepository.save(m);
        return new Message(m.isAttended() ? "Meeting recorded." : "Recorded as a missed meeting.");
    }

    public Message deleteMeeting(String username, Long meetingId) {
        User user = staff(username);
        ProjectMeeting m = meetingRepository.findById(meetingId).orElseThrow(() -> bad("Meeting not found."));
        authorize(user, m.getSupervision());
        meetingRepository.delete(m);
        return new Message("Meeting removed.");
    }

    // -------------------------------------------------------------- assessment

    public Message saveAssessment(String username, Long id, AssessmentRequest req) {
        User user = staff(username);
        ProjectSupervision p = supervision(id);
        authorize(user, p);

        ProjectAssessment a = assessmentRepository.findBySupervisionId(id).orElseGet(ProjectAssessment::new);
        a.setSupervision(p);
        a.setProblemStatement(choice(req.problemStatement(), Set.of("YES", "PARTLY", "NO"), "problem statement"));
        a.setObjectives(choice(req.objectives(), Set.of("YES", "PARTLY", "NO"), "objectives"));
        a.setPublishable(choice(req.publishable(), Set.of("YES", "NO", "WITH_REVISION"), "publishable"));
        a.setSimilarityPercent(percent(req.similarity(), "The plagiarism percentage"));
        a.setAiPercent(percent(req.ai(), "The AI-detection percentage"));

        if (req.checkedOn() != null && !req.checkedOn().isBlank()) {
            try {
                a.setReportCheckedOn(LocalDate.parse(req.checkedOn()));
            } catch (DateTimeParseException e) {
                throw bad("Enter the date the report was checked.");
            }
        } else {
            a.setReportCheckedOn(null);
        }

        a.setDiligence(rating(req.diligence()));
        a.setCommunication(rating(req.communication()));
        a.setInitiative(rating(req.initiative()));
        a.setComment(text(req.comment(), 600));
        a.setUpdatedBy(user.getUsername());
        a.setUpdatedAt(LocalDateTime.now());
        assessmentRepository.save(a);
        return new Message("Assessment saved.");
    }

    // ----------------------------------------------------------- presentations

    /** Sets the first date, changes a pending date, or books the next round after a re-presentation. */
    public Message schedule(String username, Long id, ScheduleRequest req) {
        User user = staff(username);
        requireCoordinator(user);
        ProjectSupervision p = supervision(id);

        LocalDateTime when;
        try {
            when = LocalDateTime.parse(req.scheduledAt());
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Choose the presentation date and time.");
        }

        List<ProjectPresentation> rounds = presentationRepository.findBySupervisionIdOrderByRoundNoAsc(id);
        ProjectPresentation latest = rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);

        ProjectPresentation target;
        if (latest == null) {
            target = new ProjectPresentation();
            target.setSupervision(p);
            target.setRoundNo(1);
        } else if (latest.getOutcome().equals("PENDING")) {
            target = latest;
        } else if (latest.getOutcome().equals("REPRESENT")) {
            target = new ProjectPresentation();
            target.setSupervision(p);
            target.setRoundNo(latest.getRoundNo() + 1);
        } else {
            throw bad("This student's latest presentation is already " + latest.getOutcome().replace('_', ' ').toLowerCase()
                    + ". Only a re-presentation can be scheduled again.");
        }

        target.setScheduledAt(when);
        target.setVenue(text(req.venue(), 100));
        target.setScheduledBy(user.getUsername());
        presentationRepository.save(target);
        return new Message("Presentation " + target.getRoundNo() + " scheduled.");
    }

    public Message setOutcome(String username, Long presentationId, OutcomeRequest req) {
        User user = staff(username);
        requireCoordinator(user);
        ProjectPresentation pr = presentationRepository.findById(presentationId).orElseThrow(() -> bad("Presentation not found."));

        String outcome = req.outcome() == null ? "" : req.outcome().trim().toUpperCase();
        if (!OUTCOMES.contains(outcome)) throw bad("Choose Passed, Corrections required or Re-present.");

        pr.setOutcome(outcome);
        pr.setOutcomeNote(text(req.note(), 500));
        presentationRepository.save(pr);
        return new Message(outcome.equals("REPRESENT") ? "Recorded. Schedule the next presentation for this student." : "Outcome recorded.");
    }

    /** Any staff member on the panel can say what the student must correct. */
    public Message addFeedback(String username, Long presentationId, FeedbackRequest req) {
        User user = staff(username);
        ProjectPresentation pr = presentationRepository.findById(presentationId).orElseThrow(() -> bad("Presentation not found."));

        String comments = text(req.comments(), 1500);
        if (comments == null) throw bad("Write what the student must correct.");

        PresentationFeedback f = feedbackRepository.findByPresentationIdAndAssessorId(presentationId, user.getId()).orElseGet(PresentationFeedback::new);
        f.setPresentation(pr);
        f.setAssessor(user);
        f.setComments(comments);
        f.setUpdatedAt(LocalDateTime.now());
        feedbackRepository.save(f);
        return new Message("Your feedback is saved.");
    }

    // ================================================================= student

    @Transactional(readOnly = true)
    public StudentView mine(String username) {
        Student student = studentOf(username);
        ProjectSupervision p = latestSupervision(student);
        if (p == null) return new StudentView(false, null, null, List.of(), List.of(), List.of());

        return new StudentView(true, p.getSupervisor().getFullName(), p.getAcademicSession().getName(),
                stageDtos(stageRepository.findBySupervisionId(p.getId())),
                meetingDtos(meetingRepository.findBySupervisionIdOrderByMeetingDateDesc(p.getId())),
                presentationDtos(presentationRepository.findBySupervisionIdOrderByRoundNoAsc(p.getId())));
    }

    // ================================================================== survey

    @Transactional(readOnly = true)
    public SurveyState surveyState(String username) {
        Student student = studentOf(username);
        ProjectSupervision p = latestSupervision(student);
        if (p == null) return new SurveyState(false, false, false, null);

        Long sid = p.getAcademicSession().getId();
        boolean open = windowRepository.findByAcademicSessionId(sid).map(ProjectSurveyWindow::isOpen).orElse(false);
        return new SurveyState(true, open, receiptRepository.existsByStudentIdAndAcademicSessionId(student.getId(), sid), p.getSupervisor().getFullName());
    }

    /** Saves the anonymous form and the receipt together, or neither. */
    public Message submitSurvey(String username, SurveyRequest req) {
        Student student = studentOf(username);
        ProjectSupervision p = latestSupervision(student);
        if (p == null) throw bad("You have not been assigned a project supervisor.");

        AcademicSession session = p.getAcademicSession();
        if (!windowRepository.findByAcademicSessionId(session.getId()).map(ProjectSurveyWindow::isOpen).orElse(false)) {
            throw bad("This form is not open yet.");
        }
        if (receiptRepository.existsByStudentIdAndAcademicSessionId(student.getId(), session.getId())) {
            throw bad("You have already submitted this form.");
        }

        ProjectSurveyResponse r = new ProjectSurveyResponse();
        r.setSupervisor(p.getSupervisor());
        r.setAcademicSession(session);

        r.setMoneyCollected(yesNo(req.moneyCollected(), "whether anyone collected money from you"));
        r.setMoneyDetails(detail(r.getMoneyCollected(), req.moneyDetails(), "who collected money, and what for"));
        r.setInappropriate(yesNo(req.inappropriate(), "whether there were any inappropriate moments"));
        r.setInappropriateDetails(detail(r.getInappropriate(), req.inappropriateDetails(), "what happened"));
        r.setGhostwritten(yesNo(req.ghostwritten(), "whether anyone wrote your report or pressured you"));
        r.setGhostwrittenDetails(detail(r.getGhostwritten(), req.ghostwrittenDetails(), "what happened"));

        r.setProfessional(required(req.professional(), "Professionalism"));
        r.setHelpful(required(req.helpful(), "Helpfulness"));
        r.setAvailable(required(req.available(), "Availability"));
        r.setOverall(required(req.overall(), "Overall rating"));

        r.setImpact(text(req.impact(), 1000));
        r.setProblemSolved(text(req.problemSolved(), 1000));
        String ack = req.acknowledged() == null ? "" : req.acknowledged().trim().toUpperCase();
        if (!Set.of("YES", "NO", "N/A").contains(ack)) throw bad("Say whether all contributors were acknowledged.");
        r.setAcknowledged(ack);
        r.setAcknowledgedDetails(text(req.acknowledgedDetails(), 500));
        r.setComment(text(req.comment(), 1000));

        ProjectSurveyReceipt receipt = new ProjectSurveyReceipt();
        receipt.setStudent(student);
        receipt.setAcademicSession(session);

        receiptRepository.save(receipt);
        responseRepository.save(r);
        return new Message("Thank you. Your answers were saved anonymously.");
    }

    public Message setSurveyWindow(String username, WindowRequest req) {
        User user = staff(username);
        if (!isHod(user)) throw bad("Only the H.O.D can open or close this form.");

        AcademicSession session = session(req.sessionId());
        ProjectSurveyWindow w = windowRepository.findByAcademicSessionId(session.getId()).orElseGet(ProjectSurveyWindow::new);
        w.setAcademicSession(session);
        w.setOpen(Boolean.TRUE.equals(req.open()));
        windowRepository.save(w);
        return new Message(w.isOpen() ? "The form is open to students." : "The form is closed.");
    }

    @Transactional(readOnly = true)
    public SurveyResults surveyResults(String username, Long sessionId) {
        User user = staff(username);
        if (!isHod(user)) throw bad("Only the H.O.D or an admin can see these results.");

        AcademicSession session = session(sessionId);
        boolean open = windowRepository.findByAcademicSessionId(session.getId()).map(ProjectSurveyWindow::isOpen).orElse(false);

        Map<Long, List<ProjectSurveyResponse>> bySupervisor = new LinkedHashMap<>();
        List<Concern> concerns = new ArrayList<>();
        List<String> impact = new ArrayList<>(), acknowledgements = new ArrayList<>(), comments = new ArrayList<>();

        for (ProjectSurveyResponse r : responseRepository.findForSession(session.getId())) {
            bySupervisor.computeIfAbsent(r.getSupervisor().getId(), k -> new ArrayList<>()).add(r);
            String who = r.getSupervisor().getFullName();

            if ("YES".equals(r.getMoneyCollected())) concerns.add(new Concern(who, "Money collected", r.getMoneyDetails()));
            if ("YES".equals(r.getInappropriate())) concerns.add(new Concern(who, "Inappropriate conduct", r.getInappropriateDetails()));
            if ("YES".equals(r.getGhostwritten())) concerns.add(new Concern(who, "Report written by others or pressure", r.getGhostwrittenDetails()));

            for (String s : new String[]{r.getImpact(), r.getProblemSolved()}) if (s != null && !s.isBlank()) impact.add(s);
            if (r.getAcknowledgedDetails() != null && !r.getAcknowledgedDetails().isBlank()) acknowledgements.add(r.getAcknowledgedDetails());
            if (r.getComment() != null && !r.getComment().isBlank()) comments.add(r.getComment());
        }

        List<SupervisorResult> supervisors = new ArrayList<>();
        for (List<ProjectSurveyResponse> list : bySupervisor.values()) {
            User sup = list.get(0).getSupervisor();
            boolean visible = list.size() >= SURVEY_MINIMUM;
            supervisors.add(new SupervisorResult(sup.getId(), sup.getFullName(), list.size(), visible,
                    visible ? avg(list.stream().mapToInt(ProjectSurveyResponse::getProfessional).average().orElse(0)) : null,
                    visible ? avg(list.stream().mapToInt(ProjectSurveyResponse::getHelpful).average().orElse(0)) : null,
                    visible ? avg(list.stream().mapToInt(ProjectSurveyResponse::getAvailable).average().orElse(0)) : null,
                    visible ? avg(list.stream().mapToInt(ProjectSurveyResponse::getOverall).average().orElse(0)) : null,
                    (int) list.stream().filter(x -> "YES".equals(x.getMoneyCollected())).count(),
                    (int) list.stream().filter(x -> "YES".equals(x.getInappropriate())).count(),
                    (int) list.stream().filter(x -> "YES".equals(x.getGhostwritten())).count()));
        }
        supervisors.sort(Comparator.comparing(SupervisorResult::name));

        // Shuffled, so the order of the answers says nothing about who wrote them.
        Collections.shuffle(concerns);
        Collections.shuffle(impact);
        Collections.shuffle(acknowledgements);
        Collections.shuffle(comments);

        return new SurveyResults(SURVEY_MINIMUM, open, supervisors, concerns, impact, acknowledgements, comments);
    }

    // ============================================================== summaries

    private Summary summaryOf(ProjectSupervision p) {
        List<ProjectStage> stages = stageRepository.findBySupervisionId(p.getId());
        List<ProjectMeeting> meetings = meetingRepository.findBySupervisionIdOrderByMeetingDateDesc(p.getId());
        List<ProjectPresentation> rounds = presentationRepository.findBySupervisionIdOrderByRoundNoAsc(p.getId());

        LocalDate today = LocalDate.now(LAGOS);
        long sinceAllocation = ChronoUnit.DAYS.between(p.getAssignedAt().toLocalDate(), today);
        int missed = (int) meetings.stream().filter(m -> !m.isAttended()).count();
        LocalDate last = meetings.isEmpty() ? null : meetings.get(0).getMeetingDate();

        List<String> alerts = new ArrayList<>();
        String severity = "NONE";

        if (missed >= MISSED_REPORT) {
            alerts.add("Missed " + missed + " supervision meetings. Consider reporting to the school authority: the student may not graduate.");
            severity = "HIGH";
        } else if (missed >= MISSED_WARN) {
            alerts.add("Missed " + missed + " supervision meetings.");
            severity = "MEDIUM";
        }

        if (meetings.isEmpty() && sinceAllocation > FIRST_MEETING_DAYS) {
            alerts.add("No meeting has been recorded in the " + sinceAllocation + " days since allocation.");
            severity = sinceAllocation > NO_PROGRESS_DAYS ? "HIGH" : maxSeverity(severity, "MEDIUM");
        } else if (last != null && ChronoUnit.DAYS.between(last, today) > MEETING_GAP_DAYS) {
            alerts.add("No meeting for " + ChronoUnit.DAYS.between(last, today) + " days.");
            severity = maxSeverity(severity, "MEDIUM");
        }

        boolean noProgress = stages.stream().noneMatch(s -> !s.getStage().equals("TOPIC") && !s.getStatus().equals("NOT_STARTED"));
        if (noProgress && sinceAllocation > NO_PROGRESS_DAYS) {
            alerts.add("No project progress has been recorded.");
            severity = maxSeverity(severity, "MEDIUM");
        }

        ProjectPresentation latest = rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);
        if (latest != null && latest.getOutcome().equals("REPRESENT")) {
            alerts.add("Told to re-present. A new presentation date is needed.");
            severity = maxSeverity(severity, "MEDIUM");
        }

        int done = (int) stages.stream().filter(s -> s.getStatus().equals("APPROVED")).count();
        Student s = p.getStudent();

        return new Summary(p.getId(), s.getFullName(), s.getMatricNumber(), s.getProgramme() != null ? s.getProgramme().getName() : "",
                classLabel(p.getClassification()), done, STAGES.size(), meetings.size(), missed, last == null ? null : last.toString(),
                latest == null ? "NONE" : latest.getOutcome(), severity, alerts);
    }

    private List<StageDto> stageDtos(List<ProjectStage> stages) {
        Map<String, ProjectStage> byKey = stages.stream().collect(Collectors.toMap(ProjectStage::getStage, x -> x, (a, b) -> a));
        List<StageDto> out = new ArrayList<>();
        for (String[] def : STAGES) {
            ProjectStage s = byKey.get(def[0]);
            out.add(new StageDto(def[0], def[1], s == null ? "NOT_STARTED" : s.getStatus(), s == null ? null : s.getNote(),
                    s == null || s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString()));
        }
        return out;
    }

    private List<MeetingDto> meetingDtos(List<ProjectMeeting> meetings) {
        return meetings.stream().map(m -> new MeetingDto(m.getId(), m.getMeetingDate().toString(), m.getMode(), m.isAttended(),
                m.getSummary(), m.getNextSteps(), m.getLoggedBy())).toList();
    }

    private List<PresentationDto> presentationDtos(List<ProjectPresentation> rounds) {
        List<PresentationDto> out = new ArrayList<>();
        for (ProjectPresentation r : rounds) {
            List<FeedbackDto> feedback = feedbackRepository.findByPresentationIdOrderByIdAsc(r.getId()).stream()
                    .map(f -> new FeedbackDto(f.getAssessor().getFullName(), f.getComments())).toList();
            out.add(new PresentationDto(r.getId(), r.getRoundNo(), r.getScheduledAt() == null ? null : r.getScheduledAt().toString(),
                    r.getVenue(), r.getOutcome(), r.getOutcomeNote(), feedback));
        }
        return out;
    }

    private AssessmentDto assessmentDto(ProjectAssessment a) {
        return new AssessmentDto(a.getProblemStatement(), a.getObjectives(), a.getPublishable(),
                a.getSimilarityPercent() == null ? null : a.getSimilarityPercent().doubleValue(),
                a.getAiPercent() == null ? null : a.getAiPercent().doubleValue(),
                a.getReportCheckedOn() == null ? null : a.getReportCheckedOn().toString(),
                a.getDiligence(), a.getCommunication(), a.getInitiative(), a.getComment());
    }

    // ================================================================ helpers

    private String yesNo(String v, String what) {
        String t = v == null ? "" : v.trim().toUpperCase();
        if (!t.equals("YES") && !t.equals("NO")) throw bad("Please answer " + what + ".");
        return t;
    }

    private String detail(String answer, String details, String ask) {
        String t = text(details, 500);
        if ("YES".equals(answer) && t == null) throw bad("Please say " + ask + ".");
        return t;
    }

    private int required(Integer v, String what) {
        if (v == null || v < 1 || v > 5) throw bad(what + " needs a score from 1 to 5.");
        return v;
    }

    private Integer rating(Integer v) {
        if (v == null) return null;
        if (v < 1 || v > 5) throw bad("Ratings are from 1 to 5.");
        return v;
    }

    private BigDecimal percent(Double v, String what) {
        if (v == null) return null;
        if (v < 0 || v > 100) throw bad(what + " must be between 0 and 100.");
        return BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String choice(String v, Set<String> allowed, String what) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim().toUpperCase();
        if (!allowed.contains(t)) throw bad("Invalid value for " + what + ".");
        return t;
    }

    private String text(String v, int max) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) throw bad("One of the texts is too long (" + max + " characters at most).");
        return t;
    }

    private double avg(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private int rank(String severity) {
        return severity.equals("HIGH") ? 0 : severity.equals("MEDIUM") ? 1 : 2;
    }

    private String maxSeverity(String a, String b) {
        return rank(a) <= rank(b) ? a : b;
    }

    private String classLabel(String c) {
        return "FIRST".equals(c) ? "First Class" : "UPPER".equals(c) ? "Second Class Upper" : "Other";
    }

    private ProjectSupervision supervision(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> bad("Project record not found."));
    }

    private ProjectSupervision latestSupervision(Student student) {
        List<ProjectSupervision> list = projectRepository.findForStudent(student.getId());
        return list.isEmpty() ? null : list.get(0);
    }

    private AcademicSession session(Long id) {
        if (id != null) return sessionRepository.findById(id).orElseThrow(() -> bad("Academic session not found."));
        AcademicSession current = clearanceService.currentSession();
        if (current == null) throw bad("No academic session has been set up yet.");
        return current;
    }

    private Student studentOf(String username) {
        Student s = userRepository.findByUsername(username).map(User::getStudent).orElse(null);
        if (s == null) throw bad("Your account is not linked to a student record.");
        return s;
    }

    private User staff(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!STAFF.contains(String.valueOf(user.getRole()).toUpperCase())) throw bad("You do not have permission to do this.");
        return user;
    }

    private boolean isHod(User user) {
        String role = String.valueOf(user.getRole()).toUpperCase();
        return role.equals("HOD") || role.equals("ADMIN");
    }

    private boolean isCoordinator(User user) {
        return profileRepository.findByUserId(user.getId()).map(x -> x.isProjectCoordinator() || x.isAssistantCoordinator()).orElse(false);
    }

    private void requireCoordinator(User user) {
        if (!isHod(user) && !isCoordinator(user)) throw bad("Only the H.O.D or a Project Coordinator can do this.");
    }

    /** A student's own supervisor manages their record; the H.O.D, admin and coordinators manage any. */
    private void authorize(User user, ProjectSupervision p) {
        if (p.getSupervisor().getId().equals(user.getId()) || isHod(user) || isCoordinator(user)) return;
        throw bad("This student is not one of your supervisees.");
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}