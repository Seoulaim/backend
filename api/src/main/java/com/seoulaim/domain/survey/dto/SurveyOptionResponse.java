package com.seoulaim.domain.survey.dto;

import com.seoulaim.domain.survey.entity.SurveyOption;
import java.util.Map;
import java.util.UUID;

public record SurveyOptionResponse(UUID id, Map<String, String> label) {

  public static SurveyOptionResponse from(SurveyOption option) {
    return new SurveyOptionResponse(option.getId(), option.getLabel());
  }
}
