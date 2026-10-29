package com.seoulaim.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

// contactType 은 String 으로 받아 @Pattern 으로 검증한다 (enum 역직렬화 실패는 500 이 된다)
public record CreateOrderRequest(
    @NotEmpty @Size(max = 50) List<@Valid OrderItemRequest> items,
    @NotBlank @Size(max = 100) String recipientName,
    @NotBlank @Size(max = 500) String addressLine1,
    @Size(max = 500) String addressLine2,
    @Size(max = 20) String postalCode,
    @Pattern(regexp = "PHONE|KAKAO|INSTAGRAM|NONE") String contactType,
    @Size(max = 255) String contactValue) {}
