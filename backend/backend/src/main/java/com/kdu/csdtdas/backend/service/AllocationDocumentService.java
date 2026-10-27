package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AllocationDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.DepartmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class AllocationDocumentService {

    private static final Pattern THREE_DIGITS = Pattern.compile("(\\d{3})");
    private static final Pattern UNIT = Pattern.compile("^\\s*(\\d{1,2})\\s*$");
    private static final Pattern SEMESTER_TEXT = Pattern.compile("(FIRST|SECOND|1ST|2ND)\\s+SEMESTER");
    private static final Pattern SESSION_TEXT = Pattern.compile("(20\\d{2})\\s*[/\\\\_-]\\s*(20\\d{2}|\\d{2})");
    private static final Pattern PHONE = Pattern.compile("(?:\\+?234|0)[789][01]\\d{8}");
    private static final Pattern SEPARATED_DIGITS = Pattern.compile("(?<=\\d)[\\s-]+(?=\\d)");

    private static final Set<String> TEACHING = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");

    private static final Map<String, List<String>> PROGRAMMES_BY_PREFIX = Map.of(
            "CSC", List.of("CS"),
            "CYB", List.of("CYB"),
            "SEN", List.of("SEN"),
            "IFT", List.of("IFT"),
            "COS", List.of("CS", "SEN", "CYB", "IFT")
    );

    private record Detected(AcademicSession session, String semester) {}

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final LevelRepository levelRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final CourseAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final CourseAllocationService allocationService;

    public AllocationDocumentService(
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            LevelRepository levelRepository,
            ProgrammeRepository programmeRepository,
            AcademicSessionRepository academicSessionRepository,
            CourseAllocationRepository allocationRepository,
            UserRepository userRepository,
            CourseAllocationService allocationService
    ) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.levelRepository = levelRepository;
        this.programmeRepository = programmeRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
        this.allocationService = allocationService;
    }

    // ------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public List<AllocationDTOs.PreviewRow> preview(MultipartFile file) {
        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
            return previewRows(doc);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the Word document.", e);
        }
    }

    // ----------------------------------------------------- one-step import

    /** Reads the document and creates the courses and allocations in one go. */
    public AllocationDTOs.ImportResult importDocument(MultipartFile file, Long sessionId, String semester) {

        List<AllocationDTOs.PreviewRow> rows;
        Detected detected;

        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
            detected = detect(doc);
            rows = previewRows(doc);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the Word document.", e);
        }

        AcademicSession session = sessionId != null
                ? academicSessionRepository.findById(sessionId)
                  .orElseThrow(() -> new IllegalArgumentException("Academic session not found."))
                : detected.session();

        if (session == null) {
            throw new IllegalArgumentException(
                    "The session could not be read from the document. Choose it from the list and try again."
            );
        }

        String cleanSemester = semester != null && !semester.isBlank()
                ? semester.trim().toUpperCase()
                : detected.semester();

        if (cleanSemester == null) {
            throw new IllegalArgumentException(
                    "The semester could not be read from the document. Choose it from the list and try again."
            );
        }

        List<String> notes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<AllocationDTOs.CommitRow> commitRows = new ArrayList<>();

        for (AllocationDTOs.PreviewRow r : rows) {

            for (AllocationDTOs.NewCourse nc : r.newCourses()) {
                if (!seen.add(nc.code())) {
                    notes.add("Row " + r.rowNumber() + ": " + nc.code()
                            + " appears more than once in the document, so it is only created once.");
                }
            }

            commitRows.add(new AllocationDTOs.CommitRow(
                    r.matches().stream().map(AllocationDTOs.MatchedCourse::id).toList(),
                    r.newCourses(), r.lecturers(), r.phones()
            ));
        }

        AllocationDTOs.CommitResult result = commit(
                new AllocationDTOs.CommitRequest(session.getId(), cleanSemester, commitRows)
        );

        List<String> allNotes = new ArrayList<>(notes);
        allNotes.addAll(result.notes());

        return new AllocationDTOs.ImportResult(
                session.getId(), session.getName(), cleanSemester, rows.size(),
                result.coursesCreated(), result.courseSlotsOpened(),
                result.lecturersListed(), result.alreadyListed(),
                allNotes, result.needProgramme()
        );
    }

    // -------------------------------------------------------------- commit

    public AllocationDTOs.CommitResult commit(AllocationDTOs.CommitRequest request) {

        if (request == null || request.rows() == null || request.rows().isEmpty()) {
            throw new IllegalArgumentException("There is nothing to import.");
        }
        if (request.semester() == null || request.semester().isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        String semester = request.semester().trim().toUpperCase();
        AcademicSession session = academicSessionRepository.findById(request.academicSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> TEACHING.contains(String.valueOf(u.getRole()).toUpperCase()))
                .toList();

        Department department = null;
        int created = 0, opened = 0, listed = 0, already = 0;
        List<String> notes = new ArrayList<>();
        List<AllocationDTOs.CourseRef> needProgramme = new ArrayList<>();

        for (AllocationDTOs.CommitRow row : request.rows()) {

            List<Course> courses = new ArrayList<>();

            if (row.courseIds() != null) {
                for (Long id : row.courseIds()) {
                    courseRepository.findById(id).ifPresent(courses::add);
                }
            }

            if (row.newCourses() != null) {
                for (AllocationDTOs.NewCourse nc : row.newCourses()) {

                    String code = nc.code() == null ? "" : norm(nc.code());
                    if (code.isEmpty()) continue;

                    Optional<Course> existing = courseRepository.findByCode(code);
                    if (existing.isPresent()) {
                        Course found = existing.get();
                        if (courses.stream().noneMatch(c -> c.getId().equals(found.getId()))) {
                            courses.add(found);
                        }
                        continue;
                    }
                    if (nc.unit() == null || nc.unit() <= 0) {
                        notes.add(code + " was not created: its unit in the document is missing or unclear. "
                                + "Correct it and upload again, or create the course on the Create Course page.");
                        continue;
                    }
                    if (nc.levelId() == null) {
                        notes.add(code + " was not created: its level could not be worked out from the code.");
                        continue;
                    }

                    Level level = levelRepository.findById(nc.levelId()).orElse(null);
                    if (level == null) {
                        notes.add(code + " was not created: the level was not found.");
                        continue;
                    }

                    if (department == null) {
                        department = departmentRepository.findByCode("CSDT")
                                .orElseThrow(() -> new IllegalArgumentException("Department CSDT not found."));
                    }

                    Set<Programme> programmes = new HashSet<>();
                    if (nc.programmeIds() != null && !nc.programmeIds().isEmpty()) {
                        programmes.addAll(programmeRepository.findAllById(nc.programmeIds()));
                    }

                    Course course = new Course();
                    course.setCode(code);
                    course.setTitle(nc.title() == null || nc.title().isBlank() ? code : nc.title().trim());
                    course.setCreditUnit(nc.unit());
                    course.setStatus(nc.status() != null && nc.status().matches("[CRE]") ? nc.status() : "C");
                    course.setDepartment(department);
                    course.setLevel(level);
                    course.setSemester(semester);
                    course.setProgrammes(programmes);
                    course.setActive(true);

                    Course saved = courseRepository.save(course);
                    courses.add(saved);
                    created++;

                    if (programmes.isEmpty()) {
                        needProgramme.add(new AllocationDTOs.CourseRef(saved.getId(), saved.getCode(), saved.getTitle()));
                    }
                }
            }

            if (courses.isEmpty()) continue;

            List<String> names = row.lecturers() == null ? List.of() : row.lecturers();
            List<String> phones = row.phones() == null ? List.of() : row.phones();

            for (Course course : courses) {

                if (names.isEmpty()) {
                    if (allocationService.ensureOpenSlot(course, session, semester, "DOCUMENT")) opened++;
                    continue;
                }

                for (int i = 0; i < names.size(); i++) {
                    Optional<CourseAllocation> listedRow =
                            allocationService.listName(course, session, semester, names.get(i), "DOCUMENT");

                    if (listedRow.isEmpty()) {
                        already++;
                        continue;
                    }
                    listed++;

                    String phone = i < phones.size() ? phones.get(i) : null;
                    if (phone == null || phone.isBlank()) phone = accountPhone(teachers, names.get(i));

                    if (phone != null) {
                        listedRow.get().setPhoneNumber(phone);
                        allocationRepository.save(listedRow.get());
                    }
                }
            }
        }

        return new AllocationDTOs.CommitResult(created, opened, listed, already, notes, needProgramme);
    }

    // ------------------------------------------------------------- reading

    private List<AllocationDTOs.PreviewRow> previewRows(XWPFDocument doc) {

        Map<String, Course> byCode = new HashMap<>();
        for (Course c : courseRepository.findAll()) {
            byCode.putIfAbsent(norm(c.getCode()), c);
        }
        List<Level> levels = levelRepository.findAll();
        List<Programme> programmes = programmeRepository.findAll();

        XWPFTable table = largestTable(doc);
        if (table == null) {
            throw new IllegalArgumentException("No table was found in this document.");
        }

        List<XWPFTableRow> rows = table.getRows();

        int header = -1, codeCol = -1, titleCol = -1, unitCol = -1, statusCol = -1, lecturerCol = -1, phoneCol = -1;
        for (int r = 0; r < Math.min(6, rows.size()); r++) {
            List<String> cells = texts(rows.get(r));
            int code = indexOf(cells, "CODE");
            if (code >= 0) {
                header = r;
                codeCol = code;
                titleCol = indexOf(cells, "TITLE");
                unitCol = indexOf(cells, "UNIT");
                statusCol = indexOf(cells, "STATUS");
                lecturerCol = indexOf(cells, "LECTURER");
                if (lecturerCol < 0) lecturerCol = indexOf(cells, "NAME");
                phoneCol = indexOf(cells, "PHONE");
                if (phoneCol < 0) phoneCol = indexOf(cells, "CONTACT");
                if (phoneCol < 0) phoneCol = indexOf(cells, "MOBILE");
                break;
            }
        }
        if (header < 0) {
            throw new IllegalArgumentException("Could not find a 'Course Code' column in the table.");
        }

        List<AllocationDTOs.PreviewRow> result = new ArrayList<>();
        String carried = "";

        for (int r = header + 1; r < rows.size(); r++) {

            List<XWPFTableCell> tableCells = rows.get(r).getTableCells();
            List<String> cells = texts(rows.get(r));

            String codeText = cell(cells, codeCol);
            if (codeText.isBlank()) continue;

            String lecturerText = cell(cells, lecturerCol);
            if (!lecturerText.isBlank()) {
                carried = lecturerText;
            } else if (lecturerCol >= 0 && lecturerCol < tableCells.size()
                    && isMergedContinuation(tableCells.get(lecturerCol))) {
                lecturerText = carried;
            } else {
                carried = "";
            }

            List<String> embedded = new ArrayList<>();
            String lecturerClean = extractPhones(lecturerText, embedded);
            List<String> lecturers = splitNames(lecturerClean);
            List<String> phones = alignPhones(lecturers.size(), cell(cells, phoneCol), embedded);

            String titleText = cell(cells, titleCol);
            Integer unit = parseUnit(cell(cells, unitCol));
            String status = parseStatus(cell(cells, statusCol));
            List<String> alternatives = splitAlternatives(codeText);
            List<String> titles = splitAlternatives(titleText);
            List<String> warnings = new ArrayList<>();

            List<Course> matched = new ArrayList<>();
            List<AllocationDTOs.NewCourse> newCourses = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (int i = 0; i < alternatives.size(); i++) {
                String code = norm(alternatives.get(i));
                if (code.length() < 4 || !seen.add(code)) continue;

                Course existing = byCode.get(code);
                if (existing != null) {
                    matched.add(existing);
                    continue;
                }

                String title = i < titles.size() ? titles.get(i) : (titles.isEmpty() ? code : titles.get(0));
                Level level = levelFor(code, levels);
                List<Programme> inferred = programmesFor(code, programmes);

                newCourses.add(new AllocationDTOs.NewCourse(
                        code, title, unit, status == null ? "C" : status,
                        level != null ? level.getId() : null,
                        level != null ? level.getName() : null,
                        inferred.stream().map(Programme::getId).toList(),
                        inferred.stream().map(Programme::getName).toList()
                ));
            }

            if (!newCourses.isEmpty()) {
                if (unit == null) {
                    warnings.add("The unit is missing or unclear in the document. Enter it before creating this course.");
                }
                if (newCourses.get(0).levelId() == null) {
                    warnings.add("The level could not be worked out from the code. Choose it before creating this course.");
                }
                if (newCourses.get(0).programmeIds().isEmpty()) {
                    warnings.add("The programme could not be worked out from the code. Tick the right ones, or students will not see this course.");
                }
            }

            List<AllocationDTOs.MatchedCourse> matches = matched.stream()
                    .map(c -> new AllocationDTOs.MatchedCourse(c.getId(), c.getCode(), c.getTitle()))
                    .toList();

            result.add(new AllocationDTOs.PreviewRow(
                    r, codeText, titleText, unit, status, lecturers, phones, matches, newCourses, warnings
            ));
        }

        return result;
    }

    /** Reads "FIRST semester 2026/2027" from the text above the table. */
    private Detected detect(XWPFDocument doc) {

        StringBuilder text = new StringBuilder();
        for (var paragraph : doc.getParagraphs()) {
            text.append(paragraph.getText()).append(' ');
        }
        String upper = text.toString().toUpperCase();

        String semester = null;
        Matcher sm = SEMESTER_TEXT.matcher(upper);
        if (sm.find()) {
            String word = sm.group(1);
            semester = word.startsWith("F") || word.startsWith("1") ? "FIRST" : "SECOND";
        }

        AcademicSession session = null;
        Matcher ym = SESSION_TEXT.matcher(upper);
        if (ym.find()) {
            String first = ym.group(1);
            String second = ym.group(2).length() == 2 ? first.substring(0, 2) + ym.group(2) : ym.group(2);
            String key = first + second;

            for (AcademicSession s : academicSessionRepository.findAll()) {
                if (s.getName() != null && s.getName().replaceAll("\\D", "").equals(key)) {
                    session = s;
                    break;
                }
            }
        }

        return new Detected(session, semester);
    }

    // ------------------------------------------------------------- helpers

    private String accountPhone(List<User> teachers, String listedName) {
        List<User> hits = teachers.stream()
                .filter(u -> NameMatcher.matches(listedName, u.getFullName()))
                .toList();

        if (hits.size() != 1) return null;

        String phone = hits.get(0).getPhoneNumber();
        return phone == null || phone.isBlank() ? null : phone;
    }

    /** Removes phone numbers from the text and returns them separately. */
    private String extractPhones(String text, List<String> found) {
        if (text == null) return "";

        String squeezed = SEPARATED_DIGITS.matcher(text).replaceAll("");
        Matcher m = PHONE.matcher(squeezed);
        StringBuilder out = new StringBuilder();

        while (m.find()) {
            found.add(m.group());
            m.appendReplacement(out, " ");
        }
        m.appendTail(out);

        return out.toString().replaceAll("\\(\\s*\\)", " ");
    }

    /** One phone entry per lecturer: matched by position, or shared when there is a single lecturer. */
    private List<String> alignPhones(int nameCount, String phoneCell, List<String> embedded) {
        List<String> found = new ArrayList<>();

        if (phoneCell != null && !phoneCell.isBlank()) {
            Matcher m = PHONE.matcher(SEPARATED_DIGITS.matcher(phoneCell).replaceAll(""));
            while (m.find()) found.add(m.group());
        }
        found.addAll(embedded);

        List<String> out = new ArrayList<>();
        for (int i = 0; i < nameCount; i++) {
            if (found.size() == nameCount) out.add(found.get(i));
            else if (nameCount == 1 && !found.isEmpty()) out.add(found.get(0));
            else out.add(null);
        }
        return out;
    }

    private XWPFTable largestTable(XWPFDocument doc) {
        XWPFTable best = null;
        for (XWPFTable t : doc.getTables()) {
            if (best == null || t.getRows().size() > best.getRows().size()) best = t;
        }
        return best;
    }

    private List<String> texts(XWPFTableRow row) {
        List<String> out = new ArrayList<>();
        for (XWPFTableCell c : row.getTableCells()) {
            out.add(c.getText() == null ? "" : c.getText().trim());
        }
        return out;
    }

    private int indexOf(List<String> cells, String keyword) {
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).toUpperCase().contains(keyword)) return i;
        }
        return -1;
    }

    private String cell(List<String> cells, int col) {
        return col >= 0 && col < cells.size() ? cells.get(col) : "";
    }

    /** A cell that continues a vertically merged cell above it (Word leaves these empty). */
    private boolean isMergedContinuation(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        if (properties == null) return false;
        String xml = properties.xmlText();
        return xml.contains("vMerge") && !xml.contains("restart");
    }

    private Integer parseUnit(String text) {
        Matcher m = UNIT.matcher(text == null ? "" : text);
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    private String parseStatus(String text) {
        String t = text == null ? "" : text.trim().toUpperCase();
        return t.matches("[CRE]") ? t : null;
    }

    private List<String> splitAlternatives(String text) {
        List<String> out = new ArrayList<>();
        for (String part : (text == null ? "" : text).split("[/\\n\\r]+")) {
            String t = part.trim().replaceAll("\\s+", " ");
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private List<String> splitNames(String text) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String part : (text == null ? "" : text).split("[/\\n\\r;]+")) {
            String name = part.trim().replaceAll("\\s+", " ");
            if (name.isEmpty() || NameMatcher.tokens(name).isEmpty()) continue;
            if (seen.add(NameMatcher.normalize(name))) out.add(name);
        }
        return out;
    }

    /** Upper-case letters and digits only: "KDU-CYB 311" becomes "KDUCYB311". The KDU prefix is kept. */
    private String norm(String code) {
        return code == null ? "" : code.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private Level levelFor(String code, List<Level> levels) {
        Matcher m = THREE_DIGITS.matcher(code);
        if (!m.find()) return null;
        int number = (m.group(1).charAt(0) - '0') * 100;
        return levels.stream()
                .filter(l -> l.getLevelNumber() != null && l.getLevelNumber() == number)
                .findFirst().orElse(null);
    }

    private List<Programme> programmesFor(String code, List<Programme> programmes) {
        String letters = code.replaceFirst("^KDU(?=[A-Z])", "").replaceAll("[0-9].*$", "");
        List<String> wanted = PROGRAMMES_BY_PREFIX.getOrDefault(letters, List.of());
        return programmes.stream().filter(p -> wanted.contains(p.getCode().toUpperCase())).toList();
    }
}