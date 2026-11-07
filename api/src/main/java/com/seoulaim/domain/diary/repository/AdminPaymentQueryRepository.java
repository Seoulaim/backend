package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 어드민 결제 관리 검색. 필터 조합이 많아 Criteria 로 조건을 한 번 만들고 목록·건수·합계에 같이 쓴다. */
@Repository
@RequiredArgsConstructor
public class AdminPaymentQueryRepository {

  private static final char LIKE_ESCAPE = '\\';

  private final EntityManager entityManager;

  /**
   * @param status null 이면 전체
   * @param keywordPattern {@code LikePattern.containing} 결과. null 이면 검색 안 함
   * @param keywordUserIds 검색어와 이메일·닉네임이 일치하는 회원 id. 주문번호 일치 OR 이 회원들의 결제
   * @param from created_at 이상 (null 이면 제한 없음)
   * @param to created_at 미만 (null 이면 제한 없음)
   */
  public record Filter(
      DiaryPaymentStatus status,
      String keywordPattern,
      Collection<UUID> keywordUserIds,
      Instant from,
      Instant to) {

    public Filter withoutStatus() {
      return new Filter(null, keywordPattern, keywordUserIds, from, to);
    }
  }

  /** 상태별 건수·금액 합계. */
  public record StatusTotal(DiaryPaymentStatus status, long count, long amount) {}

  /** 최신순(created_at, id 내림차순). */
  public List<DiaryPayment> search(Filter filter, int page, int size) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<DiaryPayment> query = cb.createQuery(DiaryPayment.class);
    Root<DiaryPayment> root = query.from(DiaryPayment.class);
    query
        .select(root)
        .where(predicates(cb, root, filter))
        .orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
    return entityManager
        .createQuery(query)
        .setFirstResult(Math.multiplyExact(page, size))
        .setMaxResults(size)
        .getResultList();
  }

  public long count(Filter filter) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<DiaryPayment> root = query.from(DiaryPayment.class);
    query.select(cb.count(root)).where(predicates(cb, root, filter));
    return entityManager.createQuery(query).getSingleResult();
  }

  public List<StatusTotal> totalsByStatus(Filter filter) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = cb.createTupleQuery();
    Root<DiaryPayment> root = query.from(DiaryPayment.class);
    query
        .multiselect(root.get("status"), cb.count(root), cb.sum(root.<BigDecimal>get("amount")))
        .where(predicates(cb, root, filter))
        .groupBy(root.get("status"));
    return entityManager.createQuery(query).getResultList().stream()
        .map(
            tuple -> {
              BigDecimal sum = tuple.get(2, BigDecimal.class);
              long amount = sum == null ? 0 : sum.longValue();
              return new StatusTotal(
                  tuple.get(0, DiaryPaymentStatus.class), tuple.get(1, Long.class), amount);
            })
        .toList();
  }

  private static Predicate[] predicates(
      CriteriaBuilder cb, Root<DiaryPayment> root, Filter filter) {
    List<Predicate> predicates = new ArrayList<>();
    if (filter.status() != null) {
      predicates.add(cb.equal(root.get("status"), filter.status()));
    }
    if (filter.from() != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
    }
    if (filter.to() != null) {
      predicates.add(cb.lessThan(root.get("createdAt"), filter.to()));
    }
    if (filter.keywordPattern() != null) {
      Predicate orderNumber =
          cb.like(cb.lower(root.get("orderNumber")), filter.keywordPattern(), LIKE_ESCAPE);
      predicates.add(
          filter.keywordUserIds().isEmpty()
              ? orderNumber
              : cb.or(orderNumber, root.get("userId").in(filter.keywordUserIds())));
    }
    return predicates.toArray(Predicate[]::new);
  }
}
