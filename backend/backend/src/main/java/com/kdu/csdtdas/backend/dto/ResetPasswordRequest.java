package com.kdu.csdtdas.backend.dto;

public record ResetPasswordRequest(String identifier, String email, String newPassword) {}