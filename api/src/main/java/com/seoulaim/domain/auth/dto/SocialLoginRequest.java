package com.seoulaim.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

// idToken: 구글 로그인(Google Identity Services)으로 받은 ID 토큰
public record SocialLoginRequest(@NotBlank String idToken) {}
