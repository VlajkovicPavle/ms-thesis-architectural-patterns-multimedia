package dev.pavle.mediamodular.video.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AllRenditionsCompletedEvent(UUID videoId, Instant occurredAt) {
  public AllRenditionsCompletedEvent(UUID videoId) {
    this(videoId, Instant.now());
  }
}
