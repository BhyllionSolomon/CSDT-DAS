package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.RegistrationStatusRecord;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final RegistrationRulesService rulesService;

    public CourseRegistrationService(
            CourseRegistrationRepository courseRegistrationRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository,
            RegistrationRulesService rulesService
    ) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.rulesService = rulesService;
    }

    public CourseRegistration registerCourse(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester
    ) {

        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        AcademicSession academicSession = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        if (!student.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new IllegalArgumentException("Student is not active and cannot register courses.");
        }

        if (!course.getActive()) {
            throw new IllegalArgumentException("Course is not active and cannot be registered.");
        }

        if (!academicSession.getActive()) {
            throw new IllegalArgumentException("Academic session is not active.");
        }

        boolean alreadyRegistered = courseRegistrationRepository
                .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                        studentId, courseId, academicSessionId, semester
                );

        if (alreadyRegistered) {
            throw new IllegalArgumentException("Student has already registered this course for this semester.");
        }

        if (!course.getSemester().equalsIgnoreCase(semester)) {
            throw new IllegalArgumentException("This course is not offered in the selected semester.");
        }

        if (!course.getDepartment().getId().equals(student.getDepartment().getId())) {
            throw new IllegalArgumentException("This course does not belong to the student's department.");
        }

        if (!course.getLevel().getId().equals(student.getLevel().getId())) {
            throw new IllegalArgumentException("This course is not assigned to the student's level.");
        }

        CourseRegistration registration = new CourseRegistration();

        registration.setStudent(student);
        registration.setCourse(course);
        registration.setAcademicSession(academicSession);
        registration.setSemester(semester.toUpperCase());
        registration.setStatus("REGISTERED");

        return courseRegistrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<CourseRegistration> getStudentRegistrations(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found.");
        }

        return courseRegistrationRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<CourseRegistration> getStudentRegistrationsForSession(
            Long studentId,
            Long academicSessionId
    ) {

        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found.");
        }

        if (!academicSessionRepository.existsById(academicSessionId)) {
            throw new IllegalArgumentException("Academic session not found.");
        }

        return courseRegistrationRepository
                .findByStudentIdAndAcademicSessionId(studentId, academicSessionId);
    }

    public void cancelRegistration(Long registrationId) {

        CourseRegistration registration = courseRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Course registration not found."));

        registration.setStatus("CANCELLED");

        courseRegistrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<CourseRegistration> getRegistrationsForCourse(
            Long courseId,
            Long academicSessionId,
            String semester
    ) {

        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found.");
        }

        if (!academicSessionRepository.existsById(academicSessionId)) {
            throw new IllegalArgumentException("Academic session not found.");
        }

        return courseRegistrationRepository
                .findByCourseIdAndAcademicSessionIdAndSemester(
                        courseId, academicSessionId, semester.trim().toUpperCase()
                );
    }

    /** Every rule (carry-overs, level, units, direct-entry backlog) is checked by RegistrationRulesService. */
    public com.kdu.csdtdas.backend.dto.BulkRegistrationResult registerMultiple(
            Long studentId,
            Long academicSessionId,
            String semester,
            List<Long> courseIds
    ) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one course to register.");
        }
        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        List<Long> ids = courseIds.stream().distinct().toList();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        String cleanSemester = semester.trim().toUpperCase();

        RegistrationRulesService.Checked checked = rulesService.validate(student, session, cleanSemester, ids);

        int registered = 0;
        int skipped = 0;

        for (Long courseId : ids) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

            boolean already = courseRegistrationRepository
                    .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                            studentId, course.getId(), academicSessionId, cleanSemester
                    );
            if (already) { skipped++; continue; }

            CourseRegistration reg = new CourseRegistration();
            reg.setStudent(student);
            reg.setCourse(course);
            reg.setAcademicSession(session);
            reg.setSemester(cleanSemester);
            reg.setStatus("REGISTERED");
            courseRegistrationRepository.save(reg);
            registered++;
        }

        return new com.kdu.csdtdas.backend.dto.BulkRegistrationResult(
                registered, skipped, checked.totalUnits(), checked.requiredUnits()
        );
    }

    public int registerAllMatching(
            Long courseId,
            Long academicSessionId,
            String semester
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        String cleanSemester = semester.trim().toUpperCase();

        List<Student> candidates = studentRepository.findAll().stream()
                .filter(s -> s.getLevel() != null
                        && s.getLevel().getId().equals(course.getLevel().getId()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getProgramme() != null
                        && course.getProgrammes().stream()
                        .anyMatch(p -> p.getId().equals(s.getProgramme().getId())))
                .toList();

        int registered = 0;

        for (Student student : candidates) {

            boolean alreadyRegistered = courseRegistrationRepository
                    .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                            student.getId(), courseId, academicSessionId, cleanSemester
                    );

            if (alreadyRegistered) {
                continue;
            }

            CourseRegistration registration = new CourseRegistration();
            registration.setStudent(student);
            registration.setCourse(course);
            registration.setAcademicSession(session);
            registration.setSemester(cleanSemester);
            registration.setStatus("REGISTERED");

            courseRegistrationRepository.save(registration);
            registered++;
        }

        return registered;
    }

    @Transactional(readOnly = true)
    public List<RegistrationStatusRecord> getRegistrationStatus(
            Long programmeId, Long levelId, Long academicSessionId, String semester
    ) {
        List<CourseRegistration> registrations = courseRegistrationRepository
                .findByProgrammeAndLevelAndSessionAndSemester(
                        programmeId, levelId, academicSessionId, semester.trim().toUpperCase()
                );

        Map<Long, RegistrationStatusRecord> byStudent = new LinkedHashMap<>();

        for (CourseRegistration reg : registrations) {
            Student student = reg.getStudent();
            RegistrationStatusRecord record = byStudent.get(student.getId());

            if (record == null) {
                record = new RegistrationStatusRecord(
                        student.getId(), student.getMatricNumber(), student.getFullName(),
                        student.getLevel().getCode(), 0, 0
                );
                byStudent.put(student.getId(), record);
            }

            record.setCoursesRegistered(record.getCoursesRegistered() + 1);
            record.setTotalUnits(record.getTotalUnits() + reg.getCourse().getCreditUnit());
        }

        return new ArrayList<>(byStudent.values());
    }
}