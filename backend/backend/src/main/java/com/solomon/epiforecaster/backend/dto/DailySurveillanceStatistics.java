package com.solomon.epiforecaster.backend.dto;

public class DailySurveillanceStatistics {

    private long totalReports;
    private long totalFacilities;
    private long totalSuspectedCases;
    private long totalConfirmedCases;
    private long totalDeaths;

    public DailySurveillanceStatistics() {
    }

    public DailySurveillanceStatistics(
            long totalReports,
            long totalFacilities,
            long totalSuspectedCases,
            long totalConfirmedCases,
            long totalDeaths) {

        this.totalReports = totalReports;
        this.totalFacilities = totalFacilities;
        this.totalSuspectedCases = totalSuspectedCases;
        this.totalConfirmedCases = totalConfirmedCases;
        this.totalDeaths = totalDeaths;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getTotalFacilities() {
        return totalFacilities;
    }

    public void setTotalFacilities(long totalFacilities) {
        this.totalFacilities = totalFacilities;
    }

    public long getTotalSuspectedCases() {
        return totalSuspectedCases;
    }

    public void setTotalSuspectedCases(long totalSuspectedCases) {
        this.totalSuspectedCases = totalSuspectedCases;
    }

    public long getTotalConfirmedCases() {
        return totalConfirmedCases;
    }

    public void setTotalConfirmedCases(long totalConfirmedCases) {
        this.totalConfirmedCases = totalConfirmedCases;
    }

    public long getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(long totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
}