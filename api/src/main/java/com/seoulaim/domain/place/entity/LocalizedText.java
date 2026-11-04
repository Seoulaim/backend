package com.seoulaim.domain.place.entity;

// 언어별 문구 (영어·한국어·일본어·중국어). 비어 있는 언어는 클라이언트가 영어로 표시
public record LocalizedText(String en, String ko, String ja, String zh) {}
