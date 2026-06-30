package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.event.AllRenditionsCompletedEvent;
import dev.pavle.mediamonolith.video.domain.event.RenditionCompletedEvent;
import dev.pavle.mediamonolith.video.domain.event.RenditionFailedEvent;

public interface RenditionEventPublisherPort {
  void publishRenditionCompleted(RenditionCompletedEvent event);

  void publishRenditionFailed(RenditionFailedEvent event);

  void publishAllRenditionsCompleted(AllRenditionsCompletedEvent event);
}
