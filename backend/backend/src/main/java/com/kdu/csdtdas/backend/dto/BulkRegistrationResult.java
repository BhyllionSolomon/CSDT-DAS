package com.kdu.csdtdas.backend.dto;

public class BulkRegistrationResult {
    private int registered;
    private int skipped;
    private int totalUnits;
    private int maxUnits;

    public BulkRegistrationResult() {}
    public BulkRegistrationResult(int registered, int skipped, int totalUnits, int maxUnits) {
        this.registered = registered; this.skipped = skipped;
        this.totalUnits = totalUnits; this.maxUnits = maxUnits;
    }
    public int getRegistered() { return registered; }
    public void setRegistered(int registered) { this.registered = registered; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
    public int getMaxUnits() { return maxUnits; }
    public void setMaxUnits(int maxUnits) { this.maxUnits = maxUnits; }
}