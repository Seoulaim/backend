package com.seoulaim.domain.diary.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDiaryController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminDiaryControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DiaryAssetService diaryAssetService;
  @MockitoBean private DiaryService diaryService;

  @Test
  @DisplayName("imageFileId 없이 템플릿을 수정해도 검증을 통과하고 200 과 레이아웃 필드를 반환한다")
  void updateTemplate_이미지생략_200반환() throws Exception {
    UUID templateId = UUID.randomUUID();
    given(diaryAssetService.updateTemplate(eq(templateId), any()))
        .willReturn(
            new AdminDiaryTemplateResponse(
                templateId,
                "Polaroid",
                null,
                "diary_template/2026/11/bg.png",
                1,
                true,
                null,
                1.4142857,
                List.of(new TemplateFrame("photo-1", 0.1, 0.1, 0.8, 0.4, 0))));

    mockMvc
        .perform(
            put("/api/v1/admin/diary-templates/{id}", templateId)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Polaroid", "displayOrder": 1,
                     "frames": [{"id": "photo-1", "x": 0.1, "y": 0.1, "width": 0.8, "height": 0.4, "rotation": 0}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageKey").value("diary_template/2026/11/bg.png"))
        .andExpect(jsonPath("$.aspect").value(1.4142857))
        .andExpect(jsonPath("$.frames[0].id").value("photo-1"))
        .andExpect(jsonPath("$.overlayImageKey").doesNotExist());
  }

  @Test
  @DisplayName("frames 원소의 id 가 비어 있으면 400 COMMON_INVALID_REQUEST 를 반환한다")
  void updateTemplate_프레임id없음_400반환() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/admin/diary-templates/{id}", UUID.randomUUID())
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Polaroid", "displayOrder": 1, "frames": [{"x": 0.1}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
  }

  @Test
  @DisplayName("없는 스티커를 수정하면 404 DIARY_STICKER_NOT_FOUND 를 반환한다")
  void updateSticker_없는스티커_404반환() throws Exception {
    UUID stickerId = UUID.randomUUID();
    given(diaryAssetService.updateSticker(eq(stickerId), any()))
        .willThrow(new BusinessException(ErrorCode.DIARY_STICKER_NOT_FOUND));

    mockMvc
        .perform(
            put("/api/v1/admin/diary-stickers/{id}", stickerId)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayOrder": 0}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DIARY_STICKER_NOT_FOUND"));
  }

  @Test
  @DisplayName("관리자 다이어리 목록은 새 모델 필드(id, userId, name, free, paid, status ...)를 반환한다")
  void getDiaries_정상_새모델필드반환() throws Exception {
    UUID userId = UUID.randomUUID();
    Diary diary = Diary.create(userId, true);
    given(diaryService.getAdminDiaries())
        .willReturn(List.of(AdminDiarySummaryResponse.from(diary)));

    mockMvc
        .perform(get("/api/v1/admin/diaries").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(diary.getId().toString()))
        .andExpect(jsonPath("$[0].userId").value(userId.toString()))
        .andExpect(jsonPath("$[0].name").value("My Diary"))
        .andExpect(jsonPath("$[0].free").value(true))
        .andExpect(jsonPath("$[0].paid").value(false))
        .andExpect(jsonPath("$[0].status").value("DRAFT"));
  }
}
