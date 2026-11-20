package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_assets", indexes = {
        @Index(name = "idx_lab_asset_lab", columnList = "laboratory"),
        @Index(name = "idx_lab_asset_category", columnList = "category"),
        @Index(name = "idx_lab_asset_condition", columnList = "condition_status")
})
public class LabAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String assetTag;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, length = 80)
    private String laboratory;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "condition_status", nullable = false, length = 40)
    private String conditionStatus = "FUNCTIONAL";

    private LocalDate acquiredDate;

    @Column(length = 120)
    private String roomLocation;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    @Column(length = 120)
    private String serialNumber;

    @Column(length = 120)
    private String processor;

    @Column(length = 80)
    private String ram;

    @Column(length = 80)
    private String storage;

    @Column(length = 100)
    private String operatingSystem;

    @Column(length = 40)
    private String networkStatus;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false, length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public LabAsset() {}

    @PrePersist
    public void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (quantity == null) quantity = 1;
        if (conditionStatus == null) conditionStatus = "FUNCTIONAL";
    }

    @PreUpdate
    public void beforeUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String v) { assetTag = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getLaboratory() { return laboratory; }
    public void setLaboratory(String v) { laboratory = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { quantity = v; }
    public String getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(String v) { conditionStatus = v; }
    public LocalDate getAcquiredDate() { return acquiredDate; }
    public void setAcquiredDate(LocalDate v) { acquiredDate = v; }
    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String v) { roomLocation = v; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String v) { manufacturer = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String v) { serialNumber = v; }
    public String getProcessor() { return processor; }
    public void setProcessor(String v) { processor = v; }
    public String getRam() { return ram; }
    public void setRam(String v) { ram = v; }
    public String getStorage() { return storage; }
    public void setStorage(String v) { storage = v; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String v) { operatingSystem = v; }
    public String getNetworkStatus() { return networkStatus; }
    public void setNetworkStatus(String v) { networkStatus = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { updatedBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}