package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.AcademicSession;
import com.solomon.epiforecaster.backend.repository.AcademicSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AcademicSessionService {

    private final AcademicSessionRepository academicSessionRepository;

    public AcademicSessionService(
            AcademicSessionRepository academicSessionRepository
    ) {
        this.academicSessionRepository = academicSessionRepository;
    }

    public AcademicSession createSession(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Academic session name is required."
            );
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required."
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be after start date."
            );
        }

        AcademicSession session = new AcademicSession();

        session.setName(name.trim());
        session.setStartDate(startDate);
        session.setEndDate(endDate);
        session.setCurrent(false);
        session.setActive(true);

        return academicSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public AcademicSession getSession(Long sessionId) {

        return academicSessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Academic session not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<AcademicSession> getAllSessions() {
        return academicSessionRepository.findAll();
    }

    public AcademicSession setCurrentSession(Long sessionId) {

        AcademicSession selectedSession = getSession(sessionId);

        List<AcademicSession> sessions =
                academicSessionRepository.findAll();

        for (AcademicSession session : sessions) {
            session.setCurrent(false);
        }

        selectedSession.setCurrent(true);

        academicSessionRepository.saveAll(sessions);

        return selectedSession;
    }

    public AcademicSession deactivateSession(Long sessionId) {

        AcademicSession session = getSession(sessionId);

        session.setActive(false);
        session.setCurrent(false);

        return academicSessionRepository.save(session);
    }

    public AcademicSession activateSession(Long sessionId) {

        AcademicSession session = getSession(sessionId);

        session.setActive(true);

        return academicSessionRepository.save(session);
    }
}