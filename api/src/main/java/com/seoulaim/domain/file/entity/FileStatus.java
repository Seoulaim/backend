package com.seoulaim.domain.file.entity;

public enum FileStatus {
  // presigned URL 만 발급된 상태. 48시간 내 연결되지 않으면 정리 배치가 삭제한다
  PENDING,
  // 다른 도메인 엔티티에 연결된 상태
  LINKED
}
