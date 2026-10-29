package com.seoulaim.domain.goods.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminProductCategoryRequest(
    @NotBlank @Size(max = 50) String name, @NotNull Integer displayOrder) {}
