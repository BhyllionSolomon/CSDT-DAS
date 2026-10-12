package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AllocationDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.DepartmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
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

    private static final Map<String, List<String>> PROGRAMMES_BY_PREFIX = Map.of(
            "CSC", List.of("CS"),
            "CYB", List.of("CYB"),
            "SEN", List.of("SEN"),
            "IFT", List.of("IFT"),
            "COS", List.of("CS", "SEN", "CYB", "IFT")
    );

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final LevelRepository levelRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final CourseAllocationService allocationService;

    public AllocationDocumentService(
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            LevelRepository levelRepository,
            ProgrammeRepository programmeRepository,
            AcademicSessionRepository academicSessionRepository,
            CourseAllocationService allocationService
    ) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.levelRepository = levelRepository;
        this.programmeRepository = programmeRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.allocationService = allocationService;
    }

    // ------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public List<AllocationDTOs.PreviewRow> preview(MultipartFile file) {

        Map<String, Course> byCode = new HashMap<>();
        for (Course c : courseRepository.findAll()) {
            byCode.putIfAbsent(norm(c.getCode()), c);
        }
        List<Level> levels = levelRepository.findAll();
        List<Programme> programmes = programmeRepository.findAll();

        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {

            XWPFTable table = largestTable(doc);
            if (table == null) {
                throw new IllegalArgumentException("No table was found in this document.");
            }

            List<XWPFTableRow> rows = table.getRows();

            int header = -1, codeCol = -1, titleCol = -1, unitCol = -1, statusCol = -1, lecturerCol = -1;
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

                String titleText = cell(cells, titleCol);
                Integer unit = parseUnit(cell(cells, unitCol));
                String status = parseStatus(cell(cells, statusCol));
                List<String> lecturers = splitNames(lecturerText);
                List<String> alternatives = splitAlternatives(codeText);
                List<String> warnings = new ArrayList<>();

                List<Course> matched = new ArrayList<>();
                List<String> unmatched = new ArrayList<>();
                for (String alt : alternatives) {
                    Course course = byCode.get(norm(alt));
                    if (course == null) {
                        unmatched.add(alt);
                    } else if (matched.stream().noneMatch(m -> m.getId().equals(course.getId()))) {
                        matched.add(course);
                    }
                }

                List<AllocationDTOs.MatchedCourse> matches = matched.stream()
                        .map(c -> new AllocationDTOs.MatchedCourse(c.getId(), c.getCode(), c.getTitle()))
                        .toList();

                List<AllocationDTOs.NewCourse> newCourses = new ArrayList<>();

                if (matched.isEmpty()) {
                    List<String> titles = splitAlternatives(titleText);
                    Set<String> seen = new HashSet<>();

                    for (int i = 0; i < alternatives.size(); i++) {
                        String code = norm(alternatives.get(i));
                        if (code.length() < 4 || !seen.add(code)) continue;

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

                    if (unit == null) {
                        warnings.add("The unit is missing or unclear in the document. Enter it before creating this course.");
                    }
                    if (!newCourses.isEmpty() && newCourses.get(0).levelId() == null) {
                        warnings.add("The level could not be worked out from the code. Choose it before creating this course.");
                    }
                    if (!newCourses.isEmpty() && newCourses.get(0).programmeIds().isEmpty()) {
                        warnings.add("The programme could not be worked out from the code. Tick the right ones, or students will not see this course.");
                    }
                } else if (!unmatched.isEmpty()) {
                    warnings.add("Alternative code(s) not in the system: " + String.join(", ", unmatched)
                            + ". The allocation applies to the course(s) that matched.");
                }

                result.add(new AllocationDTOs.PreviewRow(
                        r, codeText, titleText, unit, status, lecturers, matches, newCourses, warnings
                ));
            }

            return result;

        } catch (IOException e) {
            throw new IllegalStateException("Could not read the Word document.", e);
        }
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

        Department department = null;
        int created = 0, opened = 0, listed = 0, already = 0;
        List<String> notes = new ArrayList<>();

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
                        courses.add(existing.get());
                        continue;
                    }
                    if (nc.unit() == null || nc.unit() <= 0) {
                        notes.add(code + " was not created: no credit unit was given.");
                        continue;
                    }
                    if (nc.levelId() == null) {
                        notes.add(code + " was not created: no level was chosen.");
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

                    courses.add(courseRepository.save(course));
                    created++;
                }
            }

            if (courses.isEmpty()) continue;

            List<String> names = row.lecturers() == null ? List.of() : row.lecturers();

            for (Course course : courses) {
                if (names.isEmpty()) {
                    if (allocationService.ensureOpenSlot(course, session, semester, "DOCUMENT")) opened++;
                } else {
                    for (String name : names) {
                        if (allocationService.listName(course, session, semester, name, "DOCUMENT").isPresent()) {
                            listed++;
                        } else {
                            already++;
                        }
                    }
                }
            }
        }

        return new AllocationDTOs.CommitResult(created, opened, listed, already, notes);
    }

    // ------------------------------------------------------------- parsing

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