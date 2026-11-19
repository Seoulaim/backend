package com.seoulaim.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// idToken: 로그인 때와 같은 ID 토큰 (구글 기준 1시간 유효), nickname: 1~20자
public record SocialSignupRequest(
    @NotBlank String idToken, @NotBlank @Size(max = 20) String nickname) {}
