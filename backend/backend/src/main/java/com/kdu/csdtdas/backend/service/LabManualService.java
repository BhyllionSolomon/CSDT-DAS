package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LabDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.LabManual;
import com.kdu.csdtdas.backend.entity.LabManualPurchase;
import com.kdu.csdtdas.backend.entity.LabManualSale;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.LabManualPurchaseRepository;
import com.kdu.csdtdas.backend.repository.LabManualRepository;
import com.kdu.csdtdas.backend.repository.LabManualSaleRepository;
import com.kdu.csdtdas.backend.repository.ResultStatsRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional
public class LabManualService {

    private static final Set<String> LAB = Set.of("LAB_OFFICER", "TECHNICAL_STAFF", "HOD", "ADMIN");
    private static final Set<String> FINANCE = Set.of("LAB_OFFICER", "HOD", "ADMIN");
    private static final Set<String> HOD = Set.of("HOD", "ADMIN");
    private static final BigDecimal PASS_MARK = BigDecimal.valueOf(40);
    private static final int MIN_GROUP = 5;

    private final LabManualRepository manualRepository;
    private final LabManualPurchaseRepository purchaseRepository;
    private final LabManualSaleRepository saleRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final ResultStatsRepository resultRepository;
    private final RegistrationClearanceService clearanceService;
    private final UserRepository userRepository;

    public LabManualService(
            LabManualRepository manualRepository,
            LabManualPurchaseRepository purchaseRepository,
            LabManualSaleRepository saleRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            AcademicSessionRepository sessionRepository,
            CourseRegistrationRepository registrationRepository,
            ResultStatsRepository resultRepository,
            RegistrationClearanceService clearanceService,
            UserRepository userRepository
    ) {
        this.manualRepository = manualRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleRepository = saleRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
        this.resultRepository = resultRepository;
        this.clearanceService = clearanceService;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------- manuals

    @Transactional(readOnly = true)
    public List<LabDTOs.CourseHit> courses(String username) {
        require(username, LAB);
        return courseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .sorted(Comparator.comparing(Course::getCode))
                .map(c -> new LabDTOs.CourseHit(c.getId(), c.getCode(), c.getTitle()))
                .toList();
    }

    public List<LabDTOs.ManualRow> manuals(String username) {
        User user = require(username, LAB);
        return manualRows(FINANCE.contains(role(user)));
    }

    public LabDTOs.ManualRow saveManual(String username, LabDTOs.ManualRequest req) {
        require(username, FINANCE);

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty() || title.length() > 150) throw bad("Enter the manual's title.");
        if (req.unitPrice() == null || req.unitPrice().signum() <= 0) throw bad("Enter a selling price greater than zero.");

        LabManual m = req.id() == null
                ? new LabManual()
                : manualRepository.findById(req.id()).orElseThrow(() -> bad("Manual not found."));

        m.setTitle(title);
        m.setUnitPrice(req.unitPrice().setScale(2, RoundingMode.HALF_UP));
        m.setActive(req.active() == null || req.active());
        m.setCourse(req.courseId() == null ? null
                : courseRepository.findById(req.courseId()).orElseThrow(() -> bad("Course not found.")));

        LabManual saved = manualRepository.save(m);
        return manualRows(true).stream().filter(r -> r.id().equals(saved.getId())).findFirst().orElseThrow();
    }

    public LabDTOs.Message addPurchase(String username, Long manualId, LabDTOs.PurchaseRequest req) {
        User user = require(username, FINANCE);
        LabManual m = manualRepository.findById(manualId).orElseThrow(() -> bad("Manual not found."));

        if (req.quantity() == null || req.quantity() < 1 || req.quantity() > 10000) throw bad("Enter how many copies were added.");
        if (req.unitCost() == null || req.unitCost().signum() <= 0) throw bad("Enter what each copy cost.");

        LocalDate date;
        try {
            date = req.date() == null || req.date().isBlank() ? LocalDate.now() : LocalDate.parse(req.date());
        } catch (DateTimeParseException e) {
            throw bad("Enter the date properly.");
        }
        if (date.isAfter(LocalDate.now())) throw bad("The date cannot be in the future.");

        LabManualPurchase p = new LabManualPurchase();
        p.setManual(m);
        p.setQuantity(req.quantity());
        p.setUnitCost(req.unitCost().setScale(2, RoundingMode.HALF_UP));
        p.setPurchasedOn(date);
        p.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        p.setRecordedBy(user.getUsername());
        purchaseRepository.save(p);

        return new LabDTOs.Message(req.quantity() + " copies of " + m.getTitle() + " added to stock.");
    }

    // ------------------------------------------------------------------ sales

    /** Name, matric number, programme and level only. Never any academic data. */
    @Transactional(readOnly = true)
    public List<LabDTOs.StudentHit> searchStudents(String username, String q) {
        require(username, LAB);
        String term = q == null ? "" : q.trim().toLowerCase();
        if (term.length() < 2) return List.of();

        return studentRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getMatricNumber().toLowerCase().contains(term) || s.getFullName().toLowerCase().contains(term))
                .sorted(Comparator.comparing(Student::getFullName))
                .limit(10)
                .map(s -> new LabDTOs.StudentHit(
                        s.getId(), s.getMatricNumber(), s.getFullName(),
                        s.getProgramme() != null ? s.getProgramme().getName() : "",
                        s.getLevel() != null ? s.getLevel().getName() : ""))
                .toList();
    }

    public LabDTOs.Message recordSale(String username, LabDTOs.SaleRequest req) {
        User user = require(username, LAB);

        LabManual m = manualRepository.findById(req.manualId()).orElseThrow(() -> bad("Choose a manual."));
        if (!m.isActive()) throw bad("This manual is not on sale.");
        if (stockOf(m.getId()) < 1) throw bad("There are no copies of " + m.getTitle() + " left in stock.");

        Student s = studentRepository.findById(req.studentId()).orElseThrow(() -> bad("Choose a student."));
        if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) throw bad("This student is not active.");

        AcademicSession session = clearanceService.currentSession();
        if (session == null) throw bad("No academic session has been set up yet.");

        if (saleRepository.existsByManualIdAndStudentIdAndAcademicSessionIdAndVoidedFalse(m.getId(), s.getId(), session.getId())) {
            throw bad(s.getFullName() + " has already bought this manual this session. Void the earlier sale first if it was a mistake.");
        }

        LabManualSale sale = new LabManualSale();
        sale.setManual(m);
        sale.setStudent(s);
        sale.setAcademicSession(session);
        sale.setUnitPrice(m.getUnitPrice());
        sale.setQuantity(1);
        sale.setSoldAt(LocalDateTime.now());
        sale.setRecordedBy(user.getUsername());
        saleRepository.save(sale);

        return new LabDTOs.Message("Sold " + m.getTitle() + " to " + s.getFullName() + " for " + m.getUnitPrice().toPlainString() + ".");
    }

    @Transactional(readOnly = true)
    public List<LabDTOs.SaleRow> sales(String username) {
        require(username, LAB);
        return saleRepository.findAllWithDetails().stream().limit(60).map(s -> new LabDTOs.SaleRow(
                s.getId(), s.getManual().getId(), s.getManual().getTitle(),
                s.getStudent().getMatricNumber(), s.getStudent().getFullName(),
                s.getUnitPrice(), s.getQuantity(), s.getSoldAt(), s.getRecordedBy(),
                s.isVoided(), s.getAcademicSession().getName()
        )).toList();
    }

    public LabDTOs.Message voidSale(String username, Long id) {
        User user = require(username, FINANCE);
        LabManualSale s = saleRepository.findById(id).orElseThrow(() -> bad("Sale not found."));
        if (s.isVoided()) throw bad("This sale is already voided.");

        s.setVoided(true);
        s.setVoidedBy(user.getUsername());
        s.setVoidedAt(LocalDateTime.now());
        saleRepository.save(s);
        return new LabDTOs.Message("Sale voided. The copy is back in stock.");
    }

    // ---------------------------------------------------------------- finance

    @Transactional(readOnly = true)
    public LabDTOs.Finance finance(String username) {
        require(username, FINANCE);

        List<LabManualPurchase> purchases = purchaseRepository.findAllWithManual();
        List<LabManualSale> sales = liveSales();

        List<LabDTOs.ManualFinance> rows = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalStock = BigDecimal.ZERO;
        int copiesSold = 0;
        int copiesInStock = 0;

        for (LabManual m : manualRepository.findAllWithCourse()) {
            int purchased = purchases.stream().filter(p -> p.getManual().getId().equals(m.getId())).mapToInt(LabManualPurchase::getQuantity).sum();
            int sold = sales.stream().filter(s -> s.getManual().getId().equals(m.getId())).mapToInt(LabManualSale::getQuantity).sum();
            BigDecimal spent = purchases.stream().filter(p -> p.getManual().getId().equals(m.getId()))
                    .map(p -> p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal revenue = sales.stream().filter(s -> s.getManual().getId().equals(m.getId()))
                    .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avg = purchased > 0 ? spent.divide(BigDecimal.valueOf(purchased), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal profit = revenue.subtract(avg.multiply(BigDecimal.valueOf(sold)));
            int stock = purchased - sold;
            BigDecimal stockValue = avg.multiply(BigDecimal.valueOf(Math.max(stock, 0)));

            rows.add(new LabDTOs.ManualFinance(m.getId(), m.getTitle(), purchased, sold, stock,
                    money(revenue), money(spent), money(profit), money(stockValue)));

            totalRevenue = totalRevenue.add(revenue);
            totalSpent = totalSpent.add(spent);
            totalProfit = totalProfit.add(profit);
            totalStock = totalStock.add(stockValue);
            copiesSold += sold;
            copiesInStock += Math.max(stock, 0);
        }

        // Money in and out, month by month. Plain loops, so nothing is captured by a lambda.
        Map<YearMonth, BigDecimal[]> byMonth = new TreeMap<>();
        for (LabManualSale s : sales) {
            YearMonth month = YearMonth.from(s.getSoldAt());
            BigDecimal[] totals = byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[0] = totals[0].add(s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantity())));
        }
        for (LabManualPurchase p : purchases) {
            YearMonth month = YearMonth.from(p.getPurchasedOn());
            BigDecimal[] totals = byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[1] = totals[1].add(p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantity())));
        }

        List<LabDTOs.MonthPoint> allMonths = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal[]> e : byMonth.entrySet()) {
            allMonths.add(new LabDTOs.MonthPoint(e.getKey().toString(), money(e.getValue()[0]), money(e.getValue()[1])));
        }
        List<LabDTOs.MonthPoint> months = allMonths.size() > 12
                ? new ArrayList<>(allMonths.subList(allMonths.size() - 12, allMonths.size()))
                : allMonths;

        return new LabDTOs.Finance(
                money(totalRevenue), money(totalSpent), money(totalRevenue.subtract(totalSpent)),
                money(totalProfit), money(totalStock), copiesSold, copiesInStock, rows, months
        );
    }

    // ----------------------------------------------------------------- impact

    /** H.O.D only: do students who bought the manual score differently? Shows a link, not proof. */
    @Transactional(readOnly = true)
    public LabDTOs.Impact impact(String username, Long sessionId, Long manualId) {
        require(username, HOD);

        LabManual m = manualRepository.findAllWithCourse().stream()
                .filter(x -> x.getId().equals(manualId)).findFirst().orElseThrow(() -> bad("Manual not found."));
        if (m.getCourse() == null) throw bad("Link this manual to a course first, so its students can be compared.");

        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));
        Course course = m.getCourse();

        Set<Long> buyerIds = new HashSet<>();
        for (LabManualSale s : liveSales()) {
            if (s.getManual().getId().equals(m.getId()) && s.getAcademicSession().getId().equals(sessionId)) {
                buyerIds.add(s.getStudent().getId());
            }
        }

        Map<Long, BigDecimal> scores = new HashMap<>();
        for (Result r : resultRepository.findApprovedForSession(sessionId)) {
            if (r.getCourse().getId().equals(course.getId()) && r.getTotalScore() != null) {
                scores.put(r.getStudent().getId(), new BigDecimal(r.getTotalScore().toString()));
            }
        }

        List<BigDecimal> buyerScores = new ArrayList<>();
        List<BigDecimal> otherScores = new ArrayList<>();
        String semester = course.getSemester() == null ? "FIRST" : course.getSemester().toUpperCase();

        for (CourseRegistration reg : registrationRepository
                .findByCourseIdAndAcademicSessionIdAndSemester(course.getId(), sessionId, semester)) {
            if (!"REGISTERED".equalsIgnoreCase(reg.getStatus())) continue;
            BigDecimal score = scores.get(reg.getStudent().getId());
            if (score == null) continue;
            if (buyerIds.contains(reg.getStudent().getId())) {
                buyerScores.add(score);
            } else {
                otherScores.add(score);
            }
        }

        LabDTOs.Group buyers = group(buyerScores);
        LabDTOs.Group others = group(otherScores);
        boolean enough = buyerScores.size() >= MIN_GROUP && otherScores.size() >= MIN_GROUP;

        Double difference = enough ? Math.round((buyers.average() - others.average()) * 100.0) / 100.0 : null;

        String note = enough
                ? "This compares two groups of students. It shows a link between buying the manual and scores in " + course.getCode()
                  + ", not proof that the manual caused the difference. Students who buy may also differ in attendance, motivation or ability."
                : "At least " + MIN_GROUP + " students with approved results are needed in each group before a comparison means anything. "
                  + "So far: " + buyerScores.size() + " who bought, " + otherScores.size() + " who did not.";

        return new LabDTOs.Impact(m.getTitle(), course.getCode(), session.getName(), buyers, others, difference, enough, note);
    }

    // ---------------------------------------------------------------- helpers

    private LabDTOs.Group group(List<BigDecimal> scores) {
        if (scores.isEmpty()) return new LabDTOs.Group(0, null, null);

        double average = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        long passed = scores.stream().filter(s -> s.compareTo(PASS_MARK) >= 0).count();

        return new LabDTOs.Group(
                scores.size(),
                Math.round(average * 100.0) / 100.0,
                Math.round(passed * 10000.0 / scores.size()) / 100.0
        );
    }

    private List<LabDTOs.ManualRow> manualRows(boolean showCost) {
        List<LabManualPurchase> purchases = purchaseRepository.findAllWithManual();
        List<LabManualSale> sales = liveSales();
        List<LabDTOs.ManualRow> rows = new ArrayList<>();

        for (LabManual m : manualRepository.findAllWithCourse()) {
            int purchased = purchases.stream().filter(p -> p.getManual().getId().equals(m.getId())).mapToInt(LabManualPurchase::getQuantity).sum();
            int sold = sales.stream().filter(s -> s.getManual().getId().equals(m.getId())).mapToInt(LabManualSale::getQuantity).sum();
            BigDecimal spent = purchases.stream().filter(p -> p.getManual().getId().equals(m.getId()))
                    .map(p -> p.getUnitCost().multiply(BigDecimal.valueOf(p.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = purchased > 0 ? spent.divide(BigDecimal.valueOf(purchased), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            rows.add(new LabDTOs.ManualRow(
                    m.getId(), m.getTitle(),
                    m.getCourse() != null ? m.getCourse().getId() : null,
                    m.getCourse() != null ? m.getCourse().getCode() : null,
                    m.getCourse() != null ? m.getCourse().getTitle() : null,
                    m.getUnitPrice(), m.isActive(), purchased, sold, purchased - sold,
                    showCost ? avg : null
            ));
        }
        return rows;
    }

    private int stockOf(Long manualId) {
        int purchased = purchaseRepository.findAllWithManual().stream()
                .filter(p -> p.getManual().getId().equals(manualId)).mapToInt(LabManualPurchase::getQuantity).sum();
        int sold = liveSales().stream()
                .filter(s -> s.getManual().getId().equals(manualId)).mapToInt(LabManualSale::getQuantity).sum();
        return purchased - sold;
    }

    private List<LabManualSale> liveSales() {
        return saleRepository.findAllWithDetails().stream().filter(s -> !s.isVoided()).toList();
    }

    private BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private String role(User user) {
        return String.valueOf(user.getRole()).toUpperCase();
    }

    private User require(String username, Set<String> allowed) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!allowed.contains(role(user))) throw bad("You do not have permission to do this.");
        return user;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}