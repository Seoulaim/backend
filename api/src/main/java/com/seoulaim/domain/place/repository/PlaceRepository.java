package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.Place;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// 어드민 목록 필터 → Specification
public interface PlaceRepository
    extends JpaRepository<Place, UUID>, JpaSpecificationExecutor<Place> {}
