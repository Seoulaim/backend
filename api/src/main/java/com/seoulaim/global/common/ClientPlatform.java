package com.seoulaim.global.common;

import java.util.Locale;

/**
 * 요청을 보낸 클라이언트 ({@value #HEADER} 헤더). 컨트롤러 파라미터로 선언하면 {@link ClientPlatformArgumentResolver} 가 넣어
 * 준다.
 *
 * <p>앱만 헤더를 보낸다(android | ios). 헤더가 없거나 모르는 값이면 WEB. 클라이언트가 스스로 밝히는 값이라 인증·권한 판단에는 쓰지 않는다.
 */
public enum ClientPlatform {
  WEB,
  ANDROID,
  IOS;

  public static final String HEADER = "X-Client-Platform";

  /** 앞뒤 공백·대소문자는 무시한다. null·빈 값·모르는 값은 WEB. */
  public static ClientPlatform from(String headerValue) {
    if (headerValue == null) {
      return WEB;
    }
    return switch (headerValue.strip().toLowerCase(Locale.ROOT)) {
      case "android" -> ANDROID;
      case "ios" -> IOS;
      default -> WEB;
    };
  }

  public boolean isApp() {
    return this != WEB;
  }
}
