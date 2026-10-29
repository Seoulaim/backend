package com.seoulaim.global.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class HeaderCurrentUserIdArgumentResolverTest {

  private final HeaderCurrentUserIdArgumentResolver resolver =
      new HeaderCurrentUserIdArgumentResolver();

  @Test
  @DisplayName("X-User-Id 헤더가 UUID 면 그 값을 반환한다")
  void resolveArgument_올바른UUID_반환() {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderCurrentUserIdArgumentResolver.USER_ID_HEADER, userId.toString());

    UUID resolved = resolver.resolveArgument(null, null, new ServletWebRequest(request), null);

    assertThat(resolved).isEqualTo(userId);
  }

  @Test
  @DisplayName("X-User-Id 헤더가 UUID 형식이 아니면 AUTH_INVALID_TOKEN 예외가 발생한다")
  void resolveArgument_잘못된형식_예외발생() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderCurrentUserIdArgumentResolver.USER_ID_HEADER, "not-a-uuid");

    assertThatThrownBy(
            () -> resolver.resolveArgument(null, null, new ServletWebRequest(request), null))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
  }
}
