package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LabStudentDTOs;
import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class LabManualStudentService {
    private final StudentRepository students;
    private final LabManualRepository manuals;
    private final LabManualOrderRepository orders;
    private final RegistrationClearanceService clearance;

    public LabManualStudentService(StudentRepository students, LabManualRepository manuals,
            LabManualOrderRepository orders, RegistrationClearanceService clearance) {
        this.students=students; this.manuals=manuals; this.orders=orders; this.clearance=clearance;
    }

    @Transactional(readOnly=true)
    public List<LabStudentDTOs.ManualOption> catalogue(String username) {
        Student student = student(username);
        AcademicSession session = clearance.currentSession();
        if (session == null) throw new IllegalArgumentException("No active academic session is available.");
        return manuals.findAllWithCourse().stream().filter(LabManual::isActive).map(m -> {
            var order = orders.findFirstByManualIdAndStudentIdAndAcademicSessionIdAndStatusIn(
                    m.getId(), student.getId(), session.getId(), List.of("AWAITING_PAYMENT","PAID","ISSUED"));
            String status = order.map(LabManualOrder::getStatus).orElse("NOT_ORDERED");
            Long orderId = order.map(LabManualOrder::getId).orElse(null);
            return new LabStudentDTOs.ManualOption(m.getId(), m.getTitle(),
                    m.getCourse() == null ? "" : m.getCourse().getCode(),
                    m.getCourse() == null ? "" : m.getCourse().getTitle(),
                    m.getUnitPrice(), status, orderId);
        }).toList();
    }

    public LabStudentDTOs.OrderRow request(String username, LabStudentDTOs.OrderRequest request) {
        Student student = student(username);
        if (request == null || request.manualId() == null) throw new IllegalArgumentException("Select a lab manual.");
        AcademicSession session = clearance.currentSession();
        if (session == null) throw new IllegalArgumentException("No active academic session is available.");
        LabManual manual = manuals.findById(request.manualId())
                .orElseThrow(() -> new IllegalArgumentException("Lab manual not found."));
        if (!manual.isActive()) throw new IllegalArgumentException("This lab manual is not currently available.");
        var existing = orders.findFirstByManualIdAndStudentIdAndAcademicSessionIdAndStatusIn(
                manual.getId(), student.getId(), session.getId(), List.of("AWAITING_PAYMENT","PAID","ISSUED"));
        if (existing.isPresent()) return row(existing.get());
        LabManualOrder order = new LabManualOrder();
        order.setManual(manual); order.setStudent(student); order.setAcademicSession(session);
        order.setAmount(manual.getUnitPrice()); order.setStatus("AWAITING_PAYMENT");
        return row(orders.save(order));
    }

    private Student student(String username) {
        Student s = students.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Your student profile is not linked to this account. Contact the department."));
        if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) throw new IllegalArgumentException("Your student account is not active.");
        return s;
    }
    private LabStudentDTOs.OrderRow row(LabManualOrder o) {
        return new LabStudentDTOs.OrderRow(o.getId(), o.getManual().getId(), o.getManual().getTitle(),
                o.getManual().getCourse() == null ? "" : o.getManual().getCourse().getCode(),
                o.getAmount(), o.getStatus(), o.getAcademicSession().getName(), o.getCreatedAt());
    }
}
