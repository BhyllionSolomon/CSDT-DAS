package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.HomeSlide;
import com.kdu.csdtdas.backend.entity.HomeSlideImage;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.HomeSlideImageRepository;
import com.kdu.csdtdas.backend.repository.HomeSlideRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class HomeSlideService {

    private static final int MAX_SLIDES = 8;
    private static final int MAX_IMAGE_BYTES = 3 * 1024 * 1024;
    private static final Set<String> STAFF = Set.of("HOD", "ADMIN");

    public record PublicSlide(Long id, String eyebrow, String heading, String body, String sideHeading, String sideText,
                              boolean hasImage, long version) {}

    public record AdminSlide(Long id, int sortOrder, String eyebrow, String heading, String body, String sideHeading,
                             String sideText, boolean active, boolean hasImage, long version) {}

    public record SlideRequest(String eyebrow, String heading, String body, String sideHeading, String sideText, Boolean active) {}

    public record MoveRequest(String direction) {}

    private final HomeSlideRepository slideRepository;
    private final HomeSlideImageRepository imageRepository;
    private final UserRepository userRepository;

    public HomeSlideService(HomeSlideRepository slideRepository, HomeSlideImageRepository imageRepository, UserRepository userRepository) {
        this.slideRepository = slideRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------- public

    /** What visitors see on the sign-in page. */
    public List<PublicSlide> publicSlides() {
        seedIfEmpty();
        return slideRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(HomeSlide::isActive)
                .map(s -> new PublicSlide(s.getId(), s.getEyebrow(), s.getHeading(), s.getBody(),
                        s.getSideHeading(), s.getSideText(), s.isHasImage(), s.getImageVersion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeSlideImage image(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> bad("Image not found."));
    }

    // ----------------------------------------------------------------- staff

    public List<AdminSlide> list(String username) {
        requireStaff(username);
        seedIfEmpty();
        return slideRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::admin).toList();
    }

    public AdminSlide create(String username, SlideRequest req) {
        requireStaff(username);
        List<HomeSlide> all = slideRepository.findAllByOrderBySortOrderAscIdAsc();
        if (all.size() >= MAX_SLIDES) throw bad("You can have at most " + MAX_SLIDES + " slides. Delete one first.");

        HomeSlide s = new HomeSlide();
        s.setSortOrder(all.size());
        apply(s, req);
        return admin(slideRepository.save(s));
    }

    public AdminSlide update(String username, Long id, SlideRequest req) {
        requireStaff(username);
        HomeSlide s = slide(id);
        apply(s, req);
        return admin(slideRepository.save(s));
    }

    /** Only a real JPEG, PNG or WebP is accepted, whatever the file claims to be. */
    public AdminSlide uploadImage(String username, Long id, byte[] data) {
        requireStaff(username);
        HomeSlide s = slide(id);

        if (data == null || data.length == 0) throw bad("No image was received.");
        if (data.length > MAX_IMAGE_BYTES) throw bad("This image is too large. Please use one under 3 MB.");

        String type = detectType(data);
        if (type == null) throw bad("Please upload a JPG, PNG or WebP picture.");

        HomeSlideImage img = imageRepository.findById(id).orElseGet(HomeSlideImage::new);
        img.setSlideId(id);
        img.setContentType(type);
        img.setData(data);
        imageRepository.save(img);

        s.setHasImage(true);
        s.setImageVersion(System.currentTimeMillis());
        return admin(slideRepository.save(s));
    }

    public AdminSlide removeImage(String username, Long id) {
        requireStaff(username);
        HomeSlide s = slide(id);
        imageRepository.findById(id).ifPresent(imageRepository::delete);
        s.setHasImage(false);
        s.setImageVersion(System.currentTimeMillis());
        return admin(slideRepository.save(s));
    }

    public List<AdminSlide> move(String username, Long id, String direction) {
        requireStaff(username);
        List<HomeSlide> all = new java.util.ArrayList<>(slideRepository.findAllByOrderBySortOrderAscIdAsc());

        int from = -1;
        for (int i = 0; i < all.size(); i++) if (all.get(i).getId().equals(id)) from = i;
        if (from < 0) throw bad("Slide not found.");

        int to = "UP".equalsIgnoreCase(direction) ? from - 1 : from + 1;
        if (to >= 0 && to < all.size()) java.util.Collections.swap(all, from, to);

        return renumber(all);
    }

    public List<AdminSlide> delete(String username, Long id) {
        requireStaff(username);
        HomeSlide s = slide(id);
        imageRepository.findById(id).ifPresent(imageRepository::delete);
        slideRepository.delete(s);

        List<HomeSlide> rest = new java.util.ArrayList<>(slideRepository.findAllByOrderBySortOrderAscIdAsc());
        rest.removeIf(x -> x.getId().equals(id));
        return renumber(rest);
    }

    // --------------------------------------------------------------- helpers

    private void seedIfEmpty() {
        if (slideRepository.count() > 0) return;

        String[][] defaults = {
                {"Welcome to CSDT", "Shaping the Future Through Technology",
                        "Discover a department committed to developing the knowledge, skills and creativity needed to solve tomorrow's digital challenges.",
                        "Learn. Innovate. Create.", "Explore opportunities in computing, software engineering, information technology and cybersecurity."},
                {"Your Future Starts Here", "Prepare for a Digital World",
                        "Build practical skills, develop critical thinking and prepare for meaningful careers in the global technology industry.",
                        "Education Beyond the Classroom", "Learn through practical projects, collaboration, research and industry-focused experiences."},
                {"Research and Innovation", "Creating Solutions That Matter",
                        "From artificial intelligence to cybersecurity, our discipline offers opportunities to explore ideas and develop technology for real-world needs.",
                        "Ideas Into Impact", "Join a learning community where curiosity, research and innovation drive progress."}
        };

        int order = 0;
        for (String[] d : defaults) {
            HomeSlide s = new HomeSlide();
            s.setSortOrder(order++);
            s.setEyebrow(d[0]);
            s.setHeading(d[1]);
            s.setBody(d[2]);
            s.setSideHeading(d[3]);
            s.setSideText(d[4]);
            slideRepository.save(s);
        }
    }

    private void apply(HomeSlide s, SlideRequest req) {
        String heading = clean(req.heading(), 150);
        if (heading == null) throw bad("Every slide needs a headline.");

        s.setHeading(heading);
        s.setEyebrow(clean(req.eyebrow(), 60));
        s.setBody(clean(req.body(), 400));
        s.setSideHeading(clean(req.sideHeading(), 100));
        s.setSideText(clean(req.sideText(), 300));
        s.setActive(req.active() == null || req.active());
    }

    private String clean(String v, int max) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (t.length() > max) throw bad("One of the texts is too long (" + max + " characters at most).");
        return t;
    }

    private List<AdminSlide> renumber(List<HomeSlide> ordered) {
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).setSortOrder(i);
        slideRepository.saveAll(ordered);
        return ordered.stream().map(this::admin).toList();
    }

    private AdminSlide admin(HomeSlide s) {
        return new AdminSlide(s.getId(), s.getSortOrder(), s.getEyebrow(), s.getHeading(), s.getBody(),
                s.getSideHeading(), s.getSideText(), s.isActive(), s.isHasImage(), s.getImageVersion());
    }

    private HomeSlide slide(Long id) {
        return slideRepository.findById(id).orElseThrow(() -> bad("Slide not found."));
    }

    private String detectType(byte[] d) {
        if (d.length > 12 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "image/webp";
        return null;
    }

    private void requireStaff(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!STAFF.contains(String.valueOf(user.getRole()).toUpperCase())) throw bad("Only the H.O.D or an admin can change the home page.");
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}