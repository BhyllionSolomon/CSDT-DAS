package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.service.ExportService;
import com.kdu.csdtdas.backend.service.ResultCalculationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;
    private final ResultCalculationService resultCalculationService;

    public ExportController(
            ExportService exportService,
            ResultCalculationService resultCalculationService
    ) {
        this.exportService = exportService;
        this.resultCalculationService = resultCalculationService;
    }

    @GetMapping("/student/{studentId}/excel")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable Long studentId
    ) {

        List<SemesterResultDTO> history =
                resultCalculationService.calculateFullAcademicHistory(studentId);

        byte[] file = exportService.exportToExcel(history);

        return buildFileResponse(
                file,
                "academic-history-" + studentId + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    @GetMapping("/student/{studentId}/word")
    public ResponseEntity<byte[]> exportWord(
            @PathVariable Long studentId
    ) {

        List<SemesterResultDTO> history =
                resultCalculationService.calculateFullAcademicHistory(studentId);

        byte[] file = exportService.exportToWord(history);

        return buildFileResponse(
                file,
                "academic-history-" + studentId + ".docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
    }

    @GetMapping("/student/{studentId}/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long studentId
    ) {

        List<SemesterResultDTO> history =
                resultCalculationService.calculateFullAcademicHistory(studentId);

        byte[] file = exportService.exportToPdf(history);

        return buildFileResponse(
                file,
                "academic-history-" + studentId + ".pdf",
                "application/pdf"
        );
    }

    private ResponseEntity<byte[]> buildFileResponse(
            byte[] file,
            String filename,
            String contentType
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", filename);
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length)
                .body(file);
    }
}