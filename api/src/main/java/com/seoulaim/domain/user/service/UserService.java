package com.seoulaim.domain.user.service;

import com.seoulaim.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  // email 은 소문자 정규화된 값으로 전달
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }
}
