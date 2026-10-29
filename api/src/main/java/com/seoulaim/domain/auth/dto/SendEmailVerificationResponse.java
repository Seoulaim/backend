package com.seoulaim.domain.auth.dto;

public record SendEmailVerificationResponse(long expiresInSeconds, long resendAvailableInSeconds) {}
