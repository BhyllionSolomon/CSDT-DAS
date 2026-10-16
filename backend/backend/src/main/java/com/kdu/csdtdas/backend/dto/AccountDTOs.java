package com.kdu.csdtdas.backend.dto;

public final class AccountDTOs {

    private AccountDTOs() {}

    public record Profile(String username, String fullName, String email, String phoneNumber, String role) {}

    public record ChangeRequest(String purpose, String newValue, String currentPassword) {}

    public record ConfirmRequest(String purpose, String code, String newPassword) {}

    public record ChangeResult(String message, String token) {}

    public record Message(String message) {}

    public record ForgotRequest(String identifier) {}

    public record ForgotConfirm(String identifier, String code, String newPassword) {}
}