package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CreateOnlineClassRequest;
import com.kdu.csdtdas.backend.dto.OnlineClassResponse;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.OnlineClass;
import com.kdu.csdtdas.backend.entity.OnlineClassJoin;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.OnlineClassJoinRepository;
import com.kdu.csdtdas.backend.repository.OnlineClassRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OnlineClassService {

    private final OnlineClassRepository classRepository;
    private final OnlineClassJoinRepository joinRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final UserRepository userRepository;
    private final TeachingAccessService accessService;
    private final String videoBaseUrl;

    public OnlineClassService(
            OnlineClassRepository classRepository,
            OnlineClassJoinRepository joinRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository,
            UserRepository userRepository,
            TeachingAccessService accessService,
            @Value("${app.video.base-url:https://meet.jit.si}") String videoBaseUrl
    ) {
        this.classRepository = classRepository;
        this.joinRepository = joinRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.videoBaseUrl = videoBaseUrl.endsWith("/")
                ? videoBaseUrl.substring(0, videoBaseUrl.length() - 1) : videoBaseUrl;
    }

    public OnlineClassResponse create(String username, CreateOnlineClassRequest request) {
        User user = getUser(username);

        if (request.semester() == null || request.semester().isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("A class title is required.");
        }

        String semester = request.semester().trim().toUpperCase();
        accessService.requireTeachingRights(user, request.courseId(), request.academicSessionId(), semester);

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        AcademicSession session = academicSessionRepository.findById(request.academicSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        OnlineClass oc = new OnlineClass();
        oc.setCourse(course);
        oc.setAcademicSession(session);
        oc.setSemester(semester);
        oc.setLecturer(user);
        oc.setTitle(request.title().trim());
        oc.setScheduledAt(request.scheduledAt());
        oc.setStatus("SCHEDULED");
        oc.setRoomName(
                "csdtdas-" + course.getCode().toLowerCase().replaceAll("[^a-z0-9]", "")
                        + "-" + UUID.randomUUID().toString().substring(0, 8)
        );

        return OnlineClassResponse.from(classRepository.save(oc), 0);
    }

    @Transactional(readOnly = true)
    public List<OnlineClassResponse> listForCourse(String username, Long courseId, Long sessionId, String semester) {
        User user = getUser(username);
        String cleanSemester = semester.trim().toUpperCase();
        accessService.requireTeachingRights(user, courseId, sessionId, cleanSemester);

        return classRepository.findForCourse(courseId, sessionId, cleanSemester).stream()
                .map(oc -> OnlineClassResponse.from(oc, joinRepository.countByOnlineClassId(oc.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnlineClassResponse> listForStudent(String username) {
        User user = getUser(username);
        if (user.getStudent() == null) {
            throw new IllegalArgumentException("Your account is not linked to a student record.");
        }

        return classRepository.findForStudent(user.getStudent().getId()).stream()
                .map(oc -> OnlineClassResponse.from(oc, 0))
                .toList();
    }

    public OnlineClassResponse.JoinLink start(String username, Long id) {
        User user = getUser(username);
        OnlineClass oc = find(id);
        requireRights(user, oc);

        if ("ENDED".equals(oc.getStatus())) {
            throw new IllegalArgumentException("This class has already ended. Create a new class to teach again.");
        }

        oc.setStatus("LIVE");
        if (oc.getStartedAt() == null) oc.setStartedAt(LocalDateTime.now());
        classRepository.save(oc);

        return new OnlineClassResponse.JoinLink(buildJoinUrl(oc, user));
    }

    public void end(String username, Long id) {
        User user = getUser(username);
        OnlineClass oc = find(id);
        requireRights(user, oc);

        oc.setStatus("ENDED");
        oc.setEndedAt(LocalDateTime.now());
        classRepository.save(oc);
    }

    public OnlineClassResponse.JoinLink join(String username, Long id) {
        User user = getUser(username);
        OnlineClass oc = find(id);

        Long courseId = oc.getCourse().getId();
        Long sessionId = oc.getAcademicSession().getId();

        if ("ENDED".equals(oc.getStatus())) {
            throw new IllegalArgumentException("This class has ended.");
        }

        boolean teacher = accessService.canTeach(user, courseId, sessionId, oc.getSemester());

        if (!teacher) {
            if (!accessService.isRegistered(user, courseId, sessionId, oc.getSemester())) {
                throw new IllegalArgumentException("You are not registered for this course.");
            }
            if (!"LIVE".equals(oc.getStatus())) {
                throw new IllegalArgumentException("This class has not started yet. Please try again once your lecturer starts it.");
            }
            if (joinRepository.findByOnlineClassIdAndUserId(id, user.getId()).isEmpty()) {
                OnlineClassJoin join = new OnlineClassJoin();
                join.setOnlineClass(oc);
                join.setUser(user);
                joinRepository.save(join);
            }
        }

        return new OnlineClassResponse.JoinLink(buildJoinUrl(oc, user));
    }

    @Transactional(readOnly = true)
    public List<OnlineClassResponse.Participant> participants(String username, Long id) {
        User user = getUser(username);
        OnlineClass oc = find(id);
        requireRights(user, oc);

        return joinRepository.findByOnlineClassIdWithUsers(id).stream()
                .map(j -> new OnlineClassResponse.Participant(
                        j.getUser().getStudent() != null ? j.getUser().getStudent().getMatricNumber() : "—",
                        j.getUser().getFullName(),
                        j.getJoinedAt()
                ))
                .toList();
    }

    private void requireRights(User user, OnlineClass oc) {
        accessService.requireTeachingRights(
                user, oc.getCourse().getId(), oc.getAcademicSession().getId(), oc.getSemester()
        );
    }

    private String buildJoinUrl(OnlineClass oc, User user) {
        String displayName = URLEncoder
                .encode("\"" + user.getFullName() + "\"", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return videoBaseUrl + "/" + oc.getRoomName() + "#userInfo.displayName=" + displayName;
    }

    private OnlineClass find(Long id) {
        return classRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Online class not found."));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}