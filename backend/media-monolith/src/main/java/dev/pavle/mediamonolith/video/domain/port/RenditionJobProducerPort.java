package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;

public interface RenditionJobProducerPort {
  void publishCreateRenditionJob(CreateRenditionEvent event);
}
