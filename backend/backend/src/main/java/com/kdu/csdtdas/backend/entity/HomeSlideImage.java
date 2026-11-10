package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

/** The picture itself, kept apart from the slide so lists never load the image bytes. */
@Entity
@Table(name = "home_slide_images")
public class HomeSlideImage {

    @Id
    private Long slideId;

    @Column(nullable = false, length = 40)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public HomeSlideImage() {}

    public Long getSlideId() { return slideId; }
    public void setSlideId(Long slideId) { this.slideId = slideId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}