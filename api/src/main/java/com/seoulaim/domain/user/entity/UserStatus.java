package com.seoulaim.domain.user.entity;

// ACTIVE 만 로그인 가능. SUSPENDED 는 운영자 정지
public enum UserStatus {
  ACTIVE,
  SUSPENDED,
  WITHDRAWN
}
