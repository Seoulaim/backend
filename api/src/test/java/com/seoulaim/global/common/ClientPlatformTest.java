package com.seoulaim.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientPlatformTest {

  @ParameterizedTest
  @CsvSource({"android,ANDROID", "ios,IOS", "Android,ANDROID", "iOS,IOS", "' ios ',IOS"})
  @DisplayName("android·ios 는 앱 플랫폼이다 (앞뒤 공백·대소문자 무시)")
  void from_앱값_앱플랫폼(String headerValue, ClientPlatform expected) {
    assertThat(ClientPlatform.from(headerValue)).isEqualTo(expected);
    assertThat(ClientPlatform.from(headerValue).isApp()).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "web", "WEB", "app", "androidx", "android,ios", "iphone"})
  @DisplayName("헤더가 없거나 허용 값이 아니면 web 으로 취급한다")
  void from_그외_웹(String headerValue) {
    assertThat(ClientPlatform.from(headerValue)).isEqualTo(ClientPlatform.WEB);
    assertThat(ClientPlatform.from(headerValue).isApp()).isFalse();
  }
}
