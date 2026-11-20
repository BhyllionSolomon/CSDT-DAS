package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.HomeSlideImage;
import com.kdu.csdtdas.backend.service.HomeSlideService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class HomeSlideController {

    private final HomeSlideService service;

    public HomeSlideController(HomeSlideService service) {
        this.service = service;
    }

    // ---- public: the sign-in page loads these before anyone is signed in

    @GetMapping("/api/auth/home/slides")
    public List<HomeSlideService.PublicSlide> publicSlides() {
        return service.publicSlides();
    }

    @GetMapping("/api/auth/home/slides/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        HomeSlideImage img = service.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(img.getData());
    }

    // ---- H.O.D and admin only (the service checks)

    @GetMapping("/api/home-slides")
    public List<HomeSlideService.AdminSlide> list(Authentication a) {
        return service.list(name(a));
    }

    @PostMapping("/api/home-slides")
    public ResponseEntity<HomeSlideService.AdminSlide> create(@RequestBody HomeSlideService.SlideRequest body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(name(a), body));
    }

    @PutMapping("/api/home-slides/{id}")
    public HomeSlideService.AdminSlide update(@PathVariable Long id, @RequestBody HomeSlideService.SlideRequest body, Authentication a) {
        return service.update(name(a), id, body);
    }

    @PutMapping("/api/home-slides/{id}/image")
    public HomeSlideService.AdminSlide upload(@PathVariable Long id, @RequestBody byte[] data, Authentication a) {
        return service.uploadImage(name(a), id, data);
    }

    @DeleteMapping("/api/home-slides/{id}/image")
    public HomeSlideService.AdminSlide removeImage(@PathVariable Long id, Authentication a) {
        return service.removeImage(name(a), id);
    }

    @PostMapping("/api/home-slides/{id}/move")
    public List<HomeSlideService.AdminSlide> move(@PathVariable Long id, @RequestBody HomeSlideService.MoveRequest body, Authentication a) {
        return service.move(name(a), id, body.direction());
    }

    @DeleteMapping("/api/home-slides/{id}")
    public List<HomeSlideService.AdminSlide> delete(@PathVariable Long id, Authentication a) {
        return service.delete(name(a), id);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}