package com.seoulaim.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailMaskerTest {

  @Test
  @DisplayName("로컬파트 1자: 그 1자 + ** (최소 2개)")
  void mask_로컬1자() {
    assertThat(EmailMasker.mask("a@gmail.com")).isEqualTo("a**@gmail.com");
  }

  @Test
  @DisplayName("로컬파트 2자: 첫 1자만 보이고 ** (전부 드러나지 않게)")
  void mask_로컬2자() {
    assertThat(EmailMasker.mask("ab@gmail.com")).isEqualTo("a**@gmail.com");
  }

  @Test
  @DisplayName("로컬파트 3자 이상: 앞 2자 + 나머지 길이만큼 * (최소 2개)")
  void mask_로컬3자이상() {
    assertThat(EmailMasker.mask("abc@gmail.com")).isEqualTo("ab**@gmail.com");
    assertThat(EmailMasker.mask("abcde@gmail.com")).isEqualTo("ab***@gmail.com");
    assertThat(EmailMasker.mask("mina.choi@example.com")).isEqualTo("mi*******@example.com");
  }

  @Test
  @DisplayName("null(탈퇴 회원)은 null, @ 가 없으면 전체를 로컬파트로 본다")
  void mask_null과골뱅이없음() {
    assertThat(EmailMasker.mask(null)).isNull();
    assertThat(EmailMasker.mask("abcd")).isEqualTo("ab**");
  }
}
