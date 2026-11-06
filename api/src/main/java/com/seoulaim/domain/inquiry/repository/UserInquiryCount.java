package com.seoulaim.domain.inquiry.repository;

import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import java.util.UUID;

/** 회원·문의 상태·환불 상태별 문의 수 projection. */
public interface UserInquiryCount {

  UUID getUserId();

  InquiryStatus getStatus();

  RefundStatus getRefundStatus();

  long getCount();
}
