package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentLevelProgressionService {

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;

    public StudentLevelProgressionService(
            StudentRepository studentRepository,
            LevelRepository levelRepository
    ) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
    }

    /**
     * The level a student SHOULD be at right now, computed purely from how
     * many sessions have passed since their admission session — independent
     * of whatever is currently stored in Student.level.
     */
    @Transactional(readOnly = true)
    public Level computeCurrentLevel(Student student, AcademicSession currentSession) {

        if (student.getAdmissionSession() == null) {
            // No admission session on record — fall back to whatever is stored.
            return student.getLevel();
        }

        int yearsElapsed = yearsBetween(student.getAdmissionSession(), currentSession);

        int startingLevelNumber = student.getLevel() != null
                ? levelNumberAtAdmission(student)
                : 100;

        int targetLevelNumber = Math.min(startingLevelNumber + (yearsElapsed * 100), 400);

        return findLevelByNumber(targetLevelNumber).orElse(student.getLevel());
    }

    private int levelNumberAtAdmission(Student student) {
        // If the student's current stored level is already past 100, and we
        // don't know their exact admission level, assume 100 as the safest
        // default for freshly-admitted students uploaded via roster.
        return 100;
    }

    private int yearsBetween(AcademicSession admission, AcademicSession current) {
        if (admission.getStartDate() == null || current.getStartDate() == null) return 0;
        int years = current.getStartDate().getYear() - admission.getStartDate().getYear();
        return Math.max(0, years);
    }

    private Optional<Level> findLevelByNumber(int number) {
        return levelRepository.findAll().stream()
                .filter(l -> l.getLevelNumber() != null && l.getLevelNumber() == number)
                .findFirst();
    }

    /**
     * Bulk-advances every ACTIVE student's stored level to match their
     * computed current level, for the given now-current session. Run this
     * once, by the H.O.D, at the start of each new academic session.
     */
    public int advanceAllStudents(AcademicSession currentSession) {

        List<Student> students = studentRepository.findAll();
        int advanced = 0;

        for (Student student : students) {

            if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) continue;
            if (student.getAdmissionSession() == null) continue;

            Level computed = computeCurrentLevel(student, currentSession);

            if (computed != null && (student.getLevel() == null
                    || !computed.getId().equals(student.getLevel().getId()))) {
                student.setLevel(computed);
                studentRepository.save(student);
                advanced++;
            }
        }

        return advanced;
    }


    /**
     * Direct level bump for the start of a new session: every ACTIVE student
     * moves up exactly one level (100→200→300→400, capped at 400), and their
     * current academicSession updates to the new session. This does not
     * require any admission-date history — it simply advances everyone by
     * one tier, which is what actually happens at the start of each session.
     */
    public int advanceAllStudentsByOneLevel(AcademicSession newSession) {

        List<Student> students = studentRepository.findAll();
        int advanced = 0;

        for (Student student : students) {

            if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) continue;
            if (student.getLevel() == null || student.getLevel().getLevelNumber() == null) continue;

            int currentNumber = student.getLevel().getLevelNumber();
            int nextNumber = Math.min(currentNumber + 100, 400);

            Optional<Level> nextLevel = findLevelByNumber(nextNumber);

            if (nextLevel.isPresent()) {
                student.setLevel(nextLevel.get());
                student.setAcademicSession(newSession);
                studentRepository.save(student);
                advanced++;
            }
        }

        return advanced;
    }
}