package com.seoulaim.domain.auth.dto;

public record ConfirmEmailVerificationResponse(String verificationToken, long expiresInSeconds) {}
