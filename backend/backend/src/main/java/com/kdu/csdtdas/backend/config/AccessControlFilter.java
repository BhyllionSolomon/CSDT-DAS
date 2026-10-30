package com.kdu.csdtdas.backend.config;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.service.SecurityAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides, for every signed-in request, whether this person may touch this particular record.
 * Students get a short allow-list and can only name themselves. Nobody can set another person's password.
 */
@Component
public class AccessControlFilter extends OncePerRequestFilter {

    private static final int MAX_BODY = 2 * 1024 * 1024;
    private static final Set<String> CREATABLE_ROLES = Set.of("LECTURER", "ADJUNCT", "LAB_OFFICER", "TECHNICAL_STAFF");

    private static final Pattern RESET = Pattern.compile("^/api/users/\\d+/reset-password$");
    private static final Pattern USER_ONE = Pattern.compile("^/api/users/\\d+$");
    private static final Pattern STUDENT_ID_FIELD = Pattern.compile("\"studentId\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern ROLE_FIELD = Pattern.compile("\"role\"\\s*:\\s*\"([A-Za-z_]+)\"");

    private static final Pattern STUDENT_ACCOUNT = Pattern.compile("^/api/account/.*$");
    private static final Pattern STUDENT_OPEN_GET = Pattern.compile(
            "^/api/(courses|academic-sessions|sessions|programmes|levels|departments|unit-limits)(/.*)?$");
    private static final Pattern STUDENT_FIXED_GET = Pattern.compile(
            "^/api/(advisers/mine|clearance/mine|registration-form/mine|materials/mine|online-classes/mine|materials/\\d+/download)$");
    private static final Pattern STUDENT_FIXED_POST = Pattern.compile(
            "^/api/(clearance/submit|self-results/extract|online-classes/\\d+/join)$");
    private static final Pattern STUDENT_OWN_GET = Pattern.compile(
            "^/api/(?:course-registrations/student|students|results/student)/(\\d+)(?:/.*)?$");
    private static final Pattern STUDENT_ANALYTICS_GET = Pattern.compile("^/api/self-results/analytics/(\\d+)$");
    private static final Pattern STUDENT_BODY_OWN = Pattern.compile(
            "^/api/(course-registrations|course-registrations/bulk|self-results/save)$");

    private final UserRepository userRepository;
    private final SecurityAuditService audit;

    public AccessControlFilter(UserRepository userRepository, SecurityAuditService audit) {
        this.userRepository = userRepository;
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean signedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        if (!signedIn || !path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            reply(response, 401, "This account is not active. Please sign in again.");
            return;
        }

        String role = String.valueOf(user.getRole()).toUpperCase();
        String ip = ClientIp.of(request);
        HttpServletRequest effective = request;

        // 1. Nobody can set or reset another person's password.
        if (RESET.matcher(path).matches()) {
            refuse(response, 403, "Nobody can set another person's password. Use Forgot your password on the sign-in page.",
                    user, role, ip, method, path, "password reset by someone else");
            return;
        }

        // 2. Account management: no changes to anyone's email, username, role or password, and no new H.O.D or admin.
        boolean create = "POST".equals(method) && path.equals("/api/users");
        boolean edit = "PUT".equals(method) && USER_ONE.matcher(path).matches();
        if (create || edit) {
            byte[] body = readBody(request);
            if (body == null) {
                refuse(response, 413, "The request is too large.", user, role, ip, method, path, "body too large");
                return;
            }
            String problem = checkUserBody(create, body);
            if (problem != null) {
                refuse(response, 403, "That change is not allowed here: " + problem + ".", user, role, ip, method, path, problem);
                return;
            }
            effective = new CachedBodyRequest(request, body);
        }

        // 3. Students can only reach their own records.
        if ("STUDENT".equals(role)) {
            Long ownId = user.getStudent() != null ? user.getStudent().getId() : null;
            String problem;

            if ("POST".equals(method) && STUDENT_BODY_OWN.matcher(path).matches()) {
                byte[] body = readBody(request);
                if (body == null) {
                    problem = "request too large";
                } else if (ownId == null) {
                    problem = "no student record on this account";
                } else {
                    problem = checkBodyStudentId(body, ownId);
                    if (problem == null) effective = new CachedBodyRequest(request, body);
                }
            } else {
                problem = studentPathProblem(method, path, ownId);
            }

            if (problem != null) {
                refuse(response, 403, "You are not allowed to do that (" + method + " " + path + ").",
                        user, role, ip, method, path, problem);
                return;
            }
        }

        chain.doFilter(effective, response);
    }

