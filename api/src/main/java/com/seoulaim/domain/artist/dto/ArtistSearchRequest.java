package com.seoulaim.domain.artist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// size 미입력 시 20
public record ArtistSearchRequest(
    @NotBlank @Size(max = 50) String keyword, String cursor, @Min(1) @Max(50) Integer size) {

  private static final int DEFAULT_SIZE = 20;

  public int sizeOrDefault() {
    return size == null ? DEFAULT_SIZE : size;
  }
}
