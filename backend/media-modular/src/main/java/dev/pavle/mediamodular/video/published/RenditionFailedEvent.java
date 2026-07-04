package dev.pavle.mediamodular.video.published;

import java.time.Instant;
import java.util.UUID;

public record RenditionFailedEvent(
    UUID videoId, UUID renditionId, String resolution, String error, Instant occurredAt) {
  public RenditionFailedEvent(UUID videoId, UUID renditionId, String resolution, String error) {
    this(videoId, renditionId, resolution, error, Instant.now());
  }
}
