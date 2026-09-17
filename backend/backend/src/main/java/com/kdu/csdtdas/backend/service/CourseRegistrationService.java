package com.kdu.csdtdas.backend.service;

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
import java.util.List;

@Service
@Transactional
public class CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final ResultCalculationService resultCalculationService;
    private final SemesterUnitLimitService unitLimitService;

    public CourseRegistrationService(
            CourseRegistrationRepository courseRegistrationRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository,
            ResultCalculationService resultCalculationService,
            SemesterUnitLimitService unitLimitService
    ) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.resultCalculationService = resultCalculationService;
        this.unitLimitService = unitLimitService;
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
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found.")
                );

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found.")
                );

        AcademicSession academicSession = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Academic session not found.")
                );

        if (!student.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new IllegalArgumentException(
                    "Student is not active and cannot register courses."
            );
        }

        if (!course.getActive()) {
            throw new IllegalArgumentException(
                    "Course is not active and cannot be registered."
            );
        }

        if (!academicSession.getActive()) {
            throw new IllegalArgumentException(
                    "Academic session is not active."
            );
        }

        boolean alreadyRegistered =
                courseRegistrationRepository
                        .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                                studentId,
                                courseId,
                                academicSessionId,
                                semester
                        );

        if (alreadyRegistered) {
            throw new IllegalArgumentException(
                    "Student has already registered this course for this semester."
            );
        }

        if (!course.getSemester().equalsIgnoreCase(semester)) {
            throw new IllegalArgumentException(
                    "This course is not offered in the selected semester."
            );
        }

        if (!course.getDepartment().getId().equals(student.getDepartment().getId())) {
            throw new IllegalArgumentException(
                    "This course does not belong to the student's department."
            );
        }

        if (!course.getLevel().getId().equals(student.getLevel().getId())) {
            throw new IllegalArgumentException(
                    "This course is not assigned to the student's level."
            );
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
                .findByStudentIdAndAcademicSessionId(
                        studentId,
                        academicSessionId
                );
    }

    public void cancelRegistration(Long registrationId) {

        CourseRegistration registration =
                courseRegistrationRepository.findById(registrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Course registration not found."
                                )
                        );

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
                        courseId,
                        academicSessionId,
                        semester.trim().toUpperCase()
                );
    }
    public com.kdu.csdtdas.backend.dto.BulkRegistrationResult registerMultiple(
            Long studentId,
            Long academicSessionId,
            String semester,
            List<Long> courseIds
    ) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        String cleanSemester = semester.trim().toUpperCase();

        List<Course> outstanding = resultCalculationService.getOutstandingCourses(studentId);
        List<Long> mandatoryIds = outstanding.stream().map(Course::getId).toList();

        List<Long> missing = mandatoryIds.stream()
                .filter(id -> !courseIds.contains(id))
                .toList();

        if (!missing.isEmpty()) {
            List<String> codes = outstanding.stream()
                    .filter(c -> missing.contains(c.getId()))
                    .map(Course::getCode)
                    .toList();
            throw new IllegalArgumentException(
                    "Outstanding carryover course(s) must be registered first: "
                            + String.join(", ", codes)
            );
        }

        int requiredUnits = unitLimitService.getRequiredUnits(
                student.getProgramme().getId(), student.getLevel().getId(), cleanSemester
        );

        int totalUnits = 0;
        List<Course> coursesToRegister = new ArrayList<>();
        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
            totalUnits += course.getCreditUnit();
            coursesToRegister.add(course);
        }

        if (totalUnits != requiredUnits) {
            String direction = totalUnits > requiredUnits ? "exceeds" : "is below";
            throw new IllegalArgumentException(
                    "Total selected units (" + totalUnits + ") " + direction
                            + " the required total of " + requiredUnits
                            + " for this level and semester. Please adjust your selection to match "
                            + "exactly, or print this notice and contact your Level Adviser for approval."
            );
        }

        int registered = 0;
        int skipped = 0;

        for (Course course : coursesToRegister) {
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
                registered, skipped, totalUnits, requiredUnits
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

            boolean alreadyRegistered =
                    courseRegistrationRepository
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
}