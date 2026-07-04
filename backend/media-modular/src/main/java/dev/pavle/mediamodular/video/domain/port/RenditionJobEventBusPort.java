package dev.pavle.mediamodular.video.domain.port;

import dev.pavle.mediamodular.video.domain.event.CreateRenditionEvent;

public interface RenditionJobEventBusPort {
  void publishCreateRenditionJob(CreateRenditionEvent event);
}
