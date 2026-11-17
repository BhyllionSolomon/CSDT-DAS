package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LabAssetDTOs;
import com.kdu.csdtdas.backend.entity.LabAsset;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.LabAssetRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class LabAssetService {
    private static final Set<String> LAB_ROLES = Set.of("HOD", "ADMIN", "LAB_OFFICER", "TECHNICAL_STAFF");
    private static final Set<String> MANAGERS = Set.of("HOD", "ADMIN", "LAB_OFFICER");
    private static final Set<String> CONDITIONS = Set.of(
            "FUNCTIONAL", "NEEDS_ATTENTION", "FAULTY", "UNDER_MAINTENANCE", "BEYOND_REPAIR", "DECOMMISSIONED"
    );
    private static final Set<String> LABORATORIES = Set.of(
            "COMPUTER_SCIENCE", "SOFTWARE_ENGINEERING", "CYBERSECURITY", "INFORMATION_TECHNOLOGY", "SHARED"
    );

    private final LabAssetRepository assets;
    private final UserRepository users;

    public LabAssetService(LabAssetRepository assets, UserRepository users) {
        this.assets = assets;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<LabAssetDTOs.AssetRow> list(String username) {
        require(username, LAB_ROLES);
        return assets.findAllByOrderByLaboratoryAscCategoryAscNameAsc().stream()
                .map(this::row).toList();
    }

    public LabAssetDTOs.AssetRow create(String username, LabAssetDTOs.AssetRequest request) {
        User actor = require(username, MANAGERS);
        validate(request);
        String tag = clean(request.assetTag()).toUpperCase(Locale.ROOT);
        if (assets.existsByAssetTagIgnoreCase(tag)) throw bad("That asset tag is already in use.");

        LabAsset asset = new LabAsset();
        apply(asset, request);
        asset.setAssetTag(tag);
        asset.setCreatedBy(actor.getUsername());
        asset.setUpdatedBy(actor.getUsername());
        return row(assets.save(asset));
    }

    public LabAssetDTOs.AssetRow update(String username, Long id, LabAssetDTOs.AssetRequest request) {
        User actor = require(username, MANAGERS);
        validate(request);
        LabAsset asset = assets.findById(id).orElseThrow(() -> bad("Asset not found."));
        String tag = clean(request.assetTag()).toUpperCase(Locale.ROOT);
        if (assets.existsByAssetTagIgnoreCaseAndIdNot(tag, id)) throw bad("That asset tag is already in use.");
        apply(asset, request);
        asset.setAssetTag(tag);
        asset.setUpdatedBy(actor.getUsername());
        return row(assets.save(asset));
    }

    public LabAssetDTOs.AssetRow updateCondition(String username, Long id, LabAssetDTOs.ConditionRequest request) {
        User actor = require(username, LAB_ROLES);
        if (request == null || request.conditionStatus() == null) throw bad("Select an equipment condition.");
        String condition = request.conditionStatus().trim().toUpperCase(Locale.ROOT);
        if (!CONDITIONS.contains(condition)) throw bad("Invalid equipment condition.");
        LabAsset asset = assets.findById(id).orElseThrow(() -> bad("Asset not found."));
        asset.setConditionStatus(condition);
        if (request.notes() != null && !request.notes().isBlank()) {
            String note = request.notes().trim();
            if (note.length() > 1000) throw bad("Condition note is too long.");
            asset.setNotes(note);
        }
        asset.setUpdatedBy(actor.getUsername());
        return row(assets.save(asset));
    }

    private void validate(LabAssetDTOs.AssetRequest r) {
        if (r == null) throw bad("Asset details are required.");
        required(r.assetTag(), 60, "Enter an asset tag.");
        required(r.name(), 160, "Enter the asset name.");
        required(r.category(), 60, "Select an asset category.");
        required(r.laboratory(), 80, "Select the laboratory.");
        if (!LABORATORIES.contains(r.laboratory().trim().toUpperCase(Locale.ROOT))) throw bad("Select a valid laboratory.");
        if (r.quantity() == null || r.quantity() < 1 || r.quantity() > 100000) throw bad("Quantity must be between 1 and 100,000.");
        String condition = r.conditionStatus() == null ? "FUNCTIONAL" : r.conditionStatus().trim().toUpperCase(Locale.ROOT);
        if (!CONDITIONS.contains(condition)) throw bad("Select a valid equipment condition.");
        checkLength(r.description(), 1000, "Description");
        checkLength(r.roomLocation(), 120, "Room/location");
        checkLength(r.manufacturer(), 100, "Manufacturer");
        checkLength(r.model(), 100, "Model");
        checkLength(r.serialNumber(), 120, "Serial number");
        checkLength(r.processor(), 120, "Processor");
        checkLength(r.ram(), 80, "RAM");
        checkLength(r.storage(), 80, "Storage");
        checkLength(r.operatingSystem(), 100, "Operating system");
        checkLength(r.networkStatus(), 40, "Network status");
        checkLength(r.notes(), 1000, "Notes");
    }

    private void apply(LabAsset a, LabAssetDTOs.AssetRequest r) {
        a.setName(clean(r.name()));
        a.setCategory(clean(r.category()).toUpperCase(Locale.ROOT));
        a.setLaboratory(clean(r.laboratory()).toUpperCase(Locale.ROOT));
        a.setDescription(blankToNull(r.description()));
        a.setQuantity(r.quantity());
        a.setConditionStatus(r.conditionStatus() == null ? "FUNCTIONAL" : r.conditionStatus().trim().toUpperCase(Locale.ROOT));
        a.setAcquiredDate(r.acquiredDate());
        a.setRoomLocation(blankToNull(r.roomLocation()));
        a.setManufacturer(blankToNull(r.manufacturer()));
        a.setModel(blankToNull(r.model()));
        a.setSerialNumber(blankToNull(r.serialNumber()));
        a.setProcessor(blankToNull(r.processor()));
        a.setRam(blankToNull(r.ram()));
        a.setStorage(blankToNull(r.storage()));
        a.setOperatingSystem(blankToNull(r.operatingSystem()));
        a.setNetworkStatus(blankToNull(r.networkStatus()));
        a.setNotes(blankToNull(r.notes()));
        a.setActive(r.active() == null || r.active());
    }

    private LabAssetDTOs.AssetRow row(LabAsset a) {
        return new LabAssetDTOs.AssetRow(a.getId(), a.getAssetTag(), a.getName(), a.getCategory(),
                a.getLaboratory(), a.getDescription(), a.getQuantity(), a.getConditionStatus(),
                a.getAcquiredDate(), a.getRoomLocation(), a.getManufacturer(), a.getModel(),
                a.getSerialNumber(), a.getProcessor(), a.getRam(), a.getStorage(),
                a.getOperatingSystem(), a.getNetworkStatus(), a.getNotes(), a.isActive(),
                a.getCreatedBy(), a.getUpdatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private User require(String username, Set<String> allowed) {
        User user = users.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!allowed.contains(String.valueOf(user.getRole()).toUpperCase(Locale.ROOT))) {
            throw bad("You do not have permission to access this laboratory function.");
        }
        return user;
    }

    private void required(String v, int max, String message) {
        if (v == null || v.isBlank() || v.trim().length() > max) throw bad(message);
    }
    private void checkLength(String v, int max, String label) {
        if (v != null && v.length() > max) throw bad(label + " is too long.");
    }
    private String clean(String v) { return v == null ? "" : v.trim(); }
    private String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }
}