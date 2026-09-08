package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.AcademicSession;
import com.solomon.epiforecaster.backend.entity.Course;
import com.solomon.epiforecaster.backend.entity.CourseRegistration;
import com.solomon.epiforecaster.backend.entity.Student;
import com.solomon.epiforecaster.backend.repository.AcademicSessionRepository;
import com.solomon.epiforecaster.backend.repository.CourseRegistrationRepository;
import com.solomon.epiforecaster.backend.repository.CourseRepository;
import com.solomon.epiforecaster.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public CourseRegistrationService(
            CourseRegistrationRepository courseRegistrationRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository
    ) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
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
}