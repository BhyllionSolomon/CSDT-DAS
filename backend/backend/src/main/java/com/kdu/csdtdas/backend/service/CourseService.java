package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.DepartmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final LevelRepository levelRepository;

    public CourseService(
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            LevelRepository levelRepository
    ) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.levelRepository = levelRepository;
    }

    public Course createCourse(
            String code,
            String title,
            Integer creditUnit,
            Long departmentId,
            Long levelId,
            String semester
    ) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title is required.");
        }

        if (creditUnit == null || creditUnit <= 0) {
            throw new IllegalArgumentException(
                    "Credit unit must be greater than zero."
            );
        }

        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        String cleanCode = code.trim().toUpperCase();
        String cleanSemester = semester.trim().toUpperCase();

        if (courseRepository.existsByCode(cleanCode)) {
            throw new IllegalArgumentException(
                    "A course with this code already exists."
            );
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Department not found.")
                );

        Level level = levelRepository.findById(levelId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Level not found.")
                );

        Course course = new Course();

        course.setCode(cleanCode);
        course.setTitle(title.trim());
        course.setCreditUnit(creditUnit);
        course.setDepartment(department);
        course.setLevel(level);
        course.setSemester(cleanSemester);
        course.setActive(true);

        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long courseId) {

        return courseRepository.findByIdWithDetails(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found.")
                );
    }

    @Transactional(readOnly = true)
    public Course getCourseByCode(String code) {

        return courseRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found.")
                );
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAllWithDetails();
    }

    public Course updateCourse(
            Long courseId,
            String title,
            Integer creditUnit,
            String semester
    ) {

        Course course = getCourse(courseId);

        if (title != null && !title.isBlank()) {
            course.setTitle(title.trim());
        }

        if (creditUnit != null && creditUnit > 0) {
            course.setCreditUnit(creditUnit);
        }

        if (semester != null && !semester.isBlank()) {
            course.setSemester(semester.trim().toUpperCase());
        }

        return courseRepository.save(course);
    }

    public Course deactivateCourse(Long courseId) {

        Course course = getCourse(courseId);

        course.setActive(false);

        return courseRepository.save(course);
    }

    public Course activateCourse(Long courseId) {

        Course course = getCourse(courseId);

        course.setActive(true);

        return courseRepository.save(course);
    }
}