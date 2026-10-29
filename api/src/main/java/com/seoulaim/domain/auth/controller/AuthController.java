package com.seoulaim.domain.auth.controller;

import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationRequest;
import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.LoginRequest;
import com.seoulaim.domain.auth.dto.RefreshTokenRequest;
import com.seoulaim.domain.auth.dto.SendEmailVerificationRequest;
import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.SignupRequest;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.service.AuthService;
import com.seoulaim.domain.auth.service.EmailVerificationService;
import com.seoulaim.global.common.CurrentUserId;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 빈 @SecurityRequirements → Swagger 에서 Bearer 인증 표시 제외 (로그아웃만 메서드에서 지정)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

  private final EmailVerificationService emailVerificationService;
  private final AuthService authService;

  @Operation(summary = "이메일 인증번호 발송")
  @PostMapping("/email-verifications")
  @ResponseStatus(HttpStatus.CREATED)
  public SendEmailVerificationResponse sendEmailVerification(
      @Valid @RequestBody SendEmailVerificationRequest request) {
    return emailVerificationService.sendVerificationCode(request.email());
  }

  @Operation(summary = "이메일 인증번호 확인")
  @PostMapping("/email-verifications/confirm")
  public ConfirmEmailVerificationResponse confirmEmailVerification(
      @Valid @RequestBody ConfirmEmailVerificationRequest request) {
    return emailVerificationService.confirmVerificationCode(request.email(), request.code());
  }

  @Operation(summary = "회원가입")
  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
    return authService.signup(request);
  }

  @Operation(summary = "로그인")
  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.email(), request.password());
  }

  @Operation(summary = "토큰 재발급")
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request.refreshToken());
  }

  @Operation(summary = "로그아웃")
  @SecurityRequirement(name = SwaggerConfig.BEARER_SCHEME)
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@CurrentUserId UUID userId, @Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(userId, request.refreshToken());
  }
}
