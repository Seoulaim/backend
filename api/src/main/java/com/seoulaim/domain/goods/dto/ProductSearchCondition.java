package com.seoulaim.domain.goods.dto;

import java.util.UUID;

public record ProductSearchCondition(
    UUID categoryId,
    UUID artistGroupId,
    UUID artistId,
    ProductSort sort,
    ProductCursor cursor,
    int size) {}
