package dev.pavle.mediamonolith.video.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public record CreateRenditionEvent(UUID videoId, VideoResolution resolution, Instant createdAt) {
  public CreateRenditionEvent(UUID videoId, VideoResolution resolution) {
    this(videoId, resolution, Instant.now());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CreateRenditionEvent other)) {
      return false;
    }
    return videoId.equals(other.videoId) && resolution.equals(other.resolution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(videoId, resolution);
  }
}
