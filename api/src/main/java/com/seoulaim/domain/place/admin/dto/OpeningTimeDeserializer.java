package com.seoulaim.domain.place.admin.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

// 영업시간 "HH:mm". LocalTime 에 없는 "24:00" 은 00:00 (다음 날 자정 마감) 으로 받는다
public class OpeningTimeDeserializer extends JsonDeserializer<LocalTime> {

  private static final String END_OF_DAY = "24:00";
  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  @Override
  public LocalTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String text = parser.getValueAsString();
    if (text == null || text.isBlank()) {
      return null;
    }
    if (END_OF_DAY.equals(text.trim())) {
      return LocalTime.MIDNIGHT;
    }
    try {
      return LocalTime.parse(text.trim(), FORMAT);
    } catch (DateTimeParseException exception) {
      return (LocalTime) context.handleWeirdStringValue(LocalTime.class, text, "HH:mm 형식이 아닙니다");
    }
  }
}
