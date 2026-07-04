package dev.pavle.mediamodular.video.domain.event;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public record RenditionCompletedEvent(
    UUID videoId, UUID renditionId, VideoResolution resolution, Instant occurredAt) {
  public RenditionCompletedEvent(UUID videoId, UUID renditionId, VideoResolution resolution) {
    this(videoId, renditionId, resolution, Instant.now());
  }
}
