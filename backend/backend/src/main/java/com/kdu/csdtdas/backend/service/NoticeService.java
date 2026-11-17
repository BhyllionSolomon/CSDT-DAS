package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Announcement;
import com.kdu.csdtdas.backend.entity.AnnouncementRead;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AnnouncementReadRepository;
import com.kdu.csdtdas.backend.repository.AnnouncementRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class NoticeService {

    public record Notice(
            Long id, String title, String body, String audience, String audienceLabel, String priority, boolean pinned,
            String expiresOn, boolean expired, String postedBy, String postedByRole, String postedAt, boolean read, boolean mine
    ) {}

    public record Board(int unread, List<Notice> notices) {}

    public record Option(Long id, String name) {}

    public record Targets(List<String> audiences, List<Option> programmes, List<Option> courses,
                          Long fixedProgrammeId, Integer fixedLevel, boolean canPin) {}

    public record Request(String title, String body, String audience, Long programmeId, Integer levelNumber,
                          Long courseId, String priority, Boolean pinned, String expiresOn) {}

    public record Message(String message) {}

    private static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");
    private static final Set<String> HOD = Set.of("HOD", "ADMIN");
    private static final Set<String> TEACHING = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    /** Who is looking: what they are, and which courses they take or teach. */
    private static final class Viewer {
        User user;
        String role;
        boolean hod, student;
        Long programmeId;
        Integer levelNumber;
        Set<Long> courses = new HashSet<>();   // registered (students) or taught (staff) this session
    }

    @PersistenceContext
    private EntityManager em;

    private final AnnouncementRepository repository;
    private final AnnouncementReadRepository readRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final RegistrationClearanceService clearanceService;

    public NoticeService(
            AnnouncementRepository repository,
            AnnouncementReadRepository readRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            RegistrationClearanceService clearanceService
    ) {
        this.repository = repository;
        this.readRepository = readRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.clearanceService = clearanceService;
    }

    // ------------------------------------------------------------------ board

    @Transactional(readOnly = true)
    public Board board(String username) {
        Viewer v = viewer(user(username));
        Set<Long> read = new HashSet<>(readRepository.readIds(v.user.getId()));
        LocalDate today = LocalDate.now(LAGOS);

        int unread = 0;
        List<Notice> out = new ArrayList<>();

        for (Announcement a : repository.findActiveWithDetails()) {
            boolean mine = a.getPostedBy().getId().equals(v.user.getId());
            boolean expired = a.getExpiresOn() != null && a.getExpiresOn().isBefore(today);

            if (expired && !mine && !v.hod) continue;
            if (!visible(a, v, mine)) continue;

            boolean isRead = mine || read.contains(a.getId());
            if (!isRead && !expired) unread++;

            out.add(new Notice(
                    a.getId(), a.getTitle(), a.getBody(), a.getAudience(), label(a), a.getPriority(), a.isPinned(),
                    a.getExpiresOn() == null ? null : a.getExpiresOn().toString(), expired,
                    a.getPostedBy().getFullName(), String.valueOf(a.getPostedBy().getRole()),
                    a.getCreatedAt().toString(), isRead, mine));
        }
        return new Board(unread, out);
    }

    public Message markRead(String username, Long id) {
        User u = user(username);
        Announcement a = repository.findById(id).orElseThrow(() -> bad("Notice not found."));
        if (!readRepository.existsByAnnouncementIdAndUserId(id, u.getId())) {
            AnnouncementRead r = new AnnouncementRead();
            r.setAnnouncement(a);
            r.setUser(u);
            readRepository.save(r);
        }
        return new Message("Marked as read.");
    }

    // --------------------------------------------------------------- posting

    @Transactional(readOnly = true)
    public Targets targets(String username) {
        Viewer v = viewer(user(username));
        if (v.student || !(v.hod || TEACHING.contains(v.role))) throw bad("You cannot post notices.");

        List<Option> programmes = new ArrayList<>();
        for (Programme p : em.createQuery("SELECT p FROM Programme p ORDER BY p.name", Programme.class).getResultList()) {
            programmes.add(new Option(p.getId(), p.getName()));
        }

        List<Option> courses = courseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> v.hod || v.courses.contains(c.getId()))
                .sorted(Comparator.comparing(Course::getCode))
                .map(c -> new Option(c.getId(), c.getCode() + " · " + c.getTitle()))
                .toList();

        List<String> audiences = new ArrayList<>();
        Long fixedProgramme = null;
        Integer fixedLevel = null;

        if (v.hod) {
            audiences.addAll(List.of("ALL", "STAFF", "STUDENTS", "PROGRAMME_LEVEL", "COURSE"));
        } else {
            if (v.role.equals("LEVEL_ADVISER") && v.user.getProgramme() != null) {
                audiences.add("PROGRAMME_LEVEL");
                fixedProgramme = v.user.getProgramme().getId();
                fixedLevel = v.user.getLevel() != null ? v.user.getLevel().getLevelNumber() : null;
            }
            if (!courses.isEmpty()) audiences.add("COURSE");
        }
        if (audiences.isEmpty()) throw bad("You have no group to post to yet.");

        return new Targets(audiences, programmes, courses, fixedProgramme, fixedLevel, v.hod);
    }

    public Message create(String username, Request req) {
        Viewer v = viewer(user(username));
        if (v.student || !(v.hod || TEACHING.contains(v.role))) throw bad("You cannot post notices.");

        String title = req.title() == null ? "" : req.title().trim();
        String body = req.body() == null ? "" : req.body().trim();
        if (title.isEmpty() || title.length() > 150) throw bad("Give the notice a title of up to 150 characters.");
        if (body.isEmpty() || body.length() > 4000) throw bad("Write the notice, up to 4000 characters.");

        String audience = req.audience() == null ? "" : req.audience().trim().toUpperCase();
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setBody(body);
        a.setAudience(audience);
        a.setPostedBy(v.user);
        a.setPriority("URGENT".equalsIgnoreCase(req.priority()) ? "URGENT" : "NORMAL");
        a.setPinned(v.hod && Boolean.TRUE.equals(req.pinned()));

        switch (audience) {
            case "ALL", "STAFF", "STUDENTS" -> {
                if (!v.hod) throw bad("Only the H.O.D can post to this group.");
            }
            case "PROGRAMME_LEVEL" -> {
                Long programmeId = req.programmeId();
                Integer level = req.levelNumber();
                if (!v.hod) {
                    if (!v.role.equals("LEVEL_ADVISER") || v.user.getProgramme() == null) throw bad("You cannot post to a programme.");
                    programmeId = v.user.getProgramme().getId();                       // advisers are fixed to their own group
                    level = v.user.getLevel() != null ? v.user.getLevel().getLevelNumber() : level;
                }
                Programme programme = programmeId == null ? null : em.find(Programme.class, programmeId);
                if (programme == null) throw bad("Choose a programme.");
                if (level != null && (level < 100 || level > 400 || level % 100 != 0)) throw bad("Choose a level from 100 to 400.");
                a.setProgramme(programme);
                a.setLevelNumber(level);
            }
            case "COURSE" -> {
                Course course = req.courseId() == null ? null : courseRepository.findById(req.courseId()).orElse(null);
                if (course == null) throw bad("Choose a course.");
                if (!v.hod && !v.courses.contains(course.getId())) throw bad("You can only post to courses you teach.");
                a.setCourse(course);
            }
            default -> throw bad("Choose who the notice is for.");
        }

        if (req.expiresOn() != null && !req.expiresOn().isBlank()) {
            try {
                LocalDate end = LocalDate.parse(req.expiresOn());
                if (end.isBefore(LocalDate.now(LAGOS))) throw bad("The end date cannot be in the past.");
                a.setExpiresOn(end);
            } catch (DateTimeParseException e) {
                throw bad("Enter the end date properly.");
            }
        }

        repository.save(a);
        return new Message("Notice posted.");
    }

    public Message delete(String username, Long id) {
        Viewer v = viewer(user(username));
        Announcement a = repository.findById(id).orElseThrow(() -> bad("Notice not found."));
        if (!v.hod && !a.getPostedBy().getId().equals(v.user.getId())) throw bad("You can only delete your own notices.");
        a.setActive(false);
        repository.save(a);
        return new Message("Notice deleted.");
    }

    // ---------------------------------------------------------------- helpers

    private boolean visible(Announcement a, Viewer v, boolean mine) {
        if (mine || v.hod) return true;

        return switch (a.getAudience()) {
            case "ALL" -> true;
            case "STAFF" -> !v.student;
            case "STUDENTS" -> v.student;
            case "PROGRAMME_LEVEL" -> v.student && a.getProgramme() != null && a.getProgramme().getId().equals(v.programmeId)
                    && (a.getLevelNumber() == null || a.getLevelNumber().equals(v.levelNumber));
            case "COURSE" -> a.getCourse() != null && v.courses.contains(a.getCourse().getId());
            default -> false;
        };
    }

    private String label(Announcement a) {
        return switch (a.getAudience()) {
            case "ALL" -> "Everyone";
            case "STAFF" -> "All staff";
            case "STUDENTS" -> "All students";
            case "PROGRAMME_LEVEL" -> (a.getProgramme() != null ? a.getProgramme().getName() : "Programme")
                    + (a.getLevelNumber() != null ? " " + a.getLevelNumber() + " Level" : "") + " students";
            case "COURSE" -> (a.getCourse() != null ? a.getCourse().getCode() : "Course") + " students";
            default -> a.getAudience();
        };
    }

    private Viewer viewer(User u) {
        Viewer v = new Viewer();
        v.user = u;
        v.role = String.valueOf(u.getRole()).toUpperCase();
        v.hod = HOD.contains(v.role);
        v.student = v.role.equals("STUDENT");

        AcademicSession session = clearanceService.currentSession();

        if (v.student) {
            Student s = u.getStudent();
            if (s != null) {
                v.programmeId = s.getProgramme() != null ? s.getProgramme().getId() : null;
                v.levelNumber = s.getLevel() != null ? s.getLevel().getLevelNumber() : null;
                if (session != null) {
                    v.courses = registrationRepository.findByStudentIdAndAcademicSessionId(s.getId(), session.getId()).stream()
                            .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                            .map(r -> r.getCourse().getId())
                            .collect(Collectors.toSet());
                }
            }
        } else if (TEACHING.contains(v.role) && session != null) {
            for (String sem : List.of("FIRST", "SECOND")) {
                for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(session.getId(), sem)) {
                    if (a.getLecturer() != null && a.getLecturer().getId().equals(u.getId())) v.courses.add(a.getCourse().getId());
                }
            }
        }
        return v;
    }

    private User user(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}