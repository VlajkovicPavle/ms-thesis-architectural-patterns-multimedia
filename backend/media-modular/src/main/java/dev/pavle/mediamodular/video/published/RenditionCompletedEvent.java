package dev.pavle.mediamodular.video.published;

import java.time.Instant;
import java.util.UUID;

public record RenditionCompletedEvent(
    UUID videoId, UUID renditionId, String resolution, Instant occurredAt) {
  public RenditionCompletedEvent(UUID videoId, UUID renditionId, String resolution) {
    this(videoId, renditionId, resolution, Instant.now());
  }
}
