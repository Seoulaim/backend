package com.seoulaim.domain.diary.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * 편집 데이터 저장·완성 공용. 서버는 내용을 해석하지 않는다.
 *
 * <ul>
 *   <li>spreads: JSON 배열
 *   <li>placePhotos: {@code { [placeId | "other"]: [{ id: fileId, key: s3Key }] }}
 * </ul>
 *
 * <p>형태 검증은 COMMON_INVALID_REQUEST(400) 로 내리려고 JsonNode 로 받아 서비스에서 한다.
 */
public record DiarySpreadsRequest(@NotNull JsonNode spreads, @NotNull JsonNode placePhotos) {}
