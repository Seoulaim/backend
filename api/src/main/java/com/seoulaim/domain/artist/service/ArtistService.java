package com.seoulaim.domain.artist.service;

import com.seoulaim.domain.artist.dto.ArtistGroupResponse;
import com.seoulaim.domain.artist.dto.ArtistResponse;
import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 그룹·아티스트 조회 (bias 선택 화면, 일정 추천) */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArtistService {

  private final ArtistGroupRepository artistGroupRepository;
  private final ArtistRepository artistRepository;

  public List<ArtistGroupResponse> findArtistGroups() {
    return artistGroupRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(ArtistGroupResponse::from)
        .toList();
  }

  // 멤버 없는 그룹은 빈 목록, 그룹 자체가 없으면 404
  public List<ArtistResponse> findArtistsByGroup(UUID artistGroupId) {
    if (!artistGroupRepository.existsById(artistGroupId)) {
      throw new BusinessException(ErrorCode.ARTIST_GROUP_NOT_FOUND);
    }
    return artistRepository
        .findAllByArtistGroupIdOrderByDisplayOrderAscIdAsc(artistGroupId)
        .stream()
        .map(ArtistResponse::from)
        .toList();
  }

  // size + 1 개 조회 → 남는 1개로 다음 페이지 유무 판단
  public CursorPageResponse<ArtistWithGroupResponse> searchArtists(
      String keyword, String cursor, int size) {
    String pattern = "%" + escapeLike(keyword.trim()) + "%";
    List<Artist> artists = artistRepository.searchByKeyword(pattern, parseCursor(cursor), size + 1);

    boolean hasNext = artists.size() > size;
    List<Artist> page = hasNext ? artists.subList(0, size) : artists;
    String nextCursor = hasNext ? page.getLast().getId().toString() : null;

    return new CursorPageResponse<>(toResponsesWithGroup(page), nextCursor);
  }

  // 없는 id 가 섞이면 ARTIST_NOT_FOUND
  public List<Artist> getArtistsWithGroup(Collection<UUID> artistIds) {
    List<Artist> artists = artistRepository.findAllWithGroupByIdIn(artistIds);
    if (artists.size() != new HashSet<>(artistIds).size()) {
      throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
    }
    return artists;
  }

  /** 주어진 그룹 id 중 존재하는 것만 (홈 콘텐츠 대상 그룹 검증용). */
  public Set<UUID> findExistingGroupIds(Collection<UUID> groupIds) {
    if (groupIds.isEmpty()) {
      return Set.of();
    }
    return artistGroupRepository.findAllById(groupIds).stream()
        .map(ArtistGroup::getId)
        .collect(Collectors.toSet());
  }

  /** 아티스트 id → 소속 그룹 id. 없는 아티스트는 결과에서 빠진다 (생일카페 검증·최애 필터용). */
  public Map<UUID, UUID> findGroupIdsByArtistIds(Collection<UUID> artistIds) {
    if (artistIds.isEmpty()) {
      return Map.of();
    }
    return artistRepository.findAllById(artistIds).stream()
        .collect(Collectors.toMap(Artist::getId, artist -> artist.getArtistGroup().getId()));
  }

  // 그룹을 한 번에 조회 → 아티스트마다 그룹 조회(N+1) 방지
  public List<ArtistWithGroupResponse> toResponsesWithGroup(List<Artist> artists) {
    List<UUID> groupIds =
        artists.stream().map(artist -> artist.getArtistGroup().getId()).distinct().toList();
    Map<UUID, ArtistGroup> groups =
        artistGroupRepository.findAllById(groupIds).stream()
            .collect(Collectors.toMap(ArtistGroup::getId, Function.identity()));

    return artists.stream()
        .map(
            artist ->
                ArtistWithGroupResponse.of(artist, groups.get(artist.getArtistGroup().getId())))
        .toList();
  }

  // UUID 형식 검증 후 문자열 그대로 전달 (쿼리에서 uuid 로 CAST)
  private String parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(cursor).toString();
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.ARTIST_INVALID_CURSOR);
    }
  }

  // LIKE 특수문자를 문자 그대로 검색 (PostgreSQL 기본 이스케이프 문자 \)
  private String escapeLike(String keyword) {
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
