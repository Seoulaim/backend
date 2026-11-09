package com.seoulaim.domain.diary.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.dto.DiaryEditorStickersResponse;
import com.seoulaim.domain.diary.dto.DiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.dto.DiaryStickerResponse;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.global.security.SecurityTestConfig;
import com.seoulaim.global.security.TestAccessTokens;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiaryAssetController.class)
@Import(SecurityTestConfig.class)
class DiaryAssetControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private DiaryAssetService diaryAssetService;

  @Test
  @DisplayName("스티커 조회는 배열이 아니라 { categories, stickers } 객체를 반환한다")
  void getStickers_정상_카테고리와스티커객체반환() throws Exception {
    UUID categoryId = UUID.randomUUID();
    UUID stickerId = UUID.randomUUID();
    given(diaryAssetService.getEditorStickers())
        .willReturn(
            new DiaryEditorStickersResponse(
                List.of(new DiaryStickerCategoryResponse(categoryId, "Cute", 0)),
                List.of(
                    new DiaryStickerResponse(
                        stickerId, "heart", "diary_sticker/heart.png", 0, List.of(categoryId)))));

    mockMvc
        .perform(
            get("/api/v1/diary-stickers")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[0].id").value(categoryId.toString()))
        .andExpect(jsonPath("$.categories[0].displayOrder").value(0))
        .andExpect(jsonPath("$.stickers[0].imageKey").value("diary_sticker/heart.png"))
        .andExpect(jsonPath("$.stickers[0].categoryIds[0]").value(categoryId.toString()));
  }

  @Test
  @DisplayName("access token 없이 스티커를 조회하면 401 을 반환한다")
  void getStickers_토큰없음_401반환() throws Exception {
    mockMvc.perform(get("/api/v1/diary-stickers")).andExpect(status().isUnauthorized());
  }
}
