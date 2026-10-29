package com.seoulaim.domain.user.service;

import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회·생성
 *
 * <ul>
 *   <li>email 은 소문자 정규화된 값으로 전달
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User getUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  public UserResponse getUserProfile(UUID userId) {
    return UserResponse.from(getUser(userId));
  }

  @Transactional
  public User createUser(String email, String passwordHash, String nickname) {
    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
    }
    try {
      // 동시 가입 → uk_users_email_password 위반을 즉시 감지하려고 flush
      return userRepository.saveAndFlush(User.createWithEmail(email, passwordHash, nickname));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
    }
  }
}
