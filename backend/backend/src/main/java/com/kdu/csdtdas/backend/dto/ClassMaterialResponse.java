package com.kdu.csdtdas.backend.dto;

import com.kdu.csdtdas.backend.entity.ClassMaterial;

import java.time.LocalDateTime;

public record ClassMaterialResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseTitle,
        String academicSessionName,
        String semester,
        String title,
        String description,
        String type,
        String originalFilename,
        Long sizeBytes,
        String linkUrl,
        Integer downloadCount,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public static ClassMaterialResponse from(ClassMaterial m) {
        return new ClassMaterialResponse(
                m.getId(),
                m.getCourse().getId(),
                m.getCourse().getCode(),
                m.getCourse().getTitle(),
                m.getAcademicSession().getName(),
                m.getSemester(),
                m.getTitle(),
                m.getDescription(),
                m.getType(),
                m.getOriginalFilename(),
                m.getSizeBytes(),
                m.getLinkUrl(),
                m.getDownloadCount(),
                m.getUploadedBy().getId(),
                m.getUploadedBy().getFullName(),
                m.getCreatedAt()
        );
    }
}