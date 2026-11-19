package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.user.entity.SocialProvider;

/**
 * 검증된 소셜 ID 토큰의 사용자 정보
 *
 * <ul>
 *   <li>providerUserId: 제공자의 사용자 고유 id (구글 sub)
 *   <li>email: 제공자가 인증한 이메일만, 아니면 null
 *   <li>name: 표시 이름. 없으면 null
 * </ul>
 */
public record SocialProfile(
    SocialProvider provider, String providerUserId, String email, String name) {}
