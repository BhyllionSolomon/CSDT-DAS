package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AdviserDTOs;
import com.kdu.csdtdas.backend.entity.AdviserAssignment;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AdviserAssignmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AdviserService {

    private static final Set<String> ELIGIBLE = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private final UserRepository userRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AdviserAssignmentRepository assignmentRepository;

    public AdviserService(
            UserRepository userRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository,
            AdviserAssignmentRepository assignmentRepository
    ) {
        this.userRepository = userRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public AdviserDTOs.Message assign(Long userId, Long programmeId, Long levelId, String actor) {

        if (programmeId == null || levelId == null) {
            throw new IllegalArgumentException("Choose both a programme and a level.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!ELIGIBLE.contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw new IllegalArgumentException("Only an existing lecturer account can be made a Level Adviser.");
        }

        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));

        // A programme and level has one adviser: whoever holds it now is replaced and archived.
        String replaced = null;
        for (User other : userRepository.findAll()) {
            if (other.getId().equals(userId)) continue;
            if (!"LEVEL_ADVISER".equalsIgnoreCase(other.getRole())) continue;
            if (other.getProgramme() == null || !other.getProgramme().getId().equals(programmeId)) continue;
            if (other.getLevel() == null || !other.getLevel().getId().equals(levelId)) continue;

            endAdviser(other, actor, "Replaced by " + user.getFullName());
            replaced = other.getFullName();
        }

        // If this person already advises somewhere else, that post is closed and archived.
        for (AdviserAssignment current : assignmentRepository.findActiveByUser(userId)) {
            close(current, actor, "Moved to " + level.getName() + " " + programme.getName());
        }

        AdviserAssignment record = new AdviserAssignment();
        record.setUser(user);
        record.setProgramme(programme);
        record.setLevel(level);
        record.setStartedAt(LocalDateTime.now());
        record.setAssignedBy(actor);
        assignmentRepository.save(record);

        user.setRole("LEVEL_ADVISER");
        user.setProgramme(programme);
        user.setLevel(level);
        userRepository.save(user);

        String message = user.getFullName() + " is now the Level Adviser for "
                + level.getName() + " " + programme.getName() + ".";
        if (replaced != null) {
            message += " " + replaced + " was replaced and their record has been archived.";
        }
        return new AdviserDTOs.Message(message);
    }

    public AdviserDTOs.Message remove(Long userId, String reason, String actor) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!"LEVEL_ADVISER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("This person is not a Level Adviser.");
        }

        String cleaned = reason == null || reason.isBlank() ? "Removed by the H.O.D" : reason.trim();
        endAdviser(user, actor, cleaned);

        return new AdviserDTOs.Message(
                user.getFullName() + " is no longer a Level Adviser. The record is archived and the account remains as a lecturer."
        );
    }

    public List<AdviserDTOs.Entry> history() {

        // Advisers created before history tracking get a starting record so nobody is missing.
        for (User u : userRepository.findAll()) {
            if ("LEVEL_ADVISER".equalsIgnoreCase(u.getRole()) && u.getProgramme() != null
                    && assignmentRepository.findActiveByUser(u.getId()).isEmpty()) {
                assignmentRepository.save(legacyRecord(u));
            }
        }

        return assignmentRepository.findAllWithDetails().stream()
                .map(a -> new AdviserDTOs.Entry(
                        a.getId(),
                        a.getUser().getId(),
                        a.getUser().getFullName(),
                        a.getUser().getEmail(),
                        a.getUser().getPhoneNumber(),
                        a.getProgramme().getName(),
                        a.getLevel() != null ? a.getLevel().getName() : "—",
                        a.getStartedAt(),
                        a.getEndedAt(),
                        a.getAssignedBy(),
                        a.getEndedBy(),
                        a.getEndReason()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AdviserDTOs.Contact> contactForStudent(String username) {

        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Student student = me.getStudent();
        if (student == null || student.getProgramme() == null || student.getLevel() == null) {
            return Optional.empty();
        }

        Long programmeId = student.getProgramme().getId();
        Long levelId = student.getLevel().getId();

        return userRepository.findAll().stream()
                .filter(u -> "LEVEL_ADVISER".equalsIgnoreCase(u.getRole()) && Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getProgramme() != null && u.getProgramme().getId().equals(programmeId))
                .filter(u -> u.getLevel() != null && u.getLevel().getId().equals(levelId))
                .findFirst()
                .map(u -> new AdviserDTOs.Contact(
                        u.getFullName(), u.getEmail(), u.getPhoneNumber(),
                        u.getProgramme().getName(), u.getLevel().getName()
                ));
    }

    private void endAdviser(User adviser, String actor, String reason) {

        List<AdviserAssignment> active = assignmentRepository.findActiveByUser(adviser.getId());
        if (active.isEmpty() && adviser.getProgramme() != null) {
            active = List.of(legacyRecord(adviser));
        }
        for (AdviserAssignment a : active) {
            close(a, actor, reason);
        }

        adviser.setRole("LECTURER");
        adviser.setProgramme(null);
        adviser.setLevel(null);
        userRepository.save(adviser);
    }

    private AdviserAssignment legacyRecord(User u) {
        AdviserAssignment legacy = new AdviserAssignment();
        legacy.setUser(u);
        legacy.setProgramme(u.getProgramme());
        legacy.setLevel(u.getLevel());
        legacy.setStartedAt(u.getCreatedAt() != null ? u.getCreatedAt() : LocalDateTime.now());
        legacy.setAssignedBy("recorded before history tracking");
        return legacy;
    }

    private void close(AdviserAssignment a, String actor, String reason) {
        a.setEndedAt(LocalDateTime.now());
        a.setEndedBy(actor);
        a.setEndReason(reason);
        assignmentRepository.save(a);
    }
}