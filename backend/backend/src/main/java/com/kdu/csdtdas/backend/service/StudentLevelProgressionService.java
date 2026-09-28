package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentLevelProgressionService {

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;
    private final StudentLevelHistoryService studentLevelHistoryService;

    public StudentLevelProgressionService(
            StudentRepository studentRepository,
            LevelRepository levelRepository,
            StudentLevelHistoryService studentLevelHistoryService
    ) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
        this.studentLevelHistoryService = studentLevelHistoryService;
    }

    /**
     * The level a student SHOULD be at right now, computed purely from how
     * many sessions have passed since their admission session.
     */
    @Transactional(readOnly = true)
    public Level computeCurrentLevel(Student student, AcademicSession currentSession) {

        if (student.getAdmissionSession() == null) {
            return student.getLevel();
        }

        int yearsElapsed = yearsBetween(student.getAdmissionSession(), currentSession);
        int targetLevelNumber = Math.min(100 + (yearsElapsed * 100), 400);

        return findLevelByNumber(targetLevelNumber).orElse(student.getLevel());
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
     * Aligns every ACTIVE student's stored level with the level computed
     * from their admission session. Only affects students who have an
     * admission session recorded.
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
     * moves up exactly one level (100 to 200 to 300 to 400, capped at 400),
     * and their current academic session updates to the new session. Their
     * outgoing and incoming states are both recorded in the history table.
     */
    public int advanceAllStudentsByOneLevel(AcademicSession newSession) {

        List<Student> students = studentRepository.findAll();
        int advanced = 0;

        for (Student student : students) {

            if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) continue;
            if (student.getLevel() == null || student.getLevel().getLevelNumber() == null) continue;

            studentLevelHistoryService.recordSnapshot(student);

            int currentNumber = student.getLevel().getLevelNumber();
            int nextNumber = Math.min(currentNumber + 100, 400);

            Optional<Level> nextLevel = findLevelByNumber(nextNumber);

            if (nextLevel.isPresent()) {
                student.setLevel(nextLevel.get());
                student.setAcademicSession(newSession);
                Student saved = studentRepository.save(student);
                studentLevelHistoryService.recordSnapshot(saved);
                advanced++;
            }
        }

        return advanced;
    }
}