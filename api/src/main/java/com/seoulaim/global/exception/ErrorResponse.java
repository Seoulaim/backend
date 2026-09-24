package com.seoulaim.global.exception;

import java.util.Map;

public record ErrorResponse(
    String code, String message, Map<String, String> fields, String traceId) {}
