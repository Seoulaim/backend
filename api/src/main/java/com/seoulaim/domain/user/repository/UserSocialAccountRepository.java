package com.seoulaim.domain.user.repository;

import com.seoulaim.domain.user.entity.SocialProvider;
import com.seoulaim.domain.user.entity.UserSocialAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

  @Query(
      "SELECT sa FROM UserSocialAccount sa JOIN FETCH sa.user"
          + " WHERE sa.provider = :provider AND sa.providerUserId = :providerUserId")
  Optional<UserSocialAccount> findWithUser(
      @Param("provider") SocialProvider provider, @Param("providerUserId") String providerUserId);

  boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM UserSocialAccount sa WHERE sa.user.id = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
