package com.seoulaim.domain.diary.entity;

public enum DiaryStatus {
  /** 편집 중 */
  DRAFT,
  /** 편집 완료. 더 이상 편집 데이터를 저장할 수 없다 */
  FINALIZED
}
