package com.seoulaim.domain.user.dto;

import java.util.UUID;

/**
 * 프로필 수정. 전부 선택 (null = 변경 안 함)
 *
 * <ul>
 *   <li>nickname: 앞뒤 공백 제거 후 1~20자 (서비스에서 검증 → USER_INVALID_NICKNAME)
 *   <li>profileImageFileId: presigned 업로드(domain PROFILE)로 올린 본인 파일
 *   <li>removeProfileImage: true 면 프로필 이미지 제거. profileImageFileId 와 함께 보낼 수 없음
 * </ul>
 */
public record UpdateProfileRequest(
    String nickname, UUID profileImageFileId, Boolean removeProfileImage) {}
