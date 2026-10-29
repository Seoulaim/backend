package com.seoulaim.domain.auth.controller;

import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationRequest;
import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.SendEmailVerificationRequest;
import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 빈 @SecurityRequirements → Swagger 에서 Bearer 인증 표시 제외 (공개 API)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

  private final EmailVerificationService emailVerificationService;

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
}
