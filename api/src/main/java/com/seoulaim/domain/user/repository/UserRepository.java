package com.seoulaim.domain.user.repository;

import com.seoulaim.domain.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  // 탈퇴 회원은 email 이 null → 자동 제외
  boolean existsByEmail(String email);
}
