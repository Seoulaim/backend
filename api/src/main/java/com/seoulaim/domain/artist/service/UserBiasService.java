package com.seoulaim.domain.artist.service;

import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.UserBias;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.artist.repository.UserBiasRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 bias 조회·저장
 *
 * <ul>
 *   <li>저장은 전체 교체 (기존 삭제 → 요청 순서대로 등록)
 *   <li>최대 개수는 요청 DTO 에서 검증
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserBiasService {

  private final UserBiasRepository userBiasRepository;
  private final ArtistRepository artistRepository;
  private final UserService userService;

  public List<ArtistWithGroupResponse> findMyBiases(UUID userId) {
    return userBiasRepository.findAllWithArtistByUserId(userId).stream()
        .map(bias -> toResponse(bias.getArtist()))
        .toList();
  }

  @Transactional
  public List<ArtistWithGroupResponse> replaceMyBiases(UUID userId, List<UUID> artistIds) {
    if (new HashSet<>(artistIds).size() != artistIds.size()) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
    }

    // 벌크 delete 가 영속성 컨텍스트를 비움 → 삭제 후에 user·artist 조회
    userBiasRepository.deleteAllByUserId(userId);
    if (artistIds.isEmpty()) {
      return List.of();
    }

    User user = userService.getUser(userId);
    Map<UUID, Artist> artists =
        artistRepository.findAllWithGroupByIdIn(artistIds).stream()
            .collect(Collectors.toMap(Artist::getId, Function.identity()));
    // 하나라도 없으면 예외 → 트랜잭션 롤백으로 기존 bias 유지
    if (artists.size() != artistIds.size()) {
      throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
    }

    userBiasRepository.saveAll(
        artistIds.stream().map(id -> UserBias.create(user, artists.get(id))).toList());

    return artistIds.stream().map(id -> toResponse(artists.get(id))).toList();
  }

  private ArtistWithGroupResponse toResponse(Artist artist) {
    return ArtistWithGroupResponse.of(artist, artist.getArtistGroup());
  }
}
