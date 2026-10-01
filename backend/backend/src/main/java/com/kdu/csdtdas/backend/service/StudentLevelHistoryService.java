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
     * Records (or overwrites) a snapshot of the given student's level and
     * programme as of their current academic session. Always re-fetches the
     * student with all relations eagerly joined, so this is safe to call
     * regardless of the caller's transaction/session state.
     */
    public void recordSnapshot(Student studentRef) {

        if (studentRef == null || studentRef.getId() == null) return;

        Student student = studentRepository.findByIdWithDetails(studentRef.getId())
                .orElse(null);

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
        List<Student> students = studentRepository.findAllWithDetails();
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