package com.seoulaim.domain.artist.service;

import com.seoulaim.domain.artist.dto.ArtistGroupResponse;
import com.seoulaim.domain.artist.dto.ArtistResponse;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 그룹·아티스트 조회 (bias 선택 화면) */
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
}
