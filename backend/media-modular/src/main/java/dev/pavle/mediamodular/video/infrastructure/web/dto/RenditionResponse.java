package dev.pavle.mediamodular.video.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.rendition.Rendition;
import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public record RenditionResponse(
    UUID id,
    String name,
    VideoResolution resolution,
    RenditionStatus status,
    String error,
    Instant createdAt,
    Instant updatedAt) {
  public static RenditionResponse from(Rendition rendition) {
    return new RenditionResponse(
        rendition.getId(),
        rendition.getName(),
        rendition.getResolution(),
        rendition.getStatus(),
        rendition.getError(),
        rendition.getCreatedAt(),
        rendition.getUpdatedAt());
  }
}
