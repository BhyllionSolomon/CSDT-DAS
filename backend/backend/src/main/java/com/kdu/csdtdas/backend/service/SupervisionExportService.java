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
        int[] widths = {3400, 3600, 2600, 3800, 1200};
        String[] heads = {"Supervisor", "Student name", "Matriculation number", "Department", "CGPA"};

        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");

        x.append(paragraph("KOLA DAISI UNIVERSITY", true, 28, "center"));
        x.append(paragraph("DEPARTMENT OF COMPUTING SCIENCE AND DIGITAL TECHNOLOGY", true, 24, "center"));
        x.append(paragraph("FINAL-YEAR PROJECT SUPERVISION ALLOCATION, " + sessionName + " SESSION", true, 24, "center"));
        x.append(paragraph(" ", false, 20, "left"));

        x.append("<w:tbl><w:tblPr><w:tblW w:w=\"14600\" w:type=\"dxa\"/><w:tblBorders>");
        for (String side : new String[]{"top", "left", "bottom", "right", "insideH", "insideV"}) {
            x.append("<w:").append(side).append(" w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"000000\"/>");
        }
        x.append("</w:tblBorders></w:tblPr><w:tblGrid>");
        for (int w : widths) x.append("<w:gridCol w:w=\"").append(w).append("\"/>");
        x.append("</w:tblGrid>");

        x.append("<w:tr><w:trPr><w:tblHeader/></w:trPr>");
        for (int i = 0; i < heads.length; i++) x.append(cell(heads[i], widths[i], true));
        x.append("</w:tr>");

        for (Row r : rows) {
            String[] values = {r.supervisor(), r.student(), r.matric(), r.department(), r.cgpa()};
            x.append("<w:tr>");
            for (int i = 0; i < values.length; i++) x.append(cell(values[i], widths[i], false));
            x.append("</w:tr>");
        }
        x.append("</w:tbl>");

        x.append(paragraph(" ", false, 20, "left"));
        x.append(paragraph(rows.size() + " student(s). Generated on " + LocalDate.now() + ".", false, 18, "left"));

        x.append("<w:sectPr><w:pgSz w:w=\"16838\" w:h=\"11906\" w:orient=\"landscape\"/>")
                .append("<w:pgMar w:top=\"1000\" w:right=\"1000\" w:bottom=\"1000\" w:left=\"1000\" w:header=\"500\" w:footer=\"500\" w:gutter=\"0\"/></w:sectPr>")
                .append("</w:body></w:document>");
        return x.toString();
    }

    private String paragraph(String text, boolean bold, int size, String align) {
        return "<w:p><w:pPr><w:jc w:val=\"" + align + "\"/></w:pPr><w:r><w:rPr>" + (bold ? "<w:b/>" : "")
                + "<w:sz w:val=\"" + size + "\"/></w:rPr><w:t xml:space=\"preserve\">" + esc(text) + "</w:t></w:r></w:p>";
    }

    private String cell(String text, int width, boolean header) {
        return "<w:tc><w:tcPr><w:tcW w:w=\"" + width + "\" w:type=\"dxa\"/>"
                + (header ? "<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"D9E2F3\"/>" : "")
                + "</w:tcPr><w:p><w:r><w:rPr>" + (header ? "<w:b/>" : "") + "<w:sz w:val=\"20\"/></w:rPr><w:t xml:space=\"preserve\">"
                + esc(text) + "</w:t></w:r></w:p></w:tc>";
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