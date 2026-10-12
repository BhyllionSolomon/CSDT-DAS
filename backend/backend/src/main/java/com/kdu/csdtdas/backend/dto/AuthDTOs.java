package com.kdu.csdtdas.backend.dto;

public final class AuthDTOs {

    private AuthDTOs() {}

    public record SignupResult(boolean pendingApproval, String message, LoginResponse login) {}

    public record Message(String message) {}
}