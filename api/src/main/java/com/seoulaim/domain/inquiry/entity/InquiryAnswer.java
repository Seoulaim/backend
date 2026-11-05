package com.seoulaim.domain.inquiry.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 어드민 답변 1건. 회원 이메일로 보낸 내용 그대로 남긴다. */
@Getter
@Entity
@Table(name = "inquiry_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAnswer extends BaseEntity {

  @Column(name = "inquiry_id", nullable = false)
  private UUID inquiryId;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  public static InquiryAnswer create(UUID inquiryId, String body, Instant sentAt) {
    InquiryAnswer answer = new InquiryAnswer();
    answer.inquiryId = inquiryId;
    answer.body = body;
    answer.sentAt = sentAt;
    return answer;
  }
}
