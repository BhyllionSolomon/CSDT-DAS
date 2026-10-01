package com.kdu.csdtdas.backend.dto;

public class ResetPasswordRequest {
    private String matricNumber;
    private String newPassword;

    public ResetPasswordRequest() {}
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}