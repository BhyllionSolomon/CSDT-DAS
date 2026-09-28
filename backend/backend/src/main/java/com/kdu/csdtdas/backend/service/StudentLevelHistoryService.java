package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.StudentSessionRecord;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import com.kdu.csdtdas.backend.repository.StudentLevelHistoryRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentLevelHistoryService {

    private final StudentLevelHistoryRepository historyRepository;
    private final StudentRepository studentRepository;

    public StudentLevelHistoryService(
            StudentLevelHistoryRepository historyRepository,
            StudentRepository studentRepository
    ) {
        this.historyRepository = historyRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Records (or overwrites, if one already exists) the given student's
     * level/programme as of their current academicSession. Call this
     * whenever a student's session/level/programme changes, so the
     * snapshot for that session is never lost.
     */
    public void recordSnapshot(Student student) {

        if (student.getAcademicSession() == null || student.getLevel() == null
                || student.getProgramme() == null) {
            return;
        }

        StudentLevelHistory history = historyRepository
                .findByStudentIdAndAcademicSessionId(student.getId(), student.getAcademicSession().getId())
                .orElseGet(StudentLevelHistory::new);

        history.setStudent(student);
        history.setAcademicSession(student.getAcademicSession());
        history.setLevel(student.getLevel());
        history.setProgramme(student.getProgramme());

        historyRepository.save(history);
    }

    /**
     * One-time catch-up for students created before this history feature
     * existed: writes a snapshot for each student's CURRENT live session,
     * using whatever level/session/programme they presently show.
     */
    public int backfillFromCurrentState() {
        List<Student> students = studentRepository.findAll();
        int written = 0;

        for (Student student : students) {
            boolean exists = student.getAcademicSession() != null
                    && historyRepository.findByStudentIdAndAcademicSessionId(
                    student.getId(), student.getAcademicSession().getId()
            ).isPresent();

            if (!exists) {
                recordSnapshot(student);
                written++;
            }
        }
        return written;
    }

    @Transactional(readOnly = true)
    public List<StudentSessionRecord> getForSession(Long academicSessionId) {
        return historyRepository.findByAcademicSessionId(academicSessionId).stream()
                .map(h -> new StudentSessionRecord(
                        h.getStudent().getId(),
                        h.getStudent().getMatricNumber(),
                        h.getStudent().getFullName(),
                        h.getProgramme().getCode(),
                        h.getProgramme().getName(),
                        h.getLevel().getCode()
                ))
                .collect(Collectors.toList());
    }
}