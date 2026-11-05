package com.seoulaim.domain.inquiry.repository;

import com.seoulaim.domain.inquiry.entity.InquiryAnswer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, UUID> {

  List<InquiryAnswer> findAllByInquiryIdInOrderBySentAtAscIdAsc(Collection<UUID> inquiryIds);
}