    // ------------------------------------------------------------------ students

    private String studentPathProblem(String method, String path, Long ownId) {

        if (STUDENT_ACCOUNT.matcher(path).matches()) return null;

        boolean get = "GET".equals(method);

        if (get && (STUDENT_OPEN_GET.matcher(path).matches() || STUDENT_FIXED_GET.matcher(path).matches())) return null;
        if ("POST".equals(method) && STUDENT_FIXED_POST.matcher(path).matches()) return null;

        if (get) {
            Matcher own = STUDENT_OWN_GET.matcher(path);
            if (own.matches()) return sameStudent(own.group(1), ownId);

            Matcher analytics = STUDENT_ANALYTICS_GET.matcher(path);
            if (analytics.matches()) return sameStudent(analytics.group(1), ownId);
        }

        return "a student may not use this endpoint";
    }

    private String sameStudent(String id, Long ownId) {
        return ownId != null && id.equals(String.valueOf(ownId)) ? null : "it names another student";
    }

    /** Every studentId in the body must be the caller's own. Anything unusual is refused. */
    private String checkBodyStudentId(byte[] body, Long ownId) {

        String text = new String(body, StandardCharsets.UTF_8);
        if (text.contains("\\u")) return "unsupported characters in the request";

        String lower = text.toLowerCase();
        int mentions = 0;
        for (int i = lower.indexOf("studentid"); i >= 0; i = lower.indexOf("studentid", i + 1)) mentions++;

        Matcher m = STUDENT_ID_FIELD.matcher(text);
        int found = 0;
        while (m.find()) {
            found++;
            if (!m.group(1).equals(String.valueOf(ownId))) return "it names another student";
        }

        if (found == 0 || found != mentions) return "the student could not be confirmed";
        return null;
    }

    // ------------------------------------------------------------ account management

    private String checkUserBody(boolean create, byte[] body) {

        String text = new String(body, StandardCharsets.UTF_8);
        if (text.contains("\\u")) return "unsupported characters in the request";

        String lower = text.toLowerCase();

        if (!create) {
            if (lower.contains("\"email\"") || lower.contains("\"role\"")
                    || lower.contains("\"username\"") || lower.contains("\"password\"")) {
                return "only the account owner can change an email, username or password, and roles are assigned from their own screens";
            }
            return null;
        }

        int mentions = 0;
        for (int i = lower.indexOf("\"role\""); i >= 0; i = lower.indexOf("\"role\"", i + 1)) mentions++;

        Matcher m = ROLE_FIELD.matcher(text);
        int found = 0;
        while (m.find()) {
            found++;
            if (!CREATABLE_ROLES.contains(m.group(1).toUpperCase())) return "that role cannot be created here";
        }

        if (found != mentions) return "the role could not be confirmed";
        return null;
    }

    // ------------------------------------------------------------------- helpers

    private byte[] readBody(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
        return bytes.length > MAX_BODY ? null : bytes;
    }

    private void refuse(
            HttpServletResponse response, int status, String message,
            User user, String role, String ip, String method, String path, String reason
    ) throws IOException {
        audit.record(user.getUsername(), role, ip, "DENIED", method + " " + path + " (" + reason + ")");
        reply(response, status, message);
    }

    private void reply(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }
}