package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 어드민 화면용 문의 작성자(닉네임·이메일)를 UserService 로 한 번에 불러온다. */
@Component
@RequiredArgsConstructor
class InquiryAuthorLoader {

  private final UserService userService;

  Map<UUID, InquiryAuthor> load(Collection<Inquiry> inquiries) {
    Map<UUID, UserResponse> profiles =
        userService
            .findProfiles(inquiries.stream().map(Inquiry::getUserId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(UserResponse::id, Function.identity()));
    return inquiries.stream()
        .map(Inquiry::getUserId)
        .distinct()
        .collect(
            Collectors.toMap(
                Function.identity(), userId -> InquiryAuthor.of(userId, profiles.get(userId))));
  }

  InquiryAuthor load(Inquiry inquiry) {
    return load(List.of(inquiry)).get(inquiry.getUserId());
  }
}
