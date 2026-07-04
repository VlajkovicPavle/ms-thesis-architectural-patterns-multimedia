package dev.pavle.mediamodular.video.domain.port;

import dev.pavle.mediamodular.video.published.AllRenditionsCompletedEvent;
import dev.pavle.mediamodular.video.published.RenditionCompletedEvent;
import dev.pavle.mediamodular.video.published.RenditionFailedEvent;

public interface RenditionEventPublisherPort {
  void publishRenditionCompleted(RenditionCompletedEvent event);

  void publishRenditionFailed(RenditionFailedEvent event);

  void publishAllRenditionsCompleted(AllRenditionsCompletedEvent event);
}
