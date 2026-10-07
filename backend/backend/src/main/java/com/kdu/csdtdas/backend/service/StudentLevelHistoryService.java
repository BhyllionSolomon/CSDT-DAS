package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.StudentSessionRecord;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import com.kdu.csdtdas.backend.repository.StudentLevelHistoryRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
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

    /** Records (or overwrites) the student's level/programme for their current session. */
    public void recordSnapshot(Student studentRef) {

        if (studentRef == null || studentRef.getId() == null) return;

        Student student = studentRepository.findByIdWithDetails(studentRef.getId()).orElse(null);

        if (student == null) return;
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

    public int backfillFromCurrentState() {
        return ensureSnapshots();
    }

    /** Writes a snapshot for every student who does not yet have one for their current session. */
    private int ensureSnapshots() {
        Set<String> existing = historyRepository.findAll().stream()
                .map(h -> h.getStudent().getId() + "-" + h.getAcademicSession().getId())
                .collect(Collectors.toSet());

        int written = 0;

        for (Student student : studentRepository.findAllWithDetails()) {
            if (student.getAcademicSession() == null) continue;
            if (existing.contains(student.getId() + "-" + student.getAcademicSession().getId())) continue;

            recordSnapshot(student);
            written++;
        }

        return written;
    }

    /** The students who were in this programme and level during this session. */
    public List<StudentLevelHistory> cohort(Long sessionId, Long programmeId, Long levelId) {
        ensureSnapshots();

        return historyRepository.findByAcademicSessionId(sessionId).stream()
                .filter(h -> h.getProgramme().getId().equals(programmeId)
                        && h.getLevel().getId().equals(levelId))
                .toList();
    }

    public List<StudentSessionRecord> getForSession(Long academicSessionId) {
        ensureSnapshots();

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