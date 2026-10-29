package com.seoulaim.global.security;

import java.util.UUID;

public record RefreshTokenClaims(UUID userId, String tokenId) {}
