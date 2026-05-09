package dev.pavle.mediamonolith.video.domain.event;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public record CreateRenditionEvent(UUID videoId, VideoResolution resolution, Instant createdAt) {
  public CreateRenditionEvent(UUID videoId, VideoResolution resolution) {
    this(videoId, resolution, Instant.now());
  }
}
