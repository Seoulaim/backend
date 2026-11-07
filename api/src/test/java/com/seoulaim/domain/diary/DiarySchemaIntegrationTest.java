package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.entity.DiaryStatus;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import com.seoulaim.domain.diary.repository.DiaryDownloadRepository;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.order.gateway.PaymentMerchant;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class DiarySchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private DiaryDownloadRepository diaryDownloadRepository;
  @Autowired private DiaryTemplateRepository diaryTemplateRepository;
  @Autowired private DiaryStickerRepository diaryStickerRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("diary v2 스키마에서 spreads·placePhotos·pageKeys 가 jsonb 로 그대로 저장·조회된다")
  void save_whenSchemaMigrated_editDataRoundTrips() {
    UUID userId = saveUser();
    List<Object> spreads =
        List.of(
            Map.of(
                "templateId",
                "t1",
                "left",
                Map.of(
                    "elements", List.of(Map.of("type", "sticker", "x", 12.5, "text", "사랑해 ❤")))));
    Map<String, Object> placePhotos =
        Map.of("other", List.of(Map.of("id", UUID.randomUUID().toString(), "key", "diary/a.jpg")));
    Diary diary = Diary.create(userId, true);
    diary.saveSpreads(spreads, placePhotos, 1, Instant.parse("2026-11-03T03:00:00Z"));
    diaryRepository.saveAndFlush(diary);

    Diary found =
        diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diary.getId(), userId).orElseThrow();
    assertThat(found.getSpreads()).isEqualTo(spreads);
    assertThat(found.getPlacePhotos()).isEqualTo(placePhotos);
    assertThat(found.getPageKeys()).isNull();
    assertThat(found.getStatus()).isEqualTo(DiaryStatus.DRAFT);
    assertThat(found.getId().version()).isEqualTo(7);

    found.finalizeWith(spreads, placePhotos, 1, Instant.parse("2026-11-03T04:00:00Z"));
    found.completePages(List.of("diary/p1.jpg", "diary/p2.jpg"));
    diaryRepository.saveAndFlush(found);

    Diary completed = diaryRepository.findById(diary.getId()).orElseThrow();
    assertThat(completed.getSaveCount()).isEqualTo(2);
    assertThat(completed.getLastSavedAt()).isEqualTo(Instant.parse("2026-11-03T04:00:00Z"));
    assertThat(completed.getPhotoCount()).isEqualTo(1);
    assertThat(completed.getPageKeys()).containsExactly("diary/p1.jpg", "diary/p2.jpg");
    assertThat(completed.getSpreads()).isEmpty();
    assertThat(completed.getPlacePhotos()).isEmpty();
    assertThat(completed.isFinalized()).isTrue();
  }

  @Test
  @DisplayName("목록은 삭제되지 않았고 무료이거나 결제된 다이어리만 최신순으로 조회한다 (미결제 유료 제외)")
  void findVisibleByUserId_whenUnpaidExists_excludesUnpaidAndDeleted() {
    UUID userId = saveUser();
    Diary free = diaryRepository.saveAndFlush(Diary.create(userId, true));
    Diary unpaid = diaryRepository.saveAndFlush(Diary.create(userId, false));
    Diary paid = Diary.create(userId, false);
    paid.markPaid();
    diaryRepository.saveAndFlush(paid);
    Diary deletedPaid = Diary.create(userId, false);
    deletedPaid.markPaid();
    deletedPaid.delete(Instant.parse("2026-11-03T03:00:00Z"));
    diaryRepository.saveAndFlush(deletedPaid);

    assertThat(diaryRepository.findVisibleByUserId(userId))
        .extracting(Diary::getId)
        .containsExactly(paid.getId(), free.getId());
    assertThat(diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(unpaid.getId(), userId))
        .isPresent();
    assertThat(diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(deletedPaid.getId(), userId))
        .isEmpty();
  }

  @Test
  @DisplayName("무료 다이어리 이력은 soft delete 후에도 남고, 사용자당 무료 다이어리는 하나만 저장된다")
  void existsByUserIdAndIsFreeTrue_whenDeleted_stillTrue() {
    UUID userId = saveUser();
    assertThat(diaryRepository.existsByUserIdAndIsFreeTrue(userId)).isFalse();
    Diary free = Diary.create(userId, true);
    free.delete(Instant.parse("2026-11-03T03:00:00Z"));
    diaryRepository.saveAndFlush(free);

    assertThat(diaryRepository.existsByUserIdAndIsFreeTrue(userId)).isTrue();
    assertThatThrownBy(() -> diaryRepository.saveAndFlush(Diary.create(userId, true)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("PayPal 결제는 method·merchant·currency 와 소수 금액 2.99 가 그대로 저장·조회된다")
  void save_PAYPAL결제_USD금액왕복() {
    UUID userId = saveUser();
    Diary diary = diaryRepository.saveAndFlush(Diary.create(userId, false));
    DiaryPayment paypal =
        DiaryPayment.createPending(
            diary.getId(),
            userId,
            "DIA-20261107-PAYPAL0001",
            DiaryPaymentMethod.PAYPAL,
            new BigDecimal("2.99"),
            Instant.parse("2026-11-07T00:00:00Z"));
    paypal.markPaid("pk_pp", Instant.parse("2026-11-07T00:00:00Z"), "해외간편결제(PAYPAL)");
    DiaryPayment card = payment(diary, "DIA-20261107-CARD000001");
    diaryPaymentRepository.saveAllAndFlush(List.of(paypal, card));

    DiaryPayment found = diaryPaymentRepository.findById(paypal.getId()).orElseThrow();
    assertThat(found.getMethod()).isEqualTo(DiaryPaymentMethod.PAYPAL);
    assertThat(found.getMerchant()).isEqualTo(PaymentMerchant.FOREIGN);
    assertThat(found.getCurrency()).isEqualTo(PaymentCurrency.USD);
    assertThat(found.getAmount()).isEqualTo(new BigDecimal("2.99"));
    assertThat(found.getMethodDetail()).isEqualTo("해외간편결제(PAYPAL)");
    // DB 는 numeric(12,2) 라 3000.00 이지만 KRW 는 소수 없이 돌려준다
    assertThat(diaryPaymentRepository.findById(card.getId()).orElseThrow().getAmount())
        .isEqualTo(new BigDecimal("3000"));
    assertThat(
            jdbcTemplate.queryForObject(
                "select amount from diary_payments where id = ?", BigDecimal.class, paypal.getId()))
        .isEqualTo(new BigDecimal("2.99"));
  }

  @Test
  @DisplayName("최신 PAID 결제 조회는 paid_at 이 가장 늦은 결제를 고르고, order_number 는 unique 다")
  void findFirstPaid_whenMultiplePaid_returnsLatest() {
    UUID userId = saveUser();
    Diary diary = diaryRepository.saveAndFlush(Diary.create(userId, false));
    DiaryPayment older = payment(diary, "DIA-20261101-OLDER00001");
    older.markPaid("pk_old", Instant.parse("2026-11-01T00:00:00Z"));
    DiaryPayment latest = payment(diary, "DIA-20261102-LATEST0001");
    latest.markPaid("pk_new", Instant.parse("2026-11-02T00:00:00Z"));
    DiaryPayment pending = payment(diary, "DIA-20261103-PENDING001");
    diaryPaymentRepository.saveAllAndFlush(List.of(latest, older, pending));

    assertThat(
            diaryPaymentRepository.findFirstByDiaryIdAndStatusOrderByPaidAtDescIdDesc(
                diary.getId(), DiaryPaymentStatus.PAID))
        .get()
        .extracting(DiaryPayment::getId)
        .isEqualTo(latest.getId());
    assertThat(
            diaryPaymentRepository.findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(
                userId, List.of(DiaryPaymentStatus.PAID, DiaryPaymentStatus.REFUNDED)))
        .extracting(DiaryPayment::getId)
        .containsExactly(latest.getId(), older.getId());
    assertThatThrownBy(
            () -> diaryPaymentRepository.saveAndFlush(payment(diary, "DIA-20261103-PENDING001")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("활성 템플릿·스티커만 노출 순서대로 조회된다")
  void findActive_whenInactiveExists_returnsOnlyActiveInOrder() {
    diaryTemplateRepository.save(DiaryTemplate.create("B", null, "diary/b.png", 2, true));
    diaryTemplateRepository.save(DiaryTemplate.create("A", "기본", "diary/a.png", 1, true));
    diaryTemplateRepository.save(DiaryTemplate.create("Off", null, "diary/off.png", 0, false));
    diaryStickerRepository.save(DiarySticker.create(null, "diary/s1.png", 0, true));
    diaryStickerRepository.save(DiarySticker.create("off", "diary/s2.png", 1, false));

    assertThat(diaryTemplateRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
        .extracting(DiaryTemplate::getName)
        .containsExactly("A", "B");
    assertThat(diaryTemplateRepository.findAllByOrderByDisplayOrderAscIdAsc()).hasSize(3);
    assertThat(diaryStickerRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
        .extracting(DiarySticker::getImageKey)
        .containsExactly("diary/s1.png");
  }

  @Test
  @DisplayName("템플릿 frames 를 jsonb 로 저장하고 그대로 조회되며, 레이아웃 없이 만들면 기본값이 들어간다")
  void save_whenTemplateHasFrames_framesRoundTrip() {
    List<TemplateFrame> frames =
        List.of(
            new TemplateFrame("photo-1", 0.08, 0.12, 0.84, 0.4, 0),
            new TemplateFrame("photo-2", 0.1, 0.6, 0.35, 0.25, -7.5));
    DiaryTemplate withLayout =
        diaryTemplateRepository.saveAndFlush(
            DiaryTemplate.create(
                "Layout",
                null,
                "diary_template/2026/11/bg.png",
                "diary_template/2026/11/overlay.png",
                0.75,
                frames,
                0,
                true));
    DiaryTemplate plain =
        diaryTemplateRepository.saveAndFlush(
            DiaryTemplate.create("Plain", null, "diary_template/2026/11/p.png", 1, true));

    DiaryTemplate found = diaryTemplateRepository.findById(withLayout.getId()).orElseThrow();
    assertThat(found.getFrames()).isEqualTo(frames);
    assertThat(found.getOverlayImageKey()).isEqualTo("diary_template/2026/11/overlay.png");
    assertThat(found.getAspect()).isEqualTo(0.75);

    DiaryTemplate foundPlain = diaryTemplateRepository.findById(plain.getId()).orElseThrow();
    assertThat(foundPlain.getFrames()).isEmpty();
    assertThat(foundPlain.getOverlayImageKey()).isNull();
    assertThat(foundPlain.getAspect()).isEqualTo(DiaryTemplate.DEFAULT_ASPECT);
  }

  @Test
  @DisplayName("레이아웃 컬럼 없이 insert 된 기존 템플릿은 DB 기본값(1.4142857, 빈 배열)으로 조회된다")
  void findById_whenInsertedWithoutLayout_usesColumnDefaults() {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO diary_templates (id, name, image_key, created_at, updated_at)"
            + " VALUES (?, 'Legacy', 'diary/legacy.png', now(), now())",
        id);

    DiaryTemplate found = diaryTemplateRepository.findById(id).orElseThrow();

    assertThat(found.getAspect()).isEqualTo(DiaryTemplate.DEFAULT_ASPECT);
    assertThat(found.getFrames()).isEmpty();
    assertThat(found.getOverlayImageKey()).isNull();
  }

  private UUID saveUser() {
    return userRepository
        .save(User.createWithEmail(UUID.randomUUID() + "@test.com", "hash", "tester"))
        .getId();
  }

  private static DiaryPayment payment(Diary diary, String orderNumber) {
    return DiaryPayment.createPending(
        diary.getId(),
        diary.getUserId(),
        orderNumber,
        3000L,
        Instant.parse("2026-11-01T00:00:00Z"));
  }

  @Test
  @DisplayName("다운로드 이력은 diary_id 로 최신순 조회되고, 없는 format 은 CHECK 제약으로 거부된다")
  void diaryDownloads_whenSaved_listedNewestFirst() {
    UUID userId = saveUser();
    Diary diary = diaryRepository.saveAndFlush(Diary.create(userId, true));
    DiaryDownload pdf =
        diaryDownloadRepository.saveAndFlush(
            DiaryDownload.create(diary.getId(), userId, DiaryDownloadFormat.PDF, 4));
    DiaryDownload jpg =
        diaryDownloadRepository.saveAndFlush(
            DiaryDownload.create(diary.getId(), userId, DiaryDownloadFormat.JPG, 4));

    assertThat(
            diaryDownloadRepository.findAllByDiaryIdInOrderByCreatedAtDescIdDesc(
                List.of(diary.getId())))
        .extracting(DiaryDownload::getId)
        .containsExactly(jpg.getId(), pdf.getId());
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "insert into diary_downloads (id, diary_id, user_id, format, page_count, created_at, updated_at)"
                        + " values (?, ?, ?, 'PNG', 1, now(), now())",
                    UUID.randomUUID(),
                    diary.getId(),
                    userId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
