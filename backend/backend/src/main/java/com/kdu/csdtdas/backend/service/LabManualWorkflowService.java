package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LabManualWorkflowDTO;
import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class LabManualWorkflowService {
    private static final Set<String> LABS = Set.of("COMPUTER_SCIENCE", "SOFTWARE_ENGINEERING", "CYBERSECURITY");
    private final LabManualSubmissionRepository submissions;
    private final CourseRepository courses;
    private final LabManualRepository manuals;
    private final LabManualOrderRepository orders;

    public LabManualWorkflowService(LabManualSubmissionRepository submissions, CourseRepository courses,
            LabManualRepository manuals, LabManualOrderRepository orders) {
        this.submissions = submissions;
        this.courses = courses;
        this.manuals = manuals;
        this.orders = orders;
    }

    public LabManualWorkflowDTO.Row upload(String username, String role, String title, Long courseId,
            Integer pages, BigDecimal suggested, MultipartFile file) {
        if (!Set.of("LECTURER", "ADJUNCT").contains(role)) throw denied();
        if (title == null || title.isBlank() || title.length() > 150) throw bad("Enter a manual title.");
        if (courseId == null) throw bad("Select a course.");
        if (pages == null || pages < 1 || pages > 2000) throw bad("Enter a valid page count.");
        if (file == null || file.isEmpty() || file.getSize() > 15L * 1024 * 1024)
            throw bad("Upload a PDF not larger than 15 MB.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D'
                    || bytes[3] != 'F' || bytes[4] != '-')
                throw bad("The uploaded file is not a valid PDF.");
            LabManualSubmission submission = new LabManualSubmission();
            submission.setTitle(title.trim());
            submission.setCourse(courses.findById(courseId).orElseThrow(() -> bad("Course not found.")));
            submission.setPageCount(pages);
            submission.setSuggestedLevy(suggested);
            submission.setUploadedBy(username);
            submission.setFileName(file.getOriginalFilename() == null ? "course-manual.pdf" : file.getOriginalFilename());
            submission.setFileData(bytes);
            submission.setContentType("application/pdf");
            return row(submissions.save(submission));
        } catch (java.io.IOException ex) {
            throw bad("Could not read the uploaded PDF.");
        }
    }

    @Transactional(readOnly = true)
    public List<LabManualWorkflowDTO.Row> mine(String username) {
        return submissions.findAllByUploadedByOrderBySubmittedAtDesc(username).stream().map(this::row).toList();
    }

    @Transactional(readOnly = true)
    public List<LabManualWorkflowDTO.Row> hodQueue(String role) {
        if (!Set.of("HOD", "ADMIN").contains(role)) throw denied();
        return submissions.findAllByStatusOrderBySubmittedAtAsc("SUBMITTED_TO_HOD").stream().map(this::row).toList();
    }

    @Transactional(readOnly = true)
    public List<LabManualWorkflowDTO.Row> labQueue(String role) {
        if (!Set.of("LAB_OFFICER", "TECHNICAL_STAFF", "HOD", "ADMIN").contains(role)) throw denied();
        return submissions.findAll().stream()
                .filter(s -> "SENT_TO_LAB".equals(s.getStatus()) || "RECEIVED".equals(s.getStatus()))
                .map(this::row).toList();
    }

    public LabManualWorkflowDTO.Row review(String username, String role, Long id,
            LabManualWorkflowDTO.Review request, boolean approve) {
        if (!Set.of("HOD", "ADMIN").contains(role)) throw denied();
        LabManualSubmission submission = get(id);
        if (!"SUBMITTED_TO_HOD".equals(submission.getStatus())) throw bad("This submission has already been reviewed.");
        submission.setHodReviewedBy(username);
        submission.setHodComment(request == null ? null : request.comment());
        if (approve) {
            if (request == null || request.approvedLevy() == null
                    || request.approvedLevy().compareTo(BigDecimal.ZERO) <= 0)
                throw bad("HOD must set a levy greater than zero.");
            submission.setApprovedLevy(request.approvedLevy().setScale(2, java.math.RoundingMode.HALF_UP));
            submission.setStatus("APPROVED");
            submission.setApprovedAt(LocalDateTime.now());
        } else {
            submission.setStatus("RETURNED");
        }
        return row(submission);
    }

    public LabManualWorkflowDTO.Row dispatch(String username, String role, Long id, String targetLab) {
        if (!Set.of("LECTURER", "ADJUNCT").contains(role)) throw denied();
        LabManualSubmission submission = get(id);
        if (!submission.getUploadedBy().equals(username)) throw denied();
        if (!"APPROVED".equals(submission.getStatus()))
            throw bad("Only an HOD-approved manual can be sent to a laboratory.");
        if (!LABS.contains(targetLab)) throw bad("Choose Computer Science, Software Engineering or Cybersecurity.");
        submission.setTargetLab(targetLab);
        submission.setSentToLabBy(username);
        submission.setSentToLabAt(LocalDateTime.now());
        submission.setStatus("SENT_TO_LAB");
        return row(submission);
    }

    public LabManualWorkflowDTO.Row receive(String username, String role, Long id) {
        if (!Set.of("LAB_OFFICER", "TECHNICAL_STAFF", "HOD", "ADMIN").contains(role)) throw denied();
        LabManualSubmission submission = get(id);
        if (!"SENT_TO_LAB".equals(submission.getStatus())) throw bad("This manual is not awaiting receipt.");
        submission.setReceivedBy(username);
        submission.setReceivedAt(LocalDateTime.now());
        submission.setStatus("RECEIVED");

        LabManual manual = manuals.findAllWithCourse().stream()
                .filter(m -> m.getCourse() != null
                        && m.getCourse().getId().equals(submission.getCourse().getId())
                        && m.getTitle().equals(submission.getTitle()))
                .findFirst().orElse(null);
        if (manual == null) {
            manual = new LabManual();
            manual.setTitle(submission.getTitle());
            manual.setCourse(submission.getCourse());
        }
        manual.setUnitPrice(submission.getApprovedLevy());
        manual.setActive(true);
        manuals.save(manual);
        return row(submission);
    }

    @Transactional(readOnly = true)
    public List<LabManualWorkflowDTO.Buyer> buyers(String username, String role, Long submissionId) {
        LabManualSubmission submission = get(submissionId);
        if (!Set.of("HOD", "ADMIN").contains(role) && !submission.getUploadedBy().equals(username))
            throw denied();
        LabManual manual = manuals.findAllWithCourse().stream()
                .filter(m -> m.getCourse() != null
                        && m.getCourse().getId().equals(submission.getCourse().getId())
                        && m.getTitle().equals(submission.getTitle()))
                .findFirst().orElse(null);
        if (manual == null) return List.of();
        return orders.findAllByManualIdOrderByCreatedAtDesc(manual.getId()).stream()
                .map(order -> new LabManualWorkflowDTO.Buyer(order.getId(),
                        order.getStudent().getMatricNumber(), order.getStudent().getFullName(),
                        order.getStudent().getProgramme() == null ? "" : order.getStudent().getProgramme().getName(),
                        order.getStudent().getLevel() == null ? "" : order.getStudent().getLevel().getName(),
                        order.getAmount(), order.getStatus(), order.getPaidAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] download(String username, String role, Long id) {
        LabManualSubmission submission = get(id);
        boolean staff = Set.of("HOD", "ADMIN", "LAB_OFFICER", "TECHNICAL_STAFF").contains(role);
        boolean owner = Set.of("LECTURER", "ADJUNCT").contains(role) && submission.getUploadedBy().equals(username);
        if (!staff && !owner) throw denied();
        return submission.getFileData();
    }

    private LabManualSubmission get(Long id) {
        return submissions.findById(id).orElseThrow(() -> bad("Manual submission not found."));
    }

    private LabManualWorkflowDTO.Row row(LabManualSubmission s) {
        return new LabManualWorkflowDTO.Row(s.getId(), s.getTitle(), s.getCourse().getCode(),
                s.getCourse().getTitle(), s.getPageCount(), s.getSuggestedLevy(), s.getApprovedLevy(),
                s.getStatus(), s.getTargetLab(), s.getUploadedBy(), s.getHodReviewedBy(), s.getHodComment(),
                s.getReceivedBy(), s.getSubmittedAt(), s.getApprovedAt(), s.getReceivedAt());
    }

    private RuntimeException denied() {
        return new org.springframework.security.access.AccessDeniedException("You are not authorised to perform this action.");
    }

    private RuntimeException bad(String message) { return new IllegalArgumentException(message); }
}
