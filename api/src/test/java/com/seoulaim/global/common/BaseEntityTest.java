package com.seoulaim.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

  @Test
  @DisplayName("엔티티 생성 시 서로 다른 UUID v7 id 가 할당된다")
  void getId_whenCreated_returnsDistinctUuidV7() {
    BaseEntity first = new BaseEntity() {};
    BaseEntity second = new BaseEntity() {};

    assertThat(first.getId()).isNotNull();
    assertThat(second.getId()).isNotNull();
    assertThat(first.getId()).isNotEqualTo(second.getId());
    assertThat(first.getId().version()).isEqualTo(7);
    assertThat(second.getId().version()).isEqualTo(7);
  }
}
