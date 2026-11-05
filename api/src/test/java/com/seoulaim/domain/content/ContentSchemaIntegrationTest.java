package com.seoulaim.domain.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import com.seoulaim.domain.content.entity.BirthdayCafe;
import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.entity.CalendarEntry;
import com.seoulaim.domain.content.repository.BannerRepository;
import com.seoulaim.domain.content.repository.BirthdayCafeRepository;
import com.seoulaim.domain.content.repository.CalendarEntryRepository;
import com.seoulaim.domain.content.service.AdminBannerService;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.repository.FileRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class ContentSchemaIntegrationTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private BannerRepository bannerRepository;
  @Autowired private CalendarEntryRepository calendarEntryRepository;
  @Autowired private BirthdayCafeRepository birthdayCafeRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private FileRepository fileRepository;
  @Autowired private AdminBannerService adminBannerService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("마이그레이션이 HOME·DIARY 기본 배너를 1개씩 만들고, 위치별 두 번째 기본 배너는 거부된다")
  void migration_기본배너2개_위치별유일() {
    for (BannerPlacement placement : BannerPlacement.values()) {
      List<Banner> defaults =
          bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(placement).stream()
              .filter(Banner::isDefault)
              .toList();
      assertThat(defaults).hasSize(1);
      Banner banner = defaults.getFirst();
      assertThat(banner.getAdminTitle()).isEqualTo("Default");
      assertThat(banner.getTitle()).containsExactly(Map.entry("en", "Seoulaim"));
      assertThat(banner.getImageKey()).isNull();
      assertThat(banner.isEnabled()).isTrue();
      assertThat(banner.isVisibleOn(TODAY)).isTrue();
    }

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO banners (id, placement, admin_title, title, target_type,
                      is_default, display_order, created_at, updated_at)
                    VALUES (?, 'HOME', 'dup', '{"en":"x"}', 'DIARY', true, 9, now(), now())
                    """,
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("기본 배너 전환이 위치별 unique index·CHECK 제약을 어기지 않고 반영되고, 해제된 배너는 삭제할 수 있다")
  void changeDefault_제약충족_해제후삭제() {
    Banner original =
        bannerRepository
            .findAllByPlacementOrderByDisplayOrderAscIdAsc(BannerPlacement.HOME)
            .stream()
            .filter(Banner::isDefault)
            .findFirst()
            .orElseThrow();
    Banner candidate =
        bannerRepository.saveAndFlush(
            Banner.create(
                BannerPlacement.HOME,
                "새 기본 후보",
                null,
                Map.of("en", "Next"),
                null,
                null,
                BannerTargetType.DIARY,
                null,
                TODAY,
                TODAY.plusDays(1),
                50));
    try {
      adminBannerService.changeDefault(candidate.getId());

      Banner promoted = bannerRepository.findById(candidate.getId()).orElseThrow();
      assertThat(promoted.isDefault()).isTrue();
      assertThat(promoted.getStartDate()).isNull();
      assertThat(promoted.getEndDate()).isNull();
      Banner released = bannerRepository.findById(original.getId()).orElseThrow();
      assertThat(released.isDefault()).isFalse();
      assertThat(released.isEnabled()).isTrue();
    } finally {
      // 마이그레이션 기본 배너를 다른 테스트를 위해 되돌린다
      adminBannerService.changeDefault(original.getId());
    }
    adminBannerService.deleteBanner(candidate.getId());
    assertThat(bannerRepository.findById(candidate.getId())).isEmpty();
    assertThat(bannerRepository.findById(original.getId()).orElseThrow().isDefault()).isTrue();
  }

  @Test
  @DisplayName("일반 배너는 jsonb 문구·기간과 함께 저장·조회된다")
  void save_일반배너_왕복() {
    Banner saved =
        bannerRepository.saveAndFlush(
            Banner.create(
                BannerPlacement.DIARY,
                "다이어리 프로모션",
                "content/2026/11/b.jpg",
                Map.of("en", "Make every trip", "ko", "여행의 순간을"),
                Map.of("en", "Record"),
                Map.of("en", "Discover now"),
                BannerTargetType.URL,
                "https://example.com",
                TODAY,
                TODAY.plusDays(3),
                1));

    Banner found = bannerRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCtaLabel()).containsEntry("en", "Discover now");
    assertThat(found.getEndDate()).isEqualTo(TODAY.plusDays(3));
    assertThat(bannerRepository.findMaxDisplayOrder(BannerPlacement.DIARY))
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("캘린더 항목의 대상 그룹이 저장되고, 그룹이 삭제되면 매핑도 지워지며 조회 쿼리가 기간으로 거른다")
  void calendarEntry_그룹매핑과조회쿼리() {
    ArtistGroup group =
        artistGroupRepository.save(ArtistGroup.create("TEMP", "임시", null, null, null, null, 99));
    CalendarEntry upcoming =
        calendarEntryRepository.saveAndFlush(
            CalendarEntry.create(
                Map.of("en", "TOUR"),
                TODAY,
                TODAY.plusDays(2),
                true,
                CalendarColor.PURPLE,
                "content/2026/11/e.jpg",
                Map.of("en", "KSPO DOME"),
                "https://example.com",
                false,
                List.of(group.getId()),
                0));
    CalendarEntry ended =
        calendarEntryRepository.saveAndFlush(
            CalendarEntry.create(
                Map.of("en", "ENDED"),
                TODAY.minusDays(5),
                TODAY.minusDays(1),
                false,
                null,
                "content/2026/11/f.jpg",
                null,
                null,
                true,
                List.of(),
                1));

    transactionTemplate.executeWithoutResult(
        status -> {
          List<CalendarEntry> events =
              calendarEntryRepository
                  .findAllByImageKeyIsNotNullAndEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(
                      TODAY);
          assertThat(events).extracting(CalendarEntry::getId).contains(upcoming.getId());
          assertThat(events).extracting(CalendarEntry::getId).doesNotContain(ended.getId());
          assertThat(
                  events.stream()
                      .filter(e -> e.getId().equals(upcoming.getId()))
                      .findFirst()
                      .orElseThrow()
                      .getArtistGroupIds())
              .containsExactly(group.getId());
          assertThat(
                  calendarEntryRepository
                      .findAllByInCalendarTrueAndStartDateBetweenOrderByStartDateAscDisplayOrderAscIdAsc(
                          TODAY, TODAY))
              .extracting(CalendarEntry::getId)
              .contains(upcoming.getId())
              .doesNotContain(ended.getId());
        });

    jdbcTemplate.update("DELETE FROM artist_groups WHERE id = ?", group.getId());
    Integer mappings =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM calendar_entry_groups WHERE entry_id = ?",
            Integer.class,
            upcoming.getId());
    assertThat(mappings).isZero();
  }

  @Test
  @DisplayName("생일카페는 멤버가 삭제되면 함께 지워진다")
  void birthdayCafe_멤버삭제시_삭제() {
    ArtistGroup group =
        artistGroupRepository.save(ArtistGroup.create("CAFE", "카페", null, null, null, null, 98));
    Artist member =
        artistRepository.save(Artist.create(group, "MEMBER", "멤버", null, null, null, null, 1));
    BirthdayCafe cafe =
        birthdayCafeRepository.saveAndFlush(
            BirthdayCafe.create(
                member.getId(), Map.of("en", "Cafe"), TODAY, TODAY.plusDays(2), null, null, 0));

    assertThat(
            birthdayCafeRepository.findAllByEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(
                TODAY))
        .extracting(BirthdayCafe::getId)
        .contains(cafe.getId());

    jdbcTemplate.update("DELETE FROM artists WHERE id = ?", member.getId());
    assertThat(birthdayCafeRepository.findById(cafe.getId())).isEmpty();
  }

  @Test
  @DisplayName("files 에 CONTENT 도메인 파일을 저장할 수 있다")
  void files_CONTENT도메인_허용() {
    File file =
        fileRepository.saveAndFlush(
            File.createPending(
                null,
                FileDomain.CONTENT,
                "content/2026/11/" + UUID.randomUUID() + ".jpg",
                "image/jpeg",
                1024L));

    assertThat(fileRepository.findById(file.getId())).isPresent();
  }
}
