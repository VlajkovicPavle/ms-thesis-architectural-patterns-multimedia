package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;

public interface EventDispatcherPort {
  void dispatchRenditionCreation(CreateRenditionEvent event);
}
