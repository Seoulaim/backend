package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceFavorite;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 즐겨찾기
 *
 * <ul>
 *   <li>등록·해제 모두 반복 호출해도 결과 동일
 *   <li>등록은 클래스 트랜잭션 없이 실행 → 동시 요청 unique 충돌을 잡아도 롤백 전파 없음
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PlaceFavoriteService {

  private final PlaceFavoriteRepository placeFavoriteRepository;
  private final PlaceService placeService;
  private final UserService userService;

  public void addFavorite(UUID userId, UUID placeId) {
    Place place = placeService.getPlace(placeId);
    if (placeFavoriteRepository.existsByUserIdAndPlaceId(userId, placeId)) {
      return;
    }
    User user = userService.getUser(userId);
    try {
      placeFavoriteRepository.save(PlaceFavorite.create(user, place));
    } catch (DataIntegrityViolationException exception) {
      // 같은 요청이 동시에 들어와 먼저 등록됨 → 결과 동일
    }
  }

  @Transactional
  public void removeFavorite(UUID userId, UUID placeId) {
    placeFavoriteRepository.deleteByUserIdAndPlaceId(userId, placeId);
  }
}
