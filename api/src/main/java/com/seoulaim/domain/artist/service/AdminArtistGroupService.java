package com.seoulaim.domain.artist.service;

import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupListResponse;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupRequest;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupRequest.AdminArtistMemberRequest;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupResponse;
import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 그룹·멤버 관리
 *
 * <ul>
 *   <li>새로 바뀐 사진 key 만 파일 연결 처리 (기존 key 는 그대로)
 *   <li>멤버 삭제 없음 → bias·장소 연결·굿즈가 멤버를 참조
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminArtistGroupService {

  private final ArtistGroupRepository artistGroupRepository;
  private final ArtistRepository artistRepository;
  private final FileService fileService;

  public AdminArtistGroupListResponse getGroups() {
    List<ArtistGroup> groups = artistGroupRepository.findAllByOrderByDisplayOrderAscIdAsc();
    if (groups.isEmpty()) {
      return new AdminArtistGroupListResponse(List.of());
    }
    Map<UUID, List<Artist>> membersByGroupId =
        artistRepository
            .findAllByArtistGroupIdInOrderByDisplayOrderAscIdAsc(
                groups.stream().map(ArtistGroup::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(artist -> artist.getArtistGroup().getId()));
    return new AdminArtistGroupListResponse(
        groups.stream()
            .map(
                group ->
                    AdminArtistGroupResponse.of(
                        group, membersByGroupId.getOrDefault(group.getId(), List.of())))
            .toList());
  }

  @Transactional
  public AdminArtistGroupResponse createGroup(AdminArtistGroupRequest request) {
    List<String> newImageKeys = new ArrayList<>();
    addIfChanged(newImageKeys, request.imageKey(), null);
    request.membersOrEmpty().forEach(member -> addIfChanged(newImageKeys, member.imageKey(), null));
    fileService.markLinkedByS3Keys(newImageKeys, FileDomain.ARTIST);

    ArtistGroup group =
        artistGroupRepository.save(
            ArtistGroup.create(
                request.nameEn(),
                request.nameKo(),
                request.nameJa(),
                request.nameZh(),
                request.imageKey(),
                request.debutDate(),
                request.displayOrderOrDefault()));

    List<AdminArtistMemberRequest> memberRequests = request.membersOrEmpty();
    List<Artist> members = new ArrayList<>();
    for (int order = 0; order < memberRequests.size(); order++) {
      if (memberRequests.get(order).id() != null) {
        throw new BusinessException(ErrorCode.ARTIST_NOT_IN_GROUP);
      }
      members.add(artistRepository.save(newMember(group, memberRequests.get(order), order)));
    }
    return AdminArtistGroupResponse.of(group, members);
  }

  @Transactional
  public AdminArtistGroupResponse updateGroup(UUID groupId, AdminArtistGroupRequest request) {
    ArtistGroup group =
        artistGroupRepository
            .findById(groupId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ARTIST_GROUP_NOT_FOUND));
    Map<UUID, Artist> existing =
        artistRepository.findAllByArtistGroupIdOrderByDisplayOrderAscIdAsc(groupId).stream()
            .collect(Collectors.toMap(Artist::getId, Function.identity()));

    List<AdminArtistMemberRequest> memberRequests = request.membersOrEmpty();
    List<String> newImageKeys = new ArrayList<>();
    addIfChanged(newImageKeys, request.imageKey(), group.getImageKey());
    for (AdminArtistMemberRequest member : memberRequests) {
      Artist current = null;
      if (member.id() != null) {
        current = existing.get(member.id());
        if (current == null) {
          throw new BusinessException(ErrorCode.ARTIST_NOT_IN_GROUP);
        }
      }
      addIfChanged(newImageKeys, member.imageKey(), current == null ? null : current.getImageKey());
    }
    fileService.markLinkedByS3Keys(newImageKeys, FileDomain.ARTIST);

    group.update(
        request.nameEn(),
        request.nameKo(),
        request.nameJa(),
        request.nameZh(),
        request.imageKey(),
        request.debutDate(),
        request.displayOrderOrDefault());

    for (int order = 0; order < memberRequests.size(); order++) {
      AdminArtistMemberRequest member = memberRequests.get(order);
      if (member.id() == null) {
        artistRepository.save(newMember(group, member, order));
      } else {
        existing
            .get(member.id())
            .update(
                member.nameEn(),
                member.nameKo(),
                member.nameJa(),
                member.nameZh(),
                member.imageKey(),
                member.birthDate(),
                order);
      }
    }
    // updatedAt 반영 후 응답
    artistGroupRepository.flush();

    List<Artist> members =
        artistRepository.findAllByArtistGroupIdOrderByDisplayOrderAscIdAsc(groupId).stream()
            .sorted(Comparator.comparingInt(Artist::getDisplayOrder))
            .toList();
    return AdminArtistGroupResponse.of(group, members);
  }

  private Artist newMember(ArtistGroup group, AdminArtistMemberRequest member, int order) {
    return Artist.create(
        group,
        member.nameEn(),
        member.nameKo(),
        member.nameJa(),
        member.nameZh(),
        member.imageKey(),
        member.birthDate(),
        order);
  }

  // 비어 있지 않고 기존 값과 다른 key 만 연결 대상
  private void addIfChanged(List<String> keys, String imageKey, String currentKey) {
    if (imageKey != null && !imageKey.isBlank() && !Objects.equals(imageKey, currentKey)) {
      keys.add(imageKey);
    }
  }
}
