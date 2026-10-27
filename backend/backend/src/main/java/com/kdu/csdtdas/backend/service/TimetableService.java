package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.TimetableDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.TimetableEntry;
import com.kdu.csdtdas.backend.entity.TimetableRoom;
import com.kdu.csdtdas.backend.entity.TimetableWindow;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.TimetableEntryRepository;
import com.kdu.csdtdas.backend.repository.TimetableRoomRepository;
import com.kdu.csdtdas.backend.repository.TimetableWindowRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimetableService {

    private static final List<String> DAYS = List.of("MON", "TUE", "WED", "THU", "FRI");
    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END = LocalTime.of(19, 0);
    private static final LocalTime BREAK_START = LocalTime.of(13, 0);
    private static final LocalTime BREAK_END = LocalTime.of(14, 0);

    private final TimetableWindowRepository windowRepository;
    private final TimetableRoomRepository roomRepository;
    private final TimetableEntryRepository entryRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final TeachingAccessService accessService;

    public TimetableService(
            TimetableWindowRepository windowRepository,
            TimetableRoomRepository roomRepository,
            TimetableEntryRepository entryRepository,
            AcademicSessionRepository sessionRepository,
            CourseRepository courseRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            UserRepository userRepository,
            TeachingAccessService accessService
    ) {
        this.windowRepository = windowRepository;
        this.roomRepository = roomRepository;
        this.entryRepository = entryRepository;
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    private record Plan(Map<Long, Course> courses, Map<Long, List<String>> lecturerNames, Map<Long, User> firstLecturer) {}

    // ------------------------------------------------------------------ rooms

    public List<TimetableDTOs.RoomDto> rooms() {
        seedRooms();
        return roomRepository.findAllByOrderByNameAsc().stream().map(this::roomDto).toList();
    }

    public TimetableDTOs.RoomDto addRoom(String username, String name, String type) {
        requireHod(username);

        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ").toUpperCase();
        if (clean.isEmpty() || clean.length() > 40) throw bad("Enter a room name (up to 40 characters).");

        String kind = "LAB".equalsIgnoreCase(type) ? "LAB" : "LECTURE";
        if (roomRepository.findByNameIgnoreCase(clean).isPresent()) throw bad(clean + " is already on the list.");

        return roomDto(saveRoom(clean, kind));
    }

    public TimetableDTOs.RoomDto setRoomActive(String username, Long id, boolean active) {
        requireHod(username);
        TimetableRoom room = roomRepository.findById(id).orElseThrow(() -> bad("Room not found."));
        room.setActive(active);
        return roomDto(roomRepository.save(room));
    }

    private void seedRooms() {
        if (roomRepository.count() > 0) return;
        for (String name : List.of("RM 01", "RM 02", "RM 79", "RM 80", "RM 82", "NEW HALL", "AUDITORIUM")) saveRoom(name, "LECTURE");
        for (String name : List.of("COMP LAB", "CYB LAB", "PHE LAB")) saveRoom(name, "LAB");
    }

    private TimetableRoom saveRoom(String name, String type) {
        TimetableRoom r = new TimetableRoom();
        r.setName(name);
        r.setType(type);
        return roomRepository.save(r);
    }

    // ---------------------------------------------------------------- windows

    public TimetableDTOs.WindowInfo open(String username, Long sessionId, String semester, String deadline) {
        requireHod(username);
        String sem = clean(semester);

        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));
        if (findWindow(sessionId, sem).isPresent()) {
            throw bad("A timetable window already exists for this session and semester.");
        }

        seedRooms();

        TimetableWindow w = new TimetableWindow();
        w.setAcademicSession(session);
        w.setSemester(sem);
        w.setStatus("OPEN");
        w.setDeadline(parseDeadline(deadline));
        w.setOpenedAt(LocalDateTime.now());
        w.setOpenedBy(username);

        return info(windowRepository.save(w));
    }

    public TimetableDTOs.WindowInfo updateDeadline(String username, Long windowId, String deadline) {
        requireHod(username);
        TimetableWindow w = window(windowId);
        w.setDeadline(parseDeadline(deadline));
        return info(windowRepository.save(w));
    }

    /** Lets lecturers choose slots again (for example after the deadline, or after publishing). */
    public TimetableDTOs.WindowInfo reopen(String username, Long windowId, String deadline) {
        requireHod(username);
        TimetableWindow w = window(windowId);
        w.setStatus("OPEN");
        w.setDeadline(parseDeadline(deadline));
        w.setPublishedAt(null);
        w.setPublishedBy(null);
        return info(windowRepository.save(w));
    }

    public TimetableDTOs.AdminStatus adminStatus(String username, Long sessionId, String semester) {
        requireHod(username);
        String sem = clean(semester);

        TimetableWindow w = findWindow(sessionId, sem).orElse(null);
        if (w == null) {
            return new TimetableDTOs.AdminStatus(infoFor(sessionId, sem, null), 0, 0, List.of(), List.of());
        }

        enforceIfDue(w);

        List<TimetableEntry> entries = entryRepository.findForWindow(w.getId());
        Plan plan = plan(w);
        Map<Long, Long> counts = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        List<TimetableDTOs.CourseState> states = new ArrayList<>();
        int responded = 0;

        for (Course c : plan.courses().values()) {
            int sessions = counts.getOrDefault(c.getId(), 0L).intValue();
            if (sessions > 0) responded++;
            states.add(new TimetableDTOs.CourseState(
                    c.getId(), c.getCode(), c.getTitle(), unitOf(c), c.getLevel().getName(),
                    plan.lecturerNames().getOrDefault(c.getId(), List.of()), sessions, sessions > 0
            ));
        }
        states.sort(Comparator.comparing(TimetableDTOs.CourseState::code));

        return new TimetableDTOs.AdminStatus(
                info(w), states.size(), responded, states, entries.stream().map(this::dto).toList()
        );
    }

    // ---------------------------------------------------------- auto placement

    public TimetableDTOs.AutoFillResult autoFill(String username, Long windowId) {
        requireHod(username);
        return runAutoFill(window(windowId));
    }

    /** Called every hour, and whenever someone opens a timetable, so a missed deadline never waits. */
    public void enforceDeadlines() {
        for (TimetableWindow w : windowRepository.findByStatus("OPEN")) enforceIfDue(w);
    }

    private void enforceIfDue(TimetableWindow w) {
        if ("OPEN".equals(w.getStatus()) && w.getDeadline().isBefore(LocalDateTime.now())) {
            runAutoFill(w);
        }
    }

    private TimetableDTOs.AutoFillResult runAutoFill(TimetableWindow w) {

        List<TimetableEntry> entries = new ArrayList<>(entryRepository.findForWindow(w.getId()));
        List<TimetableRoom> lectureRooms = roomRepository.findAllByOrderByNameAsc().stream()
                .filter(r -> r.isActive() && "LECTURE".equals(r.getType()))
                .toList();

        Plan plan = plan(w);
        Set<Long> scheduled = entries.stream().map(e -> e.getCourse().getId()).collect(Collectors.toSet());

        List<Course> need = plan.courses().values().stream()
                .filter(c -> !scheduled.contains(c.getId()))
                .sorted(Comparator.comparingInt(this::unitOf).reversed().thenComparing(Course::getCode))
                .toList();

        int offset = 0, coursesPlaced = 0, sessionsAdded = 0;
        List<String> unplaced = new ArrayList<>();

        for (Course c : need) {
            User lecturer = plan.firstLecturer().get(c.getId());
            Set<String> usedDays = new HashSet<>();
            boolean complete = true;
            int placed = 0;

            for (int hours : blocksFor(unitOf(c))) {
                TimetableEntry e = place(w, c, lecturer, hours, usedDays, entries, lectureRooms, offset++);
                if (e == null) {
                    complete = false;
                    continue;
                }
                entries.add(e);
                usedDays.add(e.getDay());
                placed++;
                sessionsAdded++;
            }

            if (placed > 0) coursesPlaced++;
            if (!complete) unplaced.add(c.getCode() + (placed > 0 ? " (only partly placed)" : " (no free slot found)"));
        }

        w.setAutoFilledAt(LocalDateTime.now());
        if ("OPEN".equals(w.getStatus())) w.setStatus("CLOSED");
        windowRepository.save(w);

        return new TimetableDTOs.AutoFillResult(coursesPlaced, sessionsAdded, unplaced);
    }

    private TimetableEntry place(
            TimetableWindow w, Course course, User lecturer, int hours, Set<String> usedDays,
            List<TimetableEntry> entries, List<TimetableRoom> rooms, int offset
    ) {
        for (int pass = 0; pass < 2; pass++) {
            for (int d = 0; d < DAYS.size(); d++) {
                String day = DAYS.get((d + offset) % DAYS.size());
                if (pass == 0 && usedDays.contains(day)) continue;

                for (int hour = 8; hour + hours <= 19; hour++) {
                    LocalTime start = LocalTime.of(hour, 0);
                    LocalTime end = start.plusHours(hours);
                    if (touchesBreak(start, end)) continue;

                    for (TimetableRoom room : rooms) {
                        if (conflictFor(entries, course, lecturer, room, day, start, end, null) != null) continue;

                        TimetableEntry e = new TimetableEntry();
                        e.setWindow(w);
                        e.setCourse(course);
                        e.setLecturer(lecturer);
                        e.setKind("LECTURE");
                        e.setDay(day);
                        e.setStartTime(start);
                        e.setEndTime(end);
                        e.setRoom(room);
                        e.setSource("AUTO");
                        return entryRepository.save(e);
                    }
                }
            }
        }
        return null;
    }

    /** One unit is one hour a week, in blocks of at most two hours. */
    private List<Integer> blocksFor(int unit) {
        int left = Math.max(1, Math.min(unit, 6));
        List<Integer> blocks = new ArrayList<>();
        while (left > 0) {
            int h = Math.min(2, left);
            blocks.add(h);
            left -= h;
        }
        return blocks;
    }

    // ---------------------------------------------------------------- publish

    public TimetableDTOs.Message publish(String username, Long windowId, boolean force) {
        requireHod(username);
        TimetableWindow w = window(windowId);

        Set<Long> scheduled = entryRepository.findForWindow(w.getId()).stream()
                .map(e -> e.getCourse().getId()).collect(Collectors.toSet());

        List<String> missing = plan(w).courses().values().stream()
                .filter(c -> !scheduled.contains(c.getId()))
                .map(Course::getCode)
                .sorted()
                .toList();

        if (!missing.isEmpty() && !force) {
            throw bad(missing.size() + " course(s) have no slot yet: " + String.join(", ", missing)
                    + ". Press Fill missing now first, or publish anyway.");
        }

        w.setStatus("PUBLISHED");
        w.setPublishedAt(LocalDateTime.now());
        w.setPublishedBy(username);
        windowRepository.save(w);

        return new TimetableDTOs.Message("The timetable is published. Students and lecturers can now see it.");
    }

    // ------------------------------------------------------------------ views

    /** The whole department's timetable. The H.O.D sees it any time; others only once it is published. */
    public TimetableDTOs.StudentView view(String username, Long sessionId, String semester) {
        User user = getUser(username);
        String sem = clean(semester);

        TimetableWindow w = findWindow(sessionId, sem).orElse(null);
        if (w == null) return new TimetableDTOs.StudentView(infoFor(sessionId, sem, null), false, List.of());

        enforceIfDue(w);

        boolean published = "PUBLISHED".equals(w.getStatus());
        if (!published && !isHod(user)) return new TimetableDTOs.StudentView(info(w), false, List.of());

        return new TimetableDTOs.StudentView(
                info(w), published, entryRepository.findForWindow(w.getId()).stream().map(this::dto).toList()
        );
    }

    /** A student's own timetable: the courses they registered, or their programme and level if none yet. */
    public TimetableDTOs.StudentView studentView(String username, Long sessionId, String semester) {
        User user = getUser(username);
        Student student = user.getStudent();
        if (student == null) throw bad("Your account is not linked to a student record.");

        String sem = clean(semester);
        TimetableWindow w = findWindow(sessionId, sem).orElse(null);

        if (w == null || !"PUBLISHED".equals(w.getStatus())) {
            return new TimetableDTOs.StudentView(infoFor(sessionId, sem, w), false, List.of());
        }

        Set<Long> registered = registrationRepository
                .findByStudentIdAndAcademicSessionId(student.getId(), sessionId).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()) && sem.equalsIgnoreCase(r.getSemester()))
                .map(r -> r.getCourse().getId())
                .collect(Collectors.toSet());

        List<TimetableDTOs.EntryDto> mine = entryRepository.findForWindow(w.getId()).stream()
                .filter(e -> {
                    if (!registered.isEmpty()) return registered.contains(e.getCourse().getId());

                    Course c = e.getCourse();
                    return student.getLevel() != null && student.getProgramme() != null
                            && c.getLevel().getId().equals(student.getLevel().getId())
                            && c.getProgrammes().stream().anyMatch(p -> p.getId().equals(student.getProgramme().getId()));
                })
                .map(this::dto)
                .toList();

        return new TimetableDTOs.StudentView(info(w), true, mine);
    }

    /** A lecturer's claimed courses with the sessions already chosen for them. */
    public TimetableDTOs.MineView mine(String username, Long sessionId, String semester) {
        User user = getUser(username);
        String sem = clean(semester);

        TimetableWindow w = findWindow(sessionId, sem).orElse(null);
        if (w != null) enforceIfDue(w);

        Map<Long, List<TimetableEntry>> byCourse = w == null
                ? Map.of()
                : entryRepository.findForWindow(w.getId()).stream()
                  .collect(Collectors.groupingBy(e -> e.getCourse().getId()));

        List<TimetableDTOs.MineCourse> courses = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(sessionId, sem)) {
            if (a.getLecturer() == null || !a.getLecturer().getId().equals(user.getId())) continue;

            Course c = a.getCourse();
            if (!seen.add(c.getId())) continue;

            courses.add(new TimetableDTOs.MineCourse(
                    c.getId(), c.getCode(), c.getTitle(), unitOf(c), c.getLevel().getName(),
                    byCourse.getOrDefault(c.getId(), List.of()).stream().map(this::dto).toList()
            ));
        }

        return new TimetableDTOs.MineView(infoFor(sessionId, sem, w), w != null && "OPEN".equals(w.getStatus()), courses);
    }

    // ---------------------------------------------------------------- entries

    public TimetableDTOs.EntryDto addEntry(String username, TimetableDTOs.EntryRequest req) {
        User user = getUser(username);
        String sem = clean(req.semester());
        boolean hod = isHod(user);

        TimetableWindow w = findWindow(req.sessionId(), sem)
                .orElseThrow(() -> bad("The H.O.D has not opened the timetable for this session and semester yet."));
        enforceIfDue(w);

        if (!hod && !"OPEN".equals(w.getStatus())) {
            throw bad("The timetable window is closed. Please contact the H.O.D.");
        }

        Course course = courseRepository.findById(req.courseId()).orElseThrow(() -> bad("Course not found."));

        if (!accessService.canTeach(user, course.getId(), req.sessionId(), sem)) {
            throw bad("You are not assigned to teach this course for this session and semester.");
        }
        if (allocationRepository.findForCourse(course.getId(), req.sessionId(), sem).isEmpty()) {
            throw bad("This course is not on the course list for this semester.");
        }

        String kind = "PRACTICAL".equalsIgnoreCase(req.kind()) ? "PRACTICAL" : "LECTURE";
        String day = req.day() == null ? "" : req.day().trim().toUpperCase();
        if (!DAYS.contains(day)) throw bad("Choose a day from Monday to Friday.");

        LocalTime start = time(req.startTime());
        LocalTime end = time(req.endTime());
        if (start.isBefore(DAY_START) || end.isAfter(DAY_END)) throw bad("Classes run between 8:00 and 19:00.");
        if (!start.isBefore(end)) throw bad("The end time must be after the start time.");
        if (touchesBreak(start, end)) throw bad("1:00 pm to 2:00 pm is the break. Choose another time.");
        if (end.isAfter(start.plusHours(4))) throw bad("A single session cannot be longer than four hours.");

        TimetableRoom room = roomRepository.findById(req.roomId() == null ? -1L : req.roomId())
                .orElseThrow(() -> bad("Choose a room."));
        if (!room.isActive()) throw bad(room.getName() + " is not available.");
        if ("PRACTICAL".equals(kind) && !"LAB".equals(room.getType())) throw bad("Practicals must be held in a lab.");

        User lecturer = hod
                ? (req.lecturerId() == null ? null : userRepository.findById(req.lecturerId()).orElse(null))
                : user;

        List<TimetableEntry> existing = entryRepository.findForWindow(w.getId());
        String problem = conflictFor(existing, course, lecturer, room, day, start, end, null);
        if (problem != null) throw bad(problem);

        TimetableEntry e = new TimetableEntry();
        e.setWindow(w);
        e.setCourse(course);
        e.setLecturer(lecturer);
        e.setKind(kind);
        e.setDay(day);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setRoom(room);
        e.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        e.setSource(hod ? "HOD" : "LECTURER");

        return dto(entryRepository.save(e));
    }

    public void deleteEntry(String username, Long id) {
        User user = getUser(username);
        TimetableEntry e = entryRepository.findByIdWithDetails(id).orElseThrow(() -> bad("Session not found."));
        TimetableWindow w = e.getWindow();

        if (!isHod(user)) {
            if (!"OPEN".equals(w.getStatus())) throw bad("The timetable window is closed. Please contact the H.O.D.");
            if (!accessService.canTeach(user, e.getCourse().getId(), w.getAcademicSession().getId(), w.getSemester())) {
                throw bad("You are not assigned to this course.");
            }
        }

        entryRepository.delete(e);
    }

    // ----------------------------------------------------------------- checks

    /** Returns why this slot cannot be used, or null when it is free. */
    private String conflictFor(
            List<TimetableEntry> entries, Course course, User lecturer, TimetableRoom room,
            String day, LocalTime start, LocalTime end, Long ignoreId
    ) {
        Set<Long> myProgrammes = course.getProgrammes().stream().map(Programme::getId).collect(Collectors.toSet());

        for (TimetableEntry e : entries) {
            if (ignoreId != null && ignoreId.equals(e.getId())) continue;
            if (!e.getDay().equals(day) || !overlaps(e.getStartTime(), e.getEndTime(), start, end)) continue;

            String when = day + " " + e.getStartTime() + "–" + e.getEndTime();

            if (e.getRoom().getId().equals(room.getId())) {
                return room.getName() + " is already used on " + when + " by " + e.getCourse().getCode() + ".";
            }

            if (lecturer != null && e.getLecturer() != null && e.getLecturer().getId().equals(lecturer.getId())) {
                return "The lecturer already teaches " + e.getCourse().getCode() + " on " + when + ".";
            }

            Course other = e.getCourse();
            boolean same = other.getId().equals(course.getId());
            boolean sameStudents = other.getLevel().getId().equals(course.getLevel().getId())
                    && other.getProgrammes().stream().anyMatch(p -> myProgrammes.contains(p.getId()));

            if (same || sameStudents) {
                return "It clashes with " + other.getCode() + " on " + when + ", which the same students take.";
            }
        }
        return null;
    }

    private boolean overlaps(LocalTime a1, LocalTime a2, LocalTime b1, LocalTime b2) {
        return a1.isBefore(b2) && b1.isBefore(a2);
    }

    private boolean touchesBreak(LocalTime start, LocalTime end) {
        return start.isBefore(BREAK_END) && end.isAfter(BREAK_START);
    }

    // ---------------------------------------------------------------- helpers

    /** The courses on this semester's list, with their lecturers. */
    private Plan plan(TimetableWindow w) {
        Map<Long, Course> courses = new LinkedHashMap<>();
        Map<Long, List<String>> names = new HashMap<>();
        Map<Long, User> first = new HashMap<>();

        for (CourseAllocation a : allocationRepository
                .findByAcademicSessionIdAndSemester(w.getAcademicSession().getId(), w.getSemester())) {

            Long id = a.getCourse().getId();
            courses.putIfAbsent(id, a.getCourse());

            String name = a.getLecturer() != null ? a.getLecturer().getFullName() : a.getLecturerName();
            if (name != null && !name.isBlank()) {
                List<String> list = names.computeIfAbsent(id, k -> new ArrayList<>());
                if (!list.contains(name)) list.add(name);
            }
            if (a.getLecturer() != null) first.putIfAbsent(id, a.getLecturer());
        }
        return new Plan(courses, names, first);
    }

    private TimetableDTOs.EntryDto dto(TimetableEntry e) {
        return new TimetableDTOs.EntryDto(
                e.getId(), e.getCourse().getId(), e.getCourse().getCode(), e.getCourse().getTitle(),
                e.getCourse().getLevel().getName(), e.getKind(), e.getDay(),
                e.getStartTime().toString(), e.getEndTime().toString(),
                e.getRoom().getId(), e.getRoom().getName(), e.getRoom().getType(),
                e.getLecturer() != null ? e.getLecturer().getId() : null,
                e.getLecturer() != null ? e.getLecturer().getFullName() : null,
                e.getNote(), e.getSource()
        );
    }

    private TimetableDTOs.RoomDto roomDto(TimetableRoom r) {
        return new TimetableDTOs.RoomDto(r.getId(), r.getName(), r.getType(), r.isActive());
    }

    private TimetableDTOs.WindowInfo info(TimetableWindow w) {
        return new TimetableDTOs.WindowInfo(
                w.getId(), w.getAcademicSession().getId(), w.getAcademicSession().getName(), w.getSemester(),
                w.getStatus(), w.getDeadline(), w.getPublishedAt()
        );
    }

    private TimetableDTOs.WindowInfo infoFor(Long sessionId, String semester, TimetableWindow w) {
        if (w != null) return info(w);
        String name = sessionRepository.findById(sessionId).map(AcademicSession::getName).orElse("");
        return new TimetableDTOs.WindowInfo(null, sessionId, name, semester, "NONE", null, null);
    }

    private Optional<TimetableWindow> findWindow(Long sessionId, String semester) {
        return windowRepository.findByAcademicSessionIdAndSemester(sessionId, semester);
    }

    private TimetableWindow window(Long id) {
        return windowRepository.findById(id).orElseThrow(() -> bad("Timetable window not found."));
    }

    private int unitOf(Course c) {
        return c.getCreditUnit() == null ? 1 : c.getCreditUnit();
    }

    private LocalDateTime parseDeadline(String raw) {
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Enter the deadline as a date.");
        }
        if (date.isBefore(LocalDate.now())) throw bad("The deadline cannot be in the past.");
        return date.atTime(23, 59);
    }

    private LocalTime time(String raw) {
        LocalTime t;
        try {
            t = LocalTime.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Enter the time as HH:mm, for example 08:00.");
        }
        if (t.getMinute() % 30 != 0 || t.getSecond() != 0) throw bad("Times must be on the hour or the half hour.");
        return t;
    }

    private String clean(String semester) {
        String s = semester == null ? "" : semester.trim().toUpperCase();
        if (!s.equals("FIRST") && !s.equals("SECOND")) throw bad("Semester must be FIRST or SECOND.");
        return s;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
    }

    private boolean isHod(User user) {
        return "HOD".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private void requireHod(String username) {
        if (!isHod(getUser(username))) throw bad("Only the H.O.D can do this.");
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}