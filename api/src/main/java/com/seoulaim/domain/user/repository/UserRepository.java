package com.seoulaim.domain.user.repository;

import com.seoulaim.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

  // 탈퇴 회원은 email 이 null → 자동 제외
  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  /** 어드민 검색: 이메일·닉네임 부분 일치. pattern 은 소문자 + LIKE 이스케이프(\\) 된 %...% */
  @Query(
      """
      select u.id from User u
      where lower(u.email) like :pattern escape '\\' or lower(u.nickname) like :pattern escape '\\'
      """)
  List<UUID> findIdsByEmailOrNicknameLike(String pattern);
}
