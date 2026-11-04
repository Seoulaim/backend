package com.seoulaim.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryRequest;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryResponse;
import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.entity.CalendarEntry;
import com.seoulaim.domain.content.repository.CalendarEntryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCalendarEntryServiceTest {

  private static final LocalDate START = LocalDate.of(2026, 11, 10);
  private static final UUID ATEEZ = UUID.randomUUID();
  private static final String IMAGE_KEY = "content/2026/11/a.jpg";

  @Mock private CalendarEntryRepository calendarEntryRepository;
  @Mock private ArtistService artistService;
  @Mock private ContentImageResolver contentImageResolver;

  private AdminCalendarEntryService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminCalendarEntryService(calendarEntryRepository, artistService, contentImageResolver);
  }

  private static AdminCalendarEntryRequest request(
      List<String> groupIds, boolean inCalendar, UUID imageFileId, LocalDate endDate) {
    return new AdminCalendarEntryRequest(
        groupIds,
        Map.of("en", "ATEEZ TOUR"),
        START,
        inCalendar,
        inCalendar ? CalendarColor.PURPLE : null,
        imageFileId,
        null,
        endDate,
        Map.of("en", "KSPO DOME"),
        "https://example.com");
  }

  @Test
  @DisplayName("groupIds 가 [\"all\"] 이면 그룹 검증 없이 모든 회원 대상으로 저장하고 응답도 [\"all\"] 이다")
  void createEntry_all_전체대상() {
    given(calendarEntryRepository.findMaxDisplayOrder()).willReturn(-1);
    given(calendarEntryRepository.save(any(CalendarEntry.class)))
        .willAnswer(inv -> inv.getArgument(0));

    AdminCalendarEntryResponse response =
        service.createEntry(request(List.of("all"), true, null, null));

    assertThat(response.groupIds()).containsExactly("all");
    assertThat(response.displayOrder()).isZero();
    verifyNoInteractions(artistService);
  }

  @Test
  @DisplayName("없는 그룹 id 나 UUID 가 아닌 값이면 CONTENT_INVALID_GROUP")
  void createEntry_없는그룹_400() {
    UUID missing = UUID.randomUUID();
    given(artistService.findExistingGroupIds(any())).willReturn(Set.of(ATEEZ));

    assertThatThrownBy(
            () ->
                service.createEntry(
                    request(List.of(ATEEZ.toString(), missing.toString()), true, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CONTENT_INVALID_GROUP);
    assertThatThrownBy(() -> service.createEntry(request(List.of("ateez"), true, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CONTENT_INVALID_GROUP);
  }

  @Test
  @DisplayName("팬캘린더에 넣지 않는 항목에 사진이 없으면 COMMON_INVALID_REQUEST")
  void createEntry_캘린더아님사진없음_400() {
    assertThatThrownBy(
            () -> service.createEntry(request(List.of("all"), false, null, START.plusDays(1))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("사진이 있는데 종료일이 없으면 COMMON_INVALID_REQUEST")
  void createEntry_사진있고종료일없음_400() {
    assertThatThrownBy(
            () -> service.createEntry(request(List.of("all"), true, UUID.randomUUID(), null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("수정 시 imageFileId 를 생략하면 기존 사진을 유지하고 그룹을 교체한다")
  void updateEntry_이미지생략_기존유지() {
    CalendarEntry entry =
        CalendarEntry.create(
            Map.of("en", "old"),
            START,
            START.plusDays(1),
            false,
            null,
            IMAGE_KEY,
            null,
            null,
            true,
            List.of(),
            0);
    given(calendarEntryRepository.findById(entry.getId())).willReturn(Optional.of(entry));
    given(artistService.findExistingGroupIds(any())).willReturn(Set.of(ATEEZ));
    given(contentImageResolver.resolve(IMAGE_KEY, null, null)).willReturn(IMAGE_KEY);

    AdminCalendarEntryResponse response =
        service.updateEntry(
            entry.getId(), request(List.of(ATEEZ.toString()), false, null, START.plusDays(2)));

    assertThat(response.imageKey()).isEqualTo(IMAGE_KEY);
    assertThat(response.groupIds()).containsExactly(ATEEZ.toString());
    assertThat(entry.isAllGroups()).isFalse();
  }
}
