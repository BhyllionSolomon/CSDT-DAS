package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.AcademicSession;
import com.solomon.epiforecaster.backend.entity.Department;
import com.solomon.epiforecaster.backend.entity.Level;
import com.solomon.epiforecaster.backend.entity.Programme;
import com.solomon.epiforecaster.backend.entity.Student;
import com.solomon.epiforecaster.backend.repository.AcademicSessionRepository;
import com.solomon.epiforecaster.backend.repository.DepartmentRepository;
import com.solomon.epiforecaster.backend.repository.LevelRepository;
import com.solomon.epiforecaster.backend.repository.ProgrammeRepository;
import com.solomon.epiforecaster.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public StudentService(
    StudentRepository studentRepository,
    DepartmentRepository departmentRepository,
    ProgrammeRepository programmeRepository,
    LevelRepository levelRepository,
    AcademicSessionRepository academicSessionRepository
    ) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    public Student createStudent(
    String matricNumber,
    String fullName,
    String phoneNumber,
    Long departmentId,
    Long programmeId,
    Long levelId,
    Long academicSessionId
    ) {

        if (matricNumber == null || matricNumber.isBlank()) {
            throw new IllegalArgumentException("Matric number is required.");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Student name is required.");
        }

        if (studentRepository.existsByMatricNumber(matricNumber.trim())) {
            throw new IllegalArgumentException(
                    "A student with this matric number already exists."
                    );
        }

        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() ->
        new IllegalArgumentException("Department not found.")
        );

        Programme programme = programmeRepository.findById(programmeId)
            .orElseThrow(() ->
        new IllegalArgumentException("Programme not found.")
        );

        Level level = levelRepository.findById(levelId)
            .orElseThrow(() ->
        new IllegalArgumentException("Level not found.")
        );

        AcademicSession academicSession =
        academicSessionRepository.findById(academicSessionId)
            .orElseThrow(() ->
        new IllegalArgumentException(
                "Academic session not found."
                )
        );

        if (!programme.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException(
                    "The selected programme does not belong to the selected department."
                    );
        }

        Student student = new Student();

        student.setMatricNumber(matricNumber.trim());
        student.setFullName(fullName.trim());
        student.setPhoneNumber(phoneNumber);
        student.setDepartment(department);
        student.setProgramme(programme);
        student.setLevel(level);
        student.setAcademicSession(academicSession);
        student.setStatus("ACTIVE");

        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student getStudent(Long studentId) {

        return studentRepository.findById(studentId)
            .orElseThrow(() ->
        new IllegalArgumentException("Student not found.")
        );
    }

    @Transactional(readOnly = true)
    public Student getStudentByMatricNumber(String matricNumber) {

        return studentRepository.findByMatricNumber(matricNumber.trim())
            .orElseThrow(() ->
        new IllegalArgumentException("Student not found.")
        );
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student updateStudentLevel(
    Long studentId,
    Long levelId
    ) {

        Student student = getStudent(studentId);

        Level level = levelRepository.findById(levelId)
            .orElseThrow(() ->
        new IllegalArgumentException("Level not found.")
        );

        student.setLevel(level);

        return studentRepository.save(student);
    }

    public Student updateStudentProgramme(
    Long studentId,
    Long programmeId
    ) {

        Student student = getStudent(studentId);

        Programme programme = programmeRepository.findById(programmeId)
            .orElseThrow(() ->
        new IllegalArgumentException("Programme not found.")
        );

        if (!programme.getDepartment().getId()
                .equals(student.getDepartment().getId())) {

            throw new IllegalArgumentException(
                    "The selected programme does not belong to the student's department."
                    );
        }

        student.setProgramme(programme);

        return studentRepository.save(student);
    }

    public Student deactivateStudent(Long studentId) {

        Student student = getStudent(studentId);

        student.setStatus("INACTIVE");

        return studentRepository.save(student);
    }

    public Student activateStudent(Long studentId) {

        Student student = getStudent(studentId);

        student.setStatus("ACTIVE");

        return studentRepository.save(student);
    }
}