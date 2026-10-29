package com.seoulaim.domain.goods.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** 상품 생성·수정 공용. imageFileIds 순서가 product_images.display_order 가 된다. */
public record AdminProductRequest(
    @NotNull UUID categoryId,
    @NotNull UUID artistGroupId,
    UUID artistId,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 500) String summary,
    @NotNull @PositiveOrZero Long price,
    @NotNull Boolean isDiary,
    @NotNull List<@NotNull UUID> imageFileIds,
    UUID detailImageFileId) {}
