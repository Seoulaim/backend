package com.seoulaim.domain.goods.admin.dto;

import java.util.UUID;

/** 상품 이미지의 s3 key 와 원본 fileId. files 행이 없으면(시드 데이터 등) fileId 는 null. */
public record AdminProductImageFileResponse(UUID fileId, String s3Key) {}
