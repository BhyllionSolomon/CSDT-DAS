package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.ProjectSupervision;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.ProjectSupervisionRepository;
import com.kdu.csdtdas.backend.repository.StaffProfileRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** The final-year supervisor allocation as a Word document: Supervisor, Student name, Matric number, Department, CGPA. */
@Service
@Transactional(readOnly = true)
public class SupervisionExportService {

    public record Row(String supervisor, String student, String matric, String department, String cgpa) {}

    public record Preview(String session, int count, List<Row> rows) {}

    private static final String DEFAULT_DEPARTMENT = "Computing Science and Digital Technology";

    private final ProjectSupervisionRepository repository;
    private final AcademicSessionRepository sessionRepository;
    private final StaffProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final RegistrationClearanceService clearanceService;

    public SupervisionExportService(
            ProjectSupervisionRepository repository,
            AcademicSessionRepository sessionRepository,
            StaffProfileRepository profileRepository,
            UserRepository userRepository,
            RegistrationClearanceService clearanceService
    ) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.clearanceService = clearanceService;
    }

    public Preview preview(String username, Long sessionId) {
        requireAllowed(username);
        AcademicSession session = session(sessionId);
        List<Row> rows = rows(session);
        return new Preview(session.getName(), rows.size(), rows);
    }

    public byte[] word(String username, Long sessionId) throws IOException {
        requireAllowed(username);
        AcademicSession session = session(sessionId);
        return docx(session.getName(), rows(session));
    }

    public String fileName(Long sessionId) {
        return "supervision-allocation-" + session(sessionId).getName().replace('/', '-') + ".docx";
    }

    // ---------------------------------------------------------------- data

    private List<Row> rows(AcademicSession session) {
        return repository.findForSession(session.getId()).stream().map(p -> new Row(
                p.getSupervisor().getFullName(),
                p.getStudent().getFullName(),
                p.getStudent().getMatricNumber(),
                p.getStudent().getDepartment() != null ? p.getStudent().getDepartment().getName() : DEFAULT_DEPARTMENT,
                cgpa(p)
        )).toList();
    }

    private String cgpa(ProjectSupervision p) {
        return p.getCgpa() == null ? "—" : p.getCgpa().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ---------------------------------------------------------------- Word

    private byte[] docx(String sessionName, List<Row> rows) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                            + "</Types>");
            put(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                            + "</Relationships>");
            put(zip, "word/document.xml", document(sessionName, rows));
        }
        return out.toByteArray();
    }

    private String document(String sessionName, List<Row> rows) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");

        x.append(paragraph("KOLA DAISI UNIVERSITY", true, 32, "center", "17365D", 120));
        x.append(paragraph("DEPARTMENT OF COMPUTING SCIENCE AND DIGITAL TECHNOLOGY", true, 22, "center", "287C91", 100));
        x.append(paragraph("FINAL-YEAR PROJECT SUPERVISOR–SUPERVISEE ALLOCATION", true, 27, "center", "17365D", 70));
        x.append(paragraph(sessionName + " ACADEMIC SESSION", true, 22, "center", "287C91", 220));
        x.append(paragraph("This document presents the approved project supervision allocation for the academic session.", false, 19, "center", "555555", 180));

        int supervisors = (int) rows.stream().map(Row::supervisor).distinct().count();
        x.append(summaryTable(rows.size(), supervisors));
        x.append(paragraph(" ", false, 12, "left", "FFFFFF", 100));

        Map<String, List<Row>> grouped = new LinkedHashMap<>();
        for (Row row : rows) grouped.computeIfAbsent(row.supervisor(), key -> new java.util.ArrayList<>()).add(row);

        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            x.append(supervisorHeading(entry.getKey(), entry.getValue().size()));
            x.append(studentTable(entry.getValue()));
            x.append(paragraph(" ", false, 10, "left", "FFFFFF", 100));
        }

        x.append(paragraph("TOTAL: " + rows.size() + " STUDENT(S)  |  " + supervisors + " SUPERVISOR(S)", true, 20, "left", "17365D", 100));
        x.append(paragraph("Generated on " + LocalDate.now() + ".", false, 16, "left", "666666", 80));
        x.append(paragraph("For departmental use and record.", false, 16, "left", "666666", 180));

        x.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
                .append("<w:pgMar w:top=\"850\" w:right=\"850\" w:bottom=\"850\" w:left=\"850\" w:header=\"400\" w:footer=\"400\" w:gutter=\"0\"/></w:sectPr>")
                .append("</w:body></w:document>");
        return x.toString();
    }

    private String supervisorHeading(String supervisor, int count) {
        String[] details = supervisorDetails(supervisor);
        return "<w:p><w:pPr><w:spacing w:before=\"180\" w:after=\"60\"/><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"DCEAF2\"/>"
                + "<w:ind w:left=\"100\" w:right=\"100\"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val=\"17365D\"/><w:sz w:val=\"22\"/></w:rPr><w:t>"
                + esc(details[0]) + "</w:t></w:r><w:r><w:rPr><w:color w:val=\"287C91\"/><w:sz w:val=\"18\"/></w:rPr><w:t xml:space=\"preserve\">  "
                + esc(details[1]) + "  |  " + count + " student(s)</w:t></w:r></w:p>";
    }

    private String[] supervisorDetails(String supervisorName) {
        User supervisor = userRepository.findAll().stream()
                .filter(u -> supervisorName.equals(u.getFullName()))
                .findFirst().orElse(null);
        if (supervisor == null) return new String[]{supervisorName, "Project Supervisor"};

        var profile = profileRepository.findByUserId(supervisor.getId()).orElse(null);
        if (profile == null) return new String[]{supervisorName, "Project Supervisor"};

        String designation = profile.getDesignation() == null ? "" : profile.getDesignation().toUpperCase();
        String prefix = switch (designation) {
            case "PROFESSOR" -> "Prof. ";
            case "ASSOCIATE_PROFESSOR" -> "Associate Prof. ";
            case "SENIOR_LECTURER" -> "Senior Lecturer ";
            case "LECTURER_I" -> "Lecturer I ";
            case "LECTURER_II" -> "Lecturer II ";
            case "ASSISTANT_LECTURER" -> "Assistant Lecturer ";
            case "ADJUNCT" -> "Adjunct Lecturer ";
            default -> "";
        };
        String name = supervisorName;
        if (!prefix.isBlank() && !supervisorName.matches("(?i)^(prof\\.?|professor|dr\\.?|mr\\.?|mrs\\.?|miss|associate prof\\.?|senior lecturer|lecturer|assistant lecturer|adjunct lecturer).*")) {
            name = prefix + supervisorName;
        }
        String role = profile.isProjectCoordinator() ? "Project Coordinator"
                : profile.isAssistantCoordinator() ? "Assistant Project Coordinator"
                : designation.equals("HEAD_OF_DEPARTMENT") ? "Head of Department"
                : "Project Supervisor";
        return new String[]{name, role};
    }

    private String summaryTable(int studentCount, int supervisorCount) {
        StringBuilder s = new StringBuilder("<w:tbl><w:tblPr><w:tblW w:w=\"5000\" w:type=\"dxa\"/><w:tblBorders>");
        for (String side : new String[]{"top", "left", "bottom", "right", "insideH", "insideV"}) {
            s.append("<w:").append(side).append(" w:val=\"single\" w:sz=\"4\" w:color=\"B7C9D6\"/>");
        }
        s.append("</w:tblBorders></w:tblPr><w:tblGrid><w:gridCol w:w=\"2500\"/><w:gridCol w:w=\"2500\"/></w:tblGrid><w:tr>")
                .append(cell("TOTAL STUDENTS", 2500, true, "DCEAF2"))
                .append(cell("SUPERVISORS", 2500, true, "DCEAF2"))
                .append("</w:tr><w:tr>")
                .append(cell(String.valueOf(studentCount), 2500, false, "F3F7FA"))
                .append(cell(String.valueOf(supervisorCount), 2500, false, "F3F7FA"))
                .append("</w:tr></w:tbl>");
        return s.toString();
    }

    private String studentTable(List<Row> rows) {
        int[] widths = {600, 3400, 2200, 3000, 800};
        String[] heads = {"S/N", "Student name", "Matriculation number", "Department", "CGPA"};
        StringBuilder x = new StringBuilder("<w:tbl><w:tblPr><w:tblW w:w=\"12350\" w:type=\"dxa\"/><w:tblBorders>");
        for (String side : new String[]{"top", "left", "bottom", "right", "insideH", "insideV"}) {
            x.append("<w:").append(side).append(" w:val=\"single\" w:sz=\"4\" w:color=\"C7D5DF\"/>");
        }
        x.append("</w:tblBorders></w:tblPr><w:tblGrid>");
        for (int width : widths) x.append("<w:gridCol w:w=\"").append(width).append("\"/>");
        x.append("</w:tblGrid><w:tr><w:trPr><w:tblHeader/></w:trPr>");
        for (int i = 0; i < heads.length; i++) x.append(cell(heads[i], widths[i], true, "287C91"));
        x.append("</w:tr>");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = rows.get(rowIndex);
            String fill = rowIndex % 2 == 0 ? "FFFFFF" : "F3F7FA";
            String[] values = {String.valueOf(rowIndex + 1), row.student(), row.matric(), row.department(), row.cgpa()};
            x.append("<w:tr>");
            for (int i = 0; i < values.length; i++) x.append(cell(values[i], widths[i], false, fill));
            x.append("</w:tr>");
        }
        return x.append("</w:tbl>").toString();
    }

    private String paragraph(String text, boolean bold, int size, String align, String colour, int after) {
        return "<w:p><w:pPr><w:jc w:val=\"" + align + "\"/><w:spacing w:after=\"" + after + "\"/></w:pPr><w:r><w:rPr>"
                + (bold ? "<w:b/>" : "") + "<w:color w:val=\"" + colour + "\"/><w:sz w:val=\"" + size + "\"/></w:rPr><w:t xml:space=\"preserve\">"
                + esc(text) + "</w:t></w:r></w:p>";
    }

    private String cell(String text, int width, boolean header, String fill) {
        return "<w:tc><w:tcPr><w:tcW w:w=\"" + width + "\" w:type=\"dxa\"/><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + fill + "\"/>"
                + "</w:tcPr><w:p><w:r><w:rPr>" + (header ? "<w:b/><w:color w:val=\"FFFFFF\"/>" : "<w:color w:val=\"263746\"/>")
                + "<w:sz w:val=\"18\"/></w:rPr><w:t xml:space=\"preserve\">" + esc(text) + "</w:t></w:r></w:p></w:tc>";
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    // ----------------------------------------------------------------- rules

    private AcademicSession session(Long id) {
        if (id != null) return sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Academic session not found."));
        AcademicSession current = clearanceService.currentSession();
        if (current == null) throw new IllegalArgumentException("No academic session has been set up yet.");
        return current;
    }

    /** The H.O.D, admin, Project Coordinator and Assistant Project Coordinator. */
    private void requireAllowed(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (role.equals("HOD") || role.equals("ADMIN")) return;

        boolean coordinator = profileRepository.findByUserId(user.getId())
                .map(p -> p.isProjectCoordinator() || p.isAssistantCoordinator()).orElse(false);
        if (!coordinator) throw new IllegalArgumentException("Only the H.O.D or a Project Coordinator can export the allocation.");
    }
}