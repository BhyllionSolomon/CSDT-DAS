package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.ReportedCgpa;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.ReportedCgpaRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class ReportedCgpaPdfService {

    private static final Pattern MATRIC_PATTERN =
            Pattern.compile("\\bKDU[A-Z0-9]{6,15}\\b");
    private static final Pattern DECIMAL_PATTERN =
            Pattern.compile("\\d+\\.\\d{2}");

    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final ReportedCgpaRepository reportedCgpaRepository;

    public ReportedCgpaPdfService(
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository academicSessionRepository,
            ReportedCgpaRepository reportedCgpaRepository
    ) {
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.reportedCgpaRepository = reportedCgpaRepository;
    }

    public static class ParsedRow {
        public String matricNumber;
        public String fullName;
        public BigDecimal cgpa;

        public ParsedRow(String matricNumber, String fullName, BigDecimal cgpa) {
            this.matricNumber = matricNumber;
            this.fullName = fullName;
            this.cgpa = cgpa;
        }
    }

    /**
     * Extracts matric number, name and CGPA from a broadsheet-style PDF.
     * Does not save anything — used for the preview/download-as-CSV step.
     */
    public List<ParsedRow> extractRows(MultipartFile file) {

        List<ParsedRow> rows = new ArrayList<>();

        try (var in = file.getInputStream()) {
            PdfReader reader = new PdfReader(in);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);

            StringBuilder allText = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                allText.append(extractor.getTextFromPage(page)).append("\n");
            }
            reader.close();

            String[] lines = allText.toString().split("\\r?\\n");

            for (String line : lines) {
                Matcher matricMatcher = MATRIC_PATTERN.matcher(line);
                if (!matricMatcher.find()) continue;

                String matric = matricMatcher.group().toUpperCase();

                String beforeMatric = line.substring(0, matricMatcher.start()).trim();
                String name = beforeMatric.replaceFirst("^\\d+\\s+", "").trim();

                String afterMatric = line.substring(matricMatcher.end());
                Matcher decimalMatcher = DECIMAL_PATTERN.matcher(afterMatric);

                BigDecimal cgpa = null;
                while (decimalMatcher.find()) {
                    BigDecimal candidate = new BigDecimal(decimalMatcher.group());
                    if (candidate.compareTo(BigDecimal.ZERO) >= 0
                            && candidate.compareTo(BigDecimal.valueOf(5.00)) <= 0) {
                        cgpa = candidate;
                    }
                }

                if (cgpa != null && !name.isEmpty()) {
                    rows.add(new ParsedRow(matric, name, cgpa));
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read PDF file.", e);
        }

        return rows;
    }

    public int saveRows(
            Long programmeId, Long levelId, Long academicSessionId,
            List<ParsedRow> rows, String uploadedBy
    ) {
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));
        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        int saved = 0;
        for (ParsedRow row : rows) {
            ReportedCgpa entity = new ReportedCgpa();
            entity.setMatricNumber(row.matricNumber);
            entity.setFullName(row.fullName);
            entity.setProgramme(programme);
            entity.setLevel(level);
            entity.setAcademicSession(session);
            entity.setCgpa(row.cgpa);
            entity.setUploadedBy(uploadedBy);
            reportedCgpaRepository.save(entity);
            saved++;
        }
        return saved;
    }

    public void checkAdviserAuthority(String role, Long userProgrammeId, Long targetProgrammeId) {
        if ("ADMIN".equalsIgnoreCase(role) || "HOD".equalsIgnoreCase(role)) return;

        if ("LEVEL_ADVISER".equalsIgnoreCase(role)) {
            if (userProgrammeId == null || !userProgrammeId.equals(targetProgrammeId)) {
                throw new IllegalArgumentException("You can only upload results for your own programme.");
            }
            return;
        }

        throw new IllegalArgumentException("You do not have permission to upload results.");
    }

    @Transactional(readOnly = true)
    public List<ReportedCgpa> getRoster(Long programmeId, Long levelId, Long academicSessionId) {
        return reportedCgpaRepository.findByProgrammeAndLevelAndSession(programmeId, levelId, academicSessionId);
    }
}