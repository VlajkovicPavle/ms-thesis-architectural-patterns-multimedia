package dev.pavle.mediamonolith.video.domain.event;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public record RenditionFailedEvent(
    UUID videoId, UUID renditionId, VideoResolution resolution, String error, Instant occurredAt) {
  public RenditionFailedEvent(
      UUID videoId, UUID renditionId, VideoResolution resolution, String error) {
    this(videoId, renditionId, resolution, error, Instant.now());
  }
}
