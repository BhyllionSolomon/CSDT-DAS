package com.solomon.epiforecaster.backend.dto;

public class DailySurveillanceStatistics {

    private int totalReports;
    private int totalSuspectedCases;
    private int totalConfirmedCases;
    private int totalDeaths;
    private int totalRecovered;

    public DailySurveillanceStatistics() {
    }

    public int getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(int totalReports) {
        this.totalReports = totalReports;
    }

    public int getTotalSuspectedCases() {
        return totalSuspectedCases;
    }

    public void setTotalSuspectedCases(int totalSuspectedCases) {
        this.totalSuspectedCases = totalSuspectedCases;
    }

    public int getTotalConfirmedCases() {
        return totalConfirmedCases;
    }

    public void setTotalConfirmedCases(int totalConfirmedCases) {
        this.totalConfirmedCases = totalConfirmedCases;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public int getTotalRecovered() {
        return totalRecovered;
    }

    public void setTotalRecovered(int totalRecovered) {
        this.totalRecovered = totalRecovered;
    }
}