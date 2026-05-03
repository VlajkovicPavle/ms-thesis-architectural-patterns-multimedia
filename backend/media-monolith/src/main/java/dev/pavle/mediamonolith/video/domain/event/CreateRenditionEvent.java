package dev.pavle.mediamonolith.video.domain.event;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CreateRenditionEvent {
  private final UUID videoId;
  private final VideoResolution resolution;
  private final Instant createdAt = Instant.now();

  public CreateRenditionEvent(UUID videoId, VideoResolution resolution) {
    this.videoId = videoId;
    this.resolution = resolution;
  }
}
