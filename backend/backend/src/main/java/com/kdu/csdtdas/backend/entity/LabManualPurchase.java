package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lab_manual_purchases")
public class LabManualPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manual_id", nullable = false)
    private LabManual manual;

    @Column(nullable = false)
    private int quantity;

    /** What each copy cost to print or buy. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private LocalDate purchasedOn;

    @Column(length = 200)
    private String note;

    @Column(length = 100)
    private String recordedBy;

    public LabManualPurchase() {}

    public Long getId() { return id; }
    public LabManual getManual() { return manual; }
    public void setManual(LabManual manual) { this.manual = manual; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public void setPurchasedOn(LocalDate purchasedOn) { this.purchasedOn = purchasedOn; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
}