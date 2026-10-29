package com.seoulaim.domain.artist.repository;

import com.seoulaim.domain.artist.entity.UserBias;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBiasRepository extends JpaRepository<UserBias, UUID> {

  // 등록순, 아티스트·그룹 함께 조회
  @Query(
      """
            SELECT b FROM UserBias b
            JOIN FETCH b.artist a
            JOIN FETCH a.artistGroup
            WHERE b.user.id = :userId
            ORDER BY b.createdAt ASC, b.id ASC
            """)
  List<UserBias> findAllWithArtistByUserId(@Param("userId") UUID userId);

  // 벌크 delete → 즉시 실행. 이후 같은 아티스트 재등록 시 unique 충돌 방지
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("DELETE FROM UserBias b WHERE b.user.id = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
