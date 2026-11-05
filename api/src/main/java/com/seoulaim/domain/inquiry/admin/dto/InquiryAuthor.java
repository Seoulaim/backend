package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.user.dto.UserResponse;
import java.util.UUID;

/**
 * 문의 작성자. 탈퇴 회원은 email 이 null.
 *
 * @param id 회원 id
 */
public record InquiryAuthor(UUID id, String nickname, String email) {

  /** profile 이 null 이면 (조회 불가) id 만 채운다. */
  public static InquiryAuthor of(UUID userId, UserResponse profile) {
    return profile == null
        ? new InquiryAuthor(userId, null, null)
        : new InquiryAuthor(userId, profile.nickname(), profile.email());
  }
}
