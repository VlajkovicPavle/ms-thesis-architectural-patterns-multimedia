package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;

public interface RenditionJobEventBusPort {
    void publishCreateRenditionJob(CreateRenditionEvent event);
    void consumeCreateRenditionJob();
}
