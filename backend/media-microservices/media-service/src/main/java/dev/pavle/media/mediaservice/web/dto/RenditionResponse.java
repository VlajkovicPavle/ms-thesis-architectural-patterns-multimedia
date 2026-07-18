package dev.pavle.media.mediaservice.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.VideoResolution;
import dev.pavle.media.mediaservice.model.Rendition;

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
