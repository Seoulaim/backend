package com.seoulaim.domain.auth.dto;

import com.seoulaim.global.config.JwtProperties;
import com.seoulaim.global.security.IssuedToken;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long accessTokenExpiresIn,
    long refreshTokenExpiresIn) {

  private static final String BEARER = "Bearer";

  public static TokenResponse of(
      IssuedToken accessToken, IssuedToken refreshToken, JwtProperties properties) {
    return new TokenResponse(
        accessToken.token(),
        refreshToken.token(),
        BEARER,
        properties.accessTokenExpiration().toSeconds(),
        properties.refreshTokenExpiration().toSeconds());
  }
}
