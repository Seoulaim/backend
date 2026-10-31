package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceEvent;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceEventRepository extends JpaRepository<PlaceEvent, UUID> {

  // 기간이 겹치는 이벤트가 여럿이면 가장 최근 시작한 이벤트 우선
  @Query(
      """
          SELECT e FROM PlaceEvent e
          WHERE e.place.id = :placeId AND e.startsOn <= :date AND e.endsOn >= :date
          ORDER BY e.startsOn DESC, e.id DESC
          """)
  List<PlaceEvent> findOngoingByPlaceId(
      @Param("placeId") UUID placeId, @Param("date") LocalDate date);

  // 기간과 하루라도 겹치는 이벤트
  @Query(
      """
          SELECT e FROM PlaceEvent e
          WHERE e.place.id IN :placeIds AND e.startsOn <= :endDate AND e.endsOn >= :startDate
          """)
  List<PlaceEvent> findAllInPeriod(
      @Param("placeIds") Collection<UUID> placeIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
