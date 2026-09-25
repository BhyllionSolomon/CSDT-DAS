package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class SaveSelfReportedResultsRequest {
    private Long studentId;
    private List<SelfReportedResultRow> rows;

    public SaveSelfReportedResultsRequest() {}
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public List<SelfReportedResultRow> getRows() { return rows; }
    public void setRows(List<SelfReportedResultRow> rows) { this.rows = rows; }
}