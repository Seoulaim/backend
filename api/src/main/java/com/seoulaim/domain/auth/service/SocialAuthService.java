package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.auth.dto.SocialLoginResponse;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserSocialAccount;
import com.seoulaim.domain.user.repository.UserSocialAccountRepository;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인·가입 (구글)
 *
 * <ul>
 *   <li>회원 식별: 제공자 고유 id(sub) 로 연결된 계정
 *   <li>연결 계정이 없고 제공자가 인증한 이메일이 기존 회원과 같으면 자동 연결 후 로그인
 *   <li>그 외에는 신규 → 로그인 API 는 signupRequired, 가입 API 에서 회원 생성
 *   <li>가입 API 도 기존 회원이면 그대로 로그인 (중복 요청 안전)
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SocialAuthService {

  private static final int NICKNAME_MAX_LENGTH = 20;

  private final GoogleIdTokenVerifier googleIdTokenVerifier;
  private final UserSocialAccountRepository socialAccountRepository;
  private final UserService userService;
  private final AuthService authService;

  @Transactional
  public SocialLoginResponse loginWithGoogle(String idToken) {
    SocialProfile profile = googleIdTokenVerifier.verify(idToken);
    return findOrLink(profile)
        .map(user -> SocialLoginResponse.loggedIn(issueTokens(user)))
        .orElseGet(
            () -> SocialLoginResponse.signupRequired(profile.email(), suggestNickname(profile)));
  }

  @Transactional
  public TokenResponse signupWithGoogle(String idToken, String nickname) {
    SocialProfile profile = googleIdTokenVerifier.verify(idToken);
    User user = findOrLink(profile).orElseGet(() -> createUser(profile, nickname.trim()));
    return issueTokens(user);
  }

  // 연결된 계정 → 그 회원. 없으면 인증된 이메일이 같은 회원에 연결
  private Optional<User> findOrLink(SocialProfile profile) {
    Optional<UserSocialAccount> account =
        socialAccountRepository.findWithUser(profile.provider(), profile.providerUserId());
    if (account.isPresent()) {
      return Optional.of(account.get().getUser());
    }
    if (profile.email() == null) {
      return Optional.empty();
    }
    return userService
        .findByEmail(EmailNormalizer.normalize(profile.email()))
        .map(
            user -> {
              link(user, profile);
              return user;
            });
  }

  private User createUser(SocialProfile profile, String nickname) {
    String email = profile.email() == null ? null : EmailNormalizer.normalize(profile.email());
    User user = userService.createSocialUser(email, nickname);
    link(user, profile);
    return user;
  }

  // 같은 소셜 계정 동시 요청 → unique 제약 위반을 즉시 감지
  private void link(User user, SocialProfile profile) {
    try {
      socialAccountRepository.saveAndFlush(
          UserSocialAccount.create(user, profile.provider(), profile.providerUserId()));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.AUTH_SOCIAL_ALREADY_LINKED);
    }
  }

  private TokenResponse issueTokens(User user) {
    if (!user.canLogin()) {
      throw new BusinessException(ErrorCode.USER_SUSPENDED);
    }
    return authService.issueTokens(user.getId());
  }

  // 제공자 이름 → 없으면 이메일 앞부분 → 없으면 빈 문자열 (최대 20자)
  private static String suggestNickname(SocialProfile profile) {
    String base = profile.name();
    if ((base == null || base.isBlank()) && profile.email() != null) {
      int at = profile.email().indexOf('@');
      base = at > 0 ? profile.email().substring(0, at) : null;
    }
    if (base == null) {
      return "";
    }
    String trimmed = base.trim();
    return trimmed.length() > NICKNAME_MAX_LENGTH
        ? trimmed.substring(0, NICKNAME_MAX_LENGTH)
        : trimmed;
  }
}
