package com.seoulaim.domain.diary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.dto.DiaryEditorResponse;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesRequest;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesResponse;
import com.seoulaim.domain.diary.dto.DiaryNewTermsResponse;
import com.seoulaim.domain.diary.dto.DiarySpreadsRequest;
import com.seoulaim.domain.diary.dto.DiarySummaryResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 다이어리 생성·편집·완성. 결제는 {@link DiaryPaymentService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

  static final int MAX_EDIT_DATA_BYTES = 2 * 1024 * 1024;
  static final int MAX_NAME_LENGTH = 30;
  private static final String TEMPLATE_ID_FIELD = "templateId";
  private static final TypeReference<List<Object>> SPREADS_TYPE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> PLACE_PHOTOS_TYPE =
      new TypeReference<>() {};

  private final DiaryRepository diaryRepository;
  private final DiaryAssetService diaryAssetService;
  private final FileService fileService;
  private final DiaryProperties diaryProperties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** 삭제되지 않았고 무료이거나 결제된 다이어리만 최신순. 미결제 유료 다이어리는 결제 전이라 숨긴다. */
  public List<DiarySummaryResponse> getMyDiaries(UUID userId) {
    return diaryRepository.findVisibleByUserId(userId).stream()
        .map(DiarySummaryResponse::from)
        .toList();
  }

  public DiaryNewTermsResponse getNewTerms(UUID userId) {
    return new DiaryNewTermsResponse(isNextFree(userId), diaryProperties.price());
  }

  /** 첫 다이어리(삭제한 것 포함)만 무료. 이후는 결제 전까지 저장할 수 없는 유료 다이어리로 만든다. */
  @Transactional
  public DiaryCreateResponse create(UUID userId) {
    Diary diary = Diary.create(userId, isNextFree(userId));
    try {
      // 무료 다이어리 동시 생성은 (user_id) WHERE is_free partial unique 인덱스로 막는다
      diaryRepository.saveAndFlush(diary);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("diary", "동시에 생성 요청이 들어왔습니다. 다시 시도해주세요"));
    }
    return new DiaryCreateResponse(diary.getId());
  }

  @Transactional
  public void rename(UUID userId, UUID diaryId, String name) {
    String trimmed = name == null ? "" : name.strip();
    if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_NAME_LENGTH) {
      throw new BusinessException(ErrorCode.DIARY_INVALID_NAME);
    }
    findMyDiary(userId, diaryId).rename(trimmed);
  }

  @Transactional
  public void delete(UUID userId, UUID diaryId) {
    findMyDiary(userId, diaryId).delete(clock.instant());
  }

  // 회원 탈퇴용: 삭제되지 않은 다이어리 전부 soft delete. 결제(diary_payments)는 그대로 둔다
  @Transactional
  public void deleteAllDiariesOfUser(UUID userId) {
    Instant now = clock.instant();
    diaryRepository.findAllByUserIdAndDeletedAtIsNull(userId).forEach(diary -> diary.delete(now));
  }

  public DiaryEditorResponse getEditor(UUID userId, UUID diaryId) {
    Diary diary = findMyDiary(userId, diaryId);
    Set<UUID> templateIds = new LinkedHashSet<>();
    collectTemplateIds(objectMapper.valueToTree(diary.getSpreads()), templateIds);
    return DiaryEditorResponse.of(diary, diaryAssetService.getInactiveTemplates(templateIds));
  }

  @Transactional
  public void saveSpreads(UUID userId, UUID diaryId, DiarySpreadsRequest request) {
    Diary diary = findMyDiary(userId, diaryId);
    EditData data = prepareEditData(userId, diary, request);
    diary.saveSpreads(data.spreads(), data.placePhotos(), clock.instant());
  }

  @Transactional
  public void finalizeDiary(UUID userId, UUID diaryId, DiarySpreadsRequest request) {
    Diary diary = findMyDiary(userId, diaryId);
    EditData data = prepareEditData(userId, diary, request);
    diary.finalizeWith(data.spreads(), data.placePhotos(), clock.instant());
  }

  /**
   * 완성 페이지 이미지를 연결한다. 한 번 저장되면 이후 요청은 기존 key 를 그대로 돌려준다(멱등).
   *
   * <p>완성 후에는 편집 데이터가 필요 없으므로 spreads / placePhotos 를 비운다.
   */
  @Transactional
  public DiaryFinishedPagesResponse saveFinishedPages(
      UUID userId, UUID diaryId, DiaryFinishedPagesRequest request) {
    Diary diary = findMyDiary(userId, diaryId);
    if (!diary.isFinalized()) {
      throw new BusinessException(ErrorCode.DIARY_NOT_FINISHED);
    }
    if (diary.hasPageKeys()) {
      return new DiaryFinishedPagesResponse(diary.getPageKeys());
    }
    Map<UUID, FileDetail> files = loadOwnedFiles(userId, request.fileIds(), "fileIds");
    List<String> pageKeys = request.fileIds().stream().map(id -> files.get(id).s3Key()).toList();
    fileService.markLinked(request.fileIds());
    // TODO(#42): 편집에 쓰인 원본 사진(placePhotos) S3 객체 정리. 지금은 DB 에서만 비운다
    diary.completePages(pageKeys);
    return new DiaryFinishedPagesResponse(diary.getPageKeys());
  }

  public List<AdminDiarySummaryResponse> getAdminDiaries() {
    return diaryRepository.findTop200ByDeletedAtIsNullOrderByCreatedAtDescIdDesc().stream()
        .map(AdminDiarySummaryResponse::from)
        .toList();
  }

  /** 삭제되지 않은 내 다이어리. 없거나 남의 것이면 DIARY_NOT_FOUND. */
  Diary findMyDiary(UUID userId, UUID diaryId) {
    return diaryRepository
        .findByIdAndUserIdAndDeletedAtIsNull(diaryId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
  }

  private boolean isNextFree(UUID userId) {
    return !diaryRepository.existsByUserIdAndIsFreeTrue(userId);
  }

  // 저장 허용 조건 → 형태·크기 검증 → 사진 파일 LINKED 순서
  private EditData prepareEditData(UUID userId, Diary diary, DiarySpreadsRequest request) {
    if (diary.isFinalized()) {
      throw new BusinessException(ErrorCode.DIARY_FINALIZED);
    }
    JsonNode spreads = request.spreads();
    JsonNode placePhotos = request.placePhotos();
    if (!spreads.isArray()) {
      throw invalid("spreads", "배열이어야 합니다");
    }
    if (!diary.isPaid() && !(diary.isFree() && spreads.size() <= diaryProperties.freeSpreads())) {
      throw new BusinessException(ErrorCode.DIARY_PAYMENT_REQUIRED);
    }
    Map<UUID, String> photoKeys = parsePlacePhotos(placePhotos);
    validateSize(spreads, placePhotos);

    Map<UUID, FileDetail> files =
        loadOwnedFiles(userId, List.copyOf(photoKeys.keySet()), "placePhotos");
    photoKeys.forEach(
        (fileId, key) -> {
          if (!files.get(fileId).s3Key().equals(key)) {
            throw invalid("placePhotos", "파일 key 가 일치하지 않습니다: " + fileId);
          }
        });
    fileService.markLinked(List.copyOf(photoKeys.keySet()));

    return new EditData(
        objectMapper.convertValue(spreads, SPREADS_TYPE),
        objectMapper.convertValue(placePhotos, PLACE_PHOTOS_TYPE));
  }

  // { [placeId | "other"]: [{ id, key }] } 형태만 받는다. 반환: fileId → key
  private Map<UUID, String> parsePlacePhotos(JsonNode placePhotos) {
    if (!placePhotos.isObject()) {
      throw invalid("placePhotos", "객체여야 합니다");
    }
    Map<UUID, String> photoKeys = new LinkedHashMap<>();
    for (JsonNode photos : (Iterable<JsonNode>) placePhotos::elements) {
      if (!photos.isArray()) {
        throw invalid("placePhotos", "각 장소의 값은 배열이어야 합니다");
      }
      for (JsonNode photo : photos) {
        UUID fileId = parseUuid(photo.path("id"));
        JsonNode key = photo.path("key");
        if (fileId == null || !key.isTextual() || key.asText().isBlank()) {
          throw invalid("placePhotos", "사진은 { id: fileId, key: s3Key } 형태여야 합니다");
        }
        photoKeys.put(fileId, key.asText());
      }
    }
    return photoKeys;
  }

  private void validateSize(JsonNode spreads, JsonNode placePhotos) {
    try {
      long bytes =
          (long) objectMapper.writeValueAsBytes(spreads).length
              + objectMapper.writeValueAsBytes(placePhotos).length;
      if (bytes > MAX_EDIT_DATA_BYTES) {
        throw new BusinessException(ErrorCode.DIARY_CONTENT_TOO_LARGE);
      }
    } catch (JsonProcessingException exception) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
    }
  }

  // 없는 파일이나 남이 올린 파일을 끌어다 쓰지 못하게 한다
  private Map<UUID, FileDetail> loadOwnedFiles(UUID userId, List<UUID> fileIds, String field) {
    Map<UUID, FileDetail> files =
        fileService.findDetails(fileIds).stream()
            .collect(Collectors.toMap(FileDetail::fileId, Function.identity()));
    List<UUID> invalidIds = new ArrayList<>();
    for (UUID fileId : fileIds) {
      FileDetail file = files.get(fileId);
      if (file == null || !userId.equals(file.uploaderId())) {
        invalidIds.add(fileId);
      }
    }
    if (!invalidIds.isEmpty()) {
      throw invalid(field, "존재하지 않는 파일입니다: " + invalidIds);
    }
    return files;
  }

  // spreads 구조는 웹앱이 정하므로 깊이와 무관하게 templateId 필드를 모두 모은다
  private static void collectTemplateIds(JsonNode node, Set<UUID> templateIds) {
    if (node.isObject()) {
      node.properties()
          .forEach(
              entry -> {
                if (TEMPLATE_ID_FIELD.equals(entry.getKey())) {
                  UUID templateId = parseUuid(entry.getValue());
                  if (templateId != null) {
                    templateIds.add(templateId);
                  }
                } else {
                  collectTemplateIds(entry.getValue(), templateIds);
                }
              });
    } else if (node.isArray()) {
      node.forEach(child -> collectTemplateIds(child, templateIds));
    }
  }

  private static UUID parseUuid(JsonNode node) {
    if (!node.isTextual()) {
      return null;
    }
    try {
      return UUID.fromString(node.asText());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static BusinessException invalid(String field, String message) {
    return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, Map.of(field, message));
  }

  private record EditData(List<Object> spreads, Map<String, Object> placePhotos) {}
}
