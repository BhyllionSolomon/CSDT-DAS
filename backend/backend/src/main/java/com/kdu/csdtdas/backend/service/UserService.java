package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ProgrammeRepository programmeRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            ProgrammeRepository programmeRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.programmeRepository = programmeRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public User create(
            String username, String password, String fullName, String email,
            String role, Long programmeId
    ) {
        validateNewUser(username, password, role);

        String cleanRole = role.trim().toUpperCase();

        if ("LEVEL_ADVISER".equals(cleanRole) && programmeId == null) {
            throw new IllegalArgumentException("A Level Adviser must be assigned to a programme.");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(cleanRole);
        user.setActive(true);

        if (programmeId != null) {
            user.setProgramme(findProgramme(programmeId));
        }

        return userRepository.save(user);
    }

    /**
     * Public self-registration. Role is auto-detected: an ID starting with
     * "KDU" is treated as a student matric number; anything else is treated
     * as a lecturer awaiting assignment by the H.O.D. If the ID matches an
     * existing Student record (e.g. already loaded via bulk upload), the
     * account is linked to it automatically.
     */
    public User signup(String idNumber, String fullName, String email, String username, String password) {
        validateNewUser(username, password, "STUDENT");

        if (idNumber == null || idNumber.isBlank()) {
            throw new IllegalArgumentException("ID number is required.");
        }

        String cleanId = idNumber.trim().toUpperCase();
        boolean looksLikeStudent = cleanId.startsWith("KDU");

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(true);

        if (looksLikeStudent) {
            Student student = studentRepository.findByMatricNumber(cleanId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This matric number is not on the department's student roster. "
                                    + "Please contact your Level Adviser or the H.O.D to confirm you have been added."
                    ));

            boolean alreadyLinked = userRepository.findAll().stream()
                    .anyMatch(u -> u.getStudent() != null && u.getStudent().getId().equals(student.getId()));

            if (alreadyLinked) {
                throw new IllegalArgumentException(
                        "An account has already been created for this matric number."
                );
            }

            user.setRole("STUDENT");
            user.setStudent(student);
        } else {
            user.setRole("LECTURER");
        }

        return userRepository.save(user);
    }

    public User update(
            Long id, String fullName, String email, String role, Boolean active, Long programmeId
    ) {
        User existing = getById(id);

        if (fullName != null) existing.setFullName(fullName);
        if (email != null) existing.setEmail(email);
        if (role != null) existing.setRole(role.trim().toUpperCase());
        if (active != null) existing.setActive(active);
        if (programmeId != null) existing.setProgramme(findProgramme(programmeId));

        return userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public String authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("This account is inactive.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    private Programme findProgramme(Long programmeId) {
        return programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
    }

    private void validateNewUser(String username, String password, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Username already exists.");
        }
    }

    public void resetPassword(String matricNumber, String newPassword) {

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        Student student = studentRepository.findByMatricNumber(matricNumber.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No student found with this matric number."));

        User user = userRepository.findAll().stream()
                .filter(u -> u.getStudent() != null && u.getStudent().getId().equals(student.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No account found for this matric number."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}