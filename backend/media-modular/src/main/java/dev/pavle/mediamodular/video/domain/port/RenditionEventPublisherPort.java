package dev.pavle.mediamodular.video.domain.port;

import dev.pavle.mediamodular.video.domain.event.AllRenditionsCompletedEvent;
import dev.pavle.mediamodular.video.domain.event.RenditionCompletedEvent;
import dev.pavle.mediamodular.video.domain.event.RenditionFailedEvent;

public interface RenditionEventPublisherPort {
  void publishRenditionCompleted(RenditionCompletedEvent event);

  void publishRenditionFailed(RenditionFailedEvent event);

  void publishAllRenditionsCompleted(AllRenditionsCompletedEvent event);
}
