package com.seoulaim.domain.user.repository;

import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.global.common.KstDateCount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  /** 어드민 회원 목록: 상태·검색어(이메일·닉네임 부분 일치)는 null 이면 조건 없음. 가입 최신순. */
  @Query(
      """
      select u from User u
      where (:status is null or u.status = :status)
        and (:pattern is null
          or lower(u.email) like :pattern escape '\\'
          or lower(u.nickname) like :pattern escape '\\')
      order by u.createdAt desc, u.id desc
      """)
  Page<User> searchForAdmin(UserStatus status, String pattern, Pageable pageable);

  // 대시보드 ---------------------------------------------------------------

  long countByStatusNot(UserStatus status);

  long countByWithdrawnAtGreaterThanEqualAndWithdrawnAtLessThan(Instant from, Instant to);

  /** [from, to) 가입 수를 KST 날짜별로. 탈퇴한 회원도 가입 시점에 센다. 가입이 없는 날은 행이 없다. */
  @Query(
      value =
          """
          SELECT to_char(u.created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day,
                 COUNT(*) AS count
          FROM users u
          WHERE u.created_at >= :from AND u.created_at < :to
          GROUP BY day
          """,
      nativeQuery = true)
  List<KstDateCount> countSignupsByKstDate(Instant from, Instant to);
}
