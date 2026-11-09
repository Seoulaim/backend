package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryListResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import com.seoulaim.domain.diary.repository.CategoryStickerCount;
import com.seoulaim.domain.diary.repository.DiaryStickerCategoryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryStickerCategoryServiceTest {

  @Mock private DiaryStickerCategoryRepository categoryRepository;

  private DiaryStickerCategoryService service;

  @BeforeEach
  void setUp() {
    service = new DiaryStickerCategoryService(categoryRepository);
  }

  private static CategoryStickerCount count(UUID categoryId, long count) {
    return new CategoryStickerCount() {
      @Override
      public UUID getCategoryId() {
        return categoryId;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }

  @Test
  @DisplayName("카테고리를 만들면 이름 앞뒤 공백을 지우고 현재 최대 순서 다음(맨 뒤)에 둔다")
  void createCategory_기존카테고리있음_맨뒤에생성() {
    given(categoryRepository.findTopByOrderByDisplayOrderDesc())
        .willReturn(Optional.of(DiaryStickerCategory.create("Cute", 4)));
    given(categoryRepository.save(any(DiaryStickerCategory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    AdminDiaryStickerCategoryResponse response = service.createCategory("  Food  ");

    assertThat(response.name()).isEqualTo("Food");
    assertThat(response.displayOrder()).isEqualTo(5);
  }

  @Test
  @DisplayName("첫 카테고리는 순서 0 으로 만든다")
  void createCategory_카테고리없음_순서0() {
    given(categoryRepository.findTopByOrderByDisplayOrderDesc()).willReturn(Optional.empty());
    given(categoryRepository.save(any(DiaryStickerCategory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.createCategory("Cute").displayOrder()).isZero();
  }

  @Test
  @DisplayName("이름이 공백이거나 50자를 넘으면 DIARY_STICKER_CATEGORY_INVALID 이고 저장하지 않는다")
  void createCategory_이름잘못됨_예외() {
    for (String name : new String[] {null, "", "   ", "a".repeat(51)}) {
      assertThatThrownBy(() -> service.createCategory(name))
          .isInstanceOfSatisfying(
              BusinessException.class,
              e ->
                  assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DIARY_STICKER_CATEGORY_INVALID));
    }
    verify(categoryRepository, never()).save(any());
  }

  @Test
  @DisplayName("50자 이름은 허용한다")
  void createCategory_이름50자_생성() {
    given(categoryRepository.findTopByOrderByDisplayOrderDesc()).willReturn(Optional.empty());
    given(categoryRepository.save(any(DiaryStickerCategory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.createCategory("a".repeat(50)).name()).hasSize(50);
  }

  @Test
  @DisplayName("없는 카테고리 이름을 바꾸면 DIARY_STICKER_CATEGORY_NOT_FOUND")
  void updateCategory_없는카테고리_예외() {
    UUID id = UUID.randomUUID();
    given(categoryRepository.findById(id)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateCategory(id, "Food"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND));
  }

  @Test
  @DisplayName("목록은 정렬순으로 활성 스티커 수를 붙이고, 스티커가 없는 카테고리는 0")
  void getCategories_정상_스티커수포함() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    DiaryStickerCategory food = DiaryStickerCategory.create("Food", 1);
    given(categoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
        .willReturn(List.of(cute, food));
    given(categoryRepository.countActiveStickersByCategory())
        .willReturn(List.of(count(cute.getId(), 3)));

    List<AdminDiaryStickerCategoryListResponse> result = service.getCategories();

    assertThat(result)
        .extracting(AdminDiaryStickerCategoryListResponse::name)
        .containsExactly("Cute", "Food");
    assertThat(result)
        .extracting(AdminDiaryStickerCategoryListResponse::stickerCount)
        .containsExactly(3L, 0L);
  }

  @Test
  @DisplayName("카테고리를 지우면 스티커 매핑을 먼저 지운 뒤 카테고리를 지운다")
  void deleteCategory_정상_매핑제거후삭제() {
    DiaryStickerCategory category = DiaryStickerCategory.create("Cute", 0);
    UUID id = category.getId();
    given(categoryRepository.findById(id)).willReturn(Optional.of(category));

    service.deleteCategory(id);

    InOrder order = inOrder(categoryRepository);
    order.verify(categoryRepository).deleteMappingsByCategoryId(id);
    order.verify(categoryRepository).deleteById(id);
  }

  @Test
  @DisplayName("없는 카테고리를 지우면 DIARY_STICKER_CATEGORY_NOT_FOUND 이고 매핑도 건드리지 않는다")
  void deleteCategory_없는카테고리_예외() {
    UUID id = UUID.randomUUID();
    given(categoryRepository.findById(id)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteCategory(id)).isInstanceOf(BusinessException.class);
    verify(categoryRepository, never()).deleteMappingsByCategoryId(any());
  }

  @Test
  @DisplayName("정렬은 요청 순서대로 0부터 주고, 빠진 카테고리는 기존 순서대로 뒤에 둔다")
  void reorderCategories_일부만전달_빠진것은뒤로() {
    DiaryStickerCategory a = DiaryStickerCategory.create("A", 0);
    DiaryStickerCategory b = DiaryStickerCategory.create("B", 1);
    DiaryStickerCategory c = DiaryStickerCategory.create("C", 2);
    DiaryStickerCategory d = DiaryStickerCategory.create("D", 3);
    given(categoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
        .willReturn(List.of(a, b, c, d));

    service.reorderCategories(List.of(c.getId(), a.getId()));

    assertThat(c.getDisplayOrder()).isZero();
    assertThat(a.getDisplayOrder()).isEqualTo(1);
    assertThat(b.getDisplayOrder()).isEqualTo(2);
    assertThat(d.getDisplayOrder()).isEqualTo(3);
  }

  @Test
  @DisplayName("정렬 요청에 없는 id 가 있으면 DIARY_STICKER_CATEGORY_NOT_FOUND 이고 순서는 그대로")
  void reorderCategories_없는id_예외() {
    DiaryStickerCategory a = DiaryStickerCategory.create("A", 0);
    DiaryStickerCategory b = DiaryStickerCategory.create("B", 1);
    given(categoryRepository.findAllByOrderByDisplayOrderAscIdAsc()).willReturn(List.of(a, b));

    assertThatThrownBy(() -> service.reorderCategories(List.of(b.getId(), UUID.randomUUID())))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND));
    assertThat(a.getDisplayOrder()).isZero();
    assertThat(b.getDisplayOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("정렬 요청에 중복 id 가 있으면 DIARY_STICKER_CATEGORY_INVALID")
  void reorderCategories_중복id_예외() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.reorderCategories(List.of(id, id)))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DIARY_STICKER_CATEGORY_INVALID));
  }
}
