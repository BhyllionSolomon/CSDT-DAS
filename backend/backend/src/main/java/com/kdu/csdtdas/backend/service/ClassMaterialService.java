package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.ClassMaterialResponse;
import com.kdu.csdtdas.backend.dto.CreateLinkMaterialRequest;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.ClassMaterial;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.ClassMaterialRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@Transactional
public class ClassMaterialService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "png", "jpg", "jpeg", "zip");

    public record DownloadedFile(Resource resource, String filename, String contentType) {}

    private final ClassMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final UserRepository userRepository;
    private final TeachingAccessService accessService;
    private final String uploadDir;

    public ClassMaterialService(
            ClassMaterialRepository materialRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository,
            UserRepository userRepository,
            TeachingAccessService accessService,
            @Value("${app.upload-dir:./uploads}") String uploadDir
    ) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.uploadDir = uploadDir;
    }

    public ClassMaterialResponse addFile(
            String username, Long courseId, Long sessionId, String semester,
            String title, String description, MultipartFile file
    ) {
        User user = getUser(username);
        String cleanSemester = cleanSemester(semester);
        accessService.requireTeachingRights(user, courseId, sessionId, cleanSemester);

        if (title == null || title.isBlank()) throw new IllegalArgumentException("A title is required.");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please choose a file to attach.");

        String original = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(original);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "This file type is not allowed. Allowed types: " + String.join(", ", new TreeSet<>(ALLOWED_EXTENSIONS))
            );
        }

        String stored = storeFile(file, extension);

        ClassMaterial material = baseMaterial(user, courseId, sessionId, cleanSemester, title, description);
        material.setType("FILE");
        material.setOriginalFilename(original);
        material.setStoredFilename(stored);
        material.setContentType(file.getContentType());
        material.setSizeBytes(file.getSize());

        return ClassMaterialResponse.from(materialRepository.save(material));
    }

    public ClassMaterialResponse addLink(String username, CreateLinkMaterialRequest request) {
        User user = getUser(username);
        String cleanSemester = cleanSemester(request.semester());
        accessService.requireTeachingRights(user, request.courseId(), request.academicSessionId(), cleanSemester);

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("A title is required.");
        }

        String url = request.linkUrl() == null ? "" : request.linkUrl().trim();
        if (!(url.startsWith("http://") || url.startsWith("https://")) || url.length() > 1000) {
            throw new IllegalArgumentException("Please enter a valid link starting with http:// or https://");
        }

        ClassMaterial material = baseMaterial(
                user, request.courseId(), request.academicSessionId(), cleanSemester,
                request.title(), request.description()
        );
        material.setType("LINK");
        material.setLinkUrl(url);

        return ClassMaterialResponse.from(materialRepository.save(material));
    }

    @Transactional(readOnly = true)
    public List<ClassMaterialResponse> listForCourse(String username, Long courseId, Long sessionId, String semester) {
        User user = getUser(username);
        String cleanSemester = cleanSemester(semester);
        accessService.requireTeachingRights(user, courseId, sessionId, cleanSemester);

        return materialRepository.findForCourse(courseId, sessionId, cleanSemester)
                .stream().map(ClassMaterialResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ClassMaterialResponse> listForStudent(String username) {
        User user = getUser(username);
        if (user.getStudent() == null) {
            throw new IllegalArgumentException("Your account is not linked to a student record.");
        }

        return materialRepository.findForStudent(user.getStudent().getId())
                .stream().map(ClassMaterialResponse::from).toList();
    }

    public DownloadedFile download(String username, Long id) {
        User user = getUser(username);
        ClassMaterial material = materialRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Material not found."));

        if (!"FILE".equals(material.getType())) {
            throw new IllegalArgumentException("This material is a link, not a file.");
        }

        Long courseId = material.getCourse().getId();
        Long sessionId = material.getAcademicSession().getId();
        String semester = material.getSemester();

        boolean teacher = accessService.canTeach(user, courseId, sessionId, semester);
        boolean registeredStudent = accessService.isRegistered(user, courseId, sessionId, semester);

        if (!teacher && !registeredStudent) {
            throw new IllegalArgumentException("You do not have access to this material.");
        }

        Path file = uploadRoot().resolve(material.getStoredFilename()).normalize();
        Resource resource;
        try {
            resource = new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not read the stored file.", e);
        }

        if (!resource.exists()) {
            throw new IllegalArgumentException("The file is no longer available on the server.");
        }

        if (registeredStudent && !teacher) {
            material.setDownloadCount(material.getDownloadCount() + 1);
            materialRepository.save(material);
        }

        String contentType = material.getContentType() != null
                ? material.getContentType() : "application/octet-stream";

        return new DownloadedFile(resource, material.getOriginalFilename(), contentType);
    }

    public void delete(String username, Long id) {
        User user = getUser(username);
        ClassMaterial material = materialRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Material not found."));

        boolean isUploader = material.getUploadedBy().getId().equals(user.getId());

        if (!isUploader && !accessService.isPrivileged(user)) {
            throw new IllegalArgumentException(
                    "Only the lecturer who attached this material (or the H.O.D) can delete it."
            );
        }

        if ("FILE".equals(material.getType()) && material.getStoredFilename() != null) {
            try {
                Files.deleteIfExists(uploadRoot().resolve(material.getStoredFilename()).normalize());
            } catch (IOException ignored) {
                // The record is still removed even if the file is already gone.
            }
        }

        materialRepository.delete(material);
    }

    private ClassMaterial baseMaterial(
            User user, Long courseId, Long sessionId, String semester, String title, String description
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        AcademicSession session = academicSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        ClassMaterial material = new ClassMaterial();
        material.setCourse(course);
        material.setAcademicSession(session);
        material.setSemester(semester);
        material.setUploadedBy(user);
        material.setTitle(title.trim());
        material.setDescription(description == null || description.isBlank() ? null : description.trim());
        return material;
    }

    private String storeFile(MultipartFile file, String extension) {
        try {
            Path dir = uploadRoot();
            Files.createDirectories(dir);

            String stored = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(stored).normalize();

            if (!target.startsWith(dir)) {
                throw new IllegalArgumentException("Invalid file name.");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return stored;

        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded file.", e);
        }
    }

    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String sanitizeName(String name) {
        if (name == null || name.isBlank()) return "file";
        String cleaned = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private String cleanSemester(String semester) {
        if (semester == null || semester.isBlank()) throw new IllegalArgumentException("Semester is required.");
        return semester.trim().toUpperCase();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}