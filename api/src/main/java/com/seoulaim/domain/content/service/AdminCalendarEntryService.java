package com.seoulaim.domain.content.service;

import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryRequest;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryResponse;
import com.seoulaim.domain.content.entity.CalendarEntry;
import com.seoulaim.domain.content.repository.CalendarEntryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 이벤트 관리: 팬캘린더·Event coming up 항목 CRUD. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCalendarEntryService {

  private final CalendarEntryRepository calendarEntryRepository;
  private final ArtistService artistService;
  private final ContentImageResolver contentImageResolver;

  public List<AdminCalendarEntryResponse> getEntries() {
    return calendarEntryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AdminCalendarEntryResponse::from)
        .toList();
  }

  public AdminCalendarEntryResponse getEntry(UUID entryId) {
    return AdminCalendarEntryResponse.from(findEntry(entryId));
  }

  /** 맨 뒤 순서로 추가한다. */
  @Transactional
  public AdminCalendarEntryResponse createEntry(AdminCalendarEntryRequest request) {
    Fields fields = validate(request, null);
    CalendarEntry entry =
        calendarEntryRepository.save(
            CalendarEntry.create(
                fields.title(),
                request.startDate(),
                request.endDate(),
                request.inCalendar(),
                request.color(),
                fields.imageKey(),
                fields.venue(),
                fields.targetUrl(),
                fields.allGroups(),
                fields.groupIds(),
                calendarEntryRepository.findMaxDisplayOrder() + 1));
    return AdminCalendarEntryResponse.from(entry);
  }

  @Transactional
  public AdminCalendarEntryResponse updateEntry(UUID entryId, AdminCalendarEntryRequest request) {
    CalendarEntry entry = findEntry(entryId);
    Fields fields = validate(request, entry.getImageKey());
    entry.update(
        fields.title(),
        request.startDate(),
        request.endDate(),
        request.inCalendar(),
        request.color(),
        fields.imageKey(),
        fields.venue(),
        fields.targetUrl(),
        fields.allGroups(),
        fields.groupIds());
    return AdminCalendarEntryResponse.from(entry);
  }

  @Transactional
  public void deleteEntry(UUID entryId) {
    calendarEntryRepository.delete(findEntry(entryId));
  }

  private CalendarEntry findEntry(UUID entryId) {
    return calendarEntryRepository
        .findById(entryId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
  }

  // 검증 후 저장할 값. 이미지 연결(LINKED)은 다른 검증을 모두 통과한 뒤 마지막에 한다
  private Fields validate(AdminCalendarEntryRequest request, String currentImageKey) {
    Map<String, String> title = LocalizedTexts.required(request.title(), "title");
    Map<String, String> venue = LocalizedTexts.optionalOrNull(request.venue(), "venue");
    if (request.inCalendar() && request.color() == null) {
      throw LocalizedTexts.invalid("color", "팬캘린더에 넣는 항목은 색이 필요합니다");
    }
    if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
      throw LocalizedTexts.invalid("endDate", "시작일보다 빠를 수 없습니다");
    }
    boolean allGroups =
        request.groupIds().stream()
            .anyMatch(id -> AdminCalendarEntryResponse.ALL_GROUPS.equalsIgnoreCase(id.strip()));
    Set<UUID> groupIds = allGroups ? Set.of() : parseExistingGroupIds(request.groupIds());

    boolean willHaveImage =
        request.imageFileId() != null
            || (!Boolean.TRUE.equals(request.removeImage()) && currentImageKey != null);
    if (!request.inCalendar() && !willHaveImage) {
      throw LocalizedTexts.invalid("imageFileId", "팬캘린더에 넣지 않는 항목은 사진이 필요합니다");
    }
    if (willHaveImage && request.endDate() == null) {
      throw LocalizedTexts.invalid("endDate", "사진이 있는 항목(Event coming up)은 종료일이 필요합니다");
    }
    String imageKey =
        contentImageResolver.resolve(currentImageKey, request.imageFileId(), request.removeImage());
    return new Fields(
        title, venue, blankToNull(request.targetUrl()), allGroups, groupIds, imageKey);
  }

  // UUID 형식이 아니거나 없는 그룹이면 CONTENT_INVALID_GROUP (fields.groupIds 에 문제 id)
  private Set<UUID> parseExistingGroupIds(List<String> rawIds) {
    Set<UUID> groupIds = new LinkedHashSet<>();
    for (String raw : rawIds) {
      try {
        groupIds.add(UUID.fromString(raw.strip()));
      } catch (IllegalArgumentException e) {
        throw new BusinessException(ErrorCode.CONTENT_INVALID_GROUP, Map.of("groupIds", raw));
      }
    }
    Set<UUID> existing = artistService.findExistingGroupIds(groupIds);
    List<String> missing =
        groupIds.stream().filter(id -> !existing.contains(id)).map(UUID::toString).toList();
    if (!missing.isEmpty()) {
      throw new BusinessException(
          ErrorCode.CONTENT_INVALID_GROUP, Map.of("groupIds", String.join(",", missing)));
    }
    return groupIds;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private record Fields(
      Map<String, String> title,
      Map<String, String> venue,
      String targetUrl,
      boolean allGroups,
      Set<UUID> groupIds,
      String imageKey) {}
}
