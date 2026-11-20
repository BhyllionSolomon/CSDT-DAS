package com.kdu.csdtdas.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LabAssetDTOs {
    private LabAssetDTOs() {}

    public record AssetRequest(
            String assetTag, String name, String category, String laboratory,
            String description, Integer quantity, String conditionStatus, LocalDate acquiredDate,
            String roomLocation, String manufacturer, String model, String serialNumber,
            String processor, String ram, String storage, String operatingSystem,
            String networkStatus, String notes, Boolean active
    ) {}

    public record ConditionRequest(String conditionStatus, String notes) {}

    public record AssetRow(
            Long id, String assetTag, String name, String category, String laboratory,
            String description, Integer quantity, String conditionStatus, LocalDate acquiredDate,
            String roomLocation, String manufacturer, String model, String serialNumber,
            String processor, String ram, String storage, String operatingSystem,
            String networkStatus, String notes, boolean active,
            String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    public record Message(String message) {}
}