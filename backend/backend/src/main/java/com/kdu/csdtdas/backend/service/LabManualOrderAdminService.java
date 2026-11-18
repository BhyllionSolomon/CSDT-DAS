package com.kdu.csdtdas.backend.service;
import com.kdu.csdtdas.backend.dto.LabManualOrderAdminRow;
import com.kdu.csdtdas.backend.repository.LabManualOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional(readOnly=true)
public class LabManualOrderAdminService {
    private final LabManualOrderRepository orders;
    public LabManualOrderAdminService(LabManualOrderRepository orders) { this.orders=orders; }
    public List<LabManualOrderAdminRow> list() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(o -> new LabManualOrderAdminRow(
                o.getId(), o.getStudent().getMatricNumber(), o.getStudent().getFullName(),
                o.getStudent().getProgramme() == null ? "" : o.getStudent().getProgramme().getName(),
                o.getStudent().getLevel() == null ? "" : o.getStudent().getLevel().getName(),
                o.getManual().getCourse() == null ? "" : o.getManual().getCourse().getCode(),
                o.getManual().getTitle(), o.getAmount(), o.getStatus(), o.getAcademicSession().getName(), o.getCreatedAt(),
                o.getReceiptNumber(), o.getPaystackReference(), o.getPaidAt(), o.getIssuedAt(), o.getIssuedBy()
        )).toList();
    }
}
