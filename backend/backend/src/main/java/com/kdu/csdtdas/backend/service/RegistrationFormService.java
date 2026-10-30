package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.RegistrationFormDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.RegistrationClearance;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.RegistrationClearanceRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrationFormService {

    private final RegistrationClearanceService clearanceService;
    private final RegistrationClearanceRepository clearanceRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public RegistrationFormService(
            RegistrationClearanceService clearanceService,
            RegistrationClearanceRepository clearanceRepository,
            CourseRegistrationRepository registrationRepository,
            StudentRepository studentRepository,
            UserRepository userRepository
    ) {
        this.clearanceService = clearanceService;
        this.clearanceRepository = clearanceRepository;
        this.registrationRepository = registrationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    /** The signed-in student's own form. */
    public RegistrationFormDTOs.Form forUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getStudent() == null) {
            throw new IllegalArgumentException("Your account is not linked to a student record.");
        }
        return build(user.getStudent().getId());
    }

    public RegistrationFormDTOs.Form forStudent(Long studentId) {
        return build(studentId);
    }

    private RegistrationFormDTOs.Form build(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        AcademicSession session = clearanceService.currentSession();

        List<RegistrationFormDTOs.CourseLine> lines = new ArrayList<>();
        int total = 0;
        RegistrationClearance clearance = null;

        if (session != null) {
            for (CourseRegistration r : registrationRepository
                    .findByStudentIdAndAcademicSessionId(studentId, session.getId())) {

                if (!"REGISTERED".equalsIgnoreCase(r.getStatus())) continue;

                Course c = r.getCourse();
                lines.add(new RegistrationFormDTOs.CourseLine(
                        c.getCode(), c.getTitle(), c.getCreditUnit(), c.getStatus(), r.getSemester()
                ));
                total += c.getCreditUnit() == null ? 0 : c.getCreditUnit();
            }

            lines.sort(Comparator.comparing(RegistrationFormDTOs.CourseLine::semester)
                    .thenComparing(RegistrationFormDTOs.CourseLine::code));

            clearance = clearanceRepository
                    .findByStudentIdAndAcademicSessionId(studentId, session.getId())
                    .orElse(null);
        }

        return new RegistrationFormDTOs.Form(
                student.getId(), student.getMatricNumber(), student.getFullName(),
                student.getProgramme() != null ? student.getProgramme().getName() : null,
                student.getLevel() != null ? student.getLevel().getName() : null,
                session != null ? session.getId() : null,
                session != null ? session.getName() : null,
                lines, total,
                clearance == null ? "NONE" : clearance.getStatus(),
                clearance == null ? null : clearance.getRegistrationCode(),
                clearance == null ? null : clearance.getInvoiceCode(),
                clearance == null ? null : clearance.getNote(),
                clearance == null ? null : clearance.getDecidedBy(),
                clearance == null ? null : clearance.getDecidedAt()
        );
    }
}