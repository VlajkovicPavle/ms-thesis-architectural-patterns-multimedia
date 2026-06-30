package dev.pavle.mediamonolith.video.infrastructure.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.video.domain.event.AllRenditionsCompletedEvent;
import dev.pavle.mediamonolith.video.domain.event.RenditionCompletedEvent;
import dev.pavle.mediamonolith.video.domain.event.RenditionFailedEvent;
import dev.pavle.mediamonolith.video.domain.port.RenditionEventPublisherPort;

@Component
public class SpringRenditionEventPublisher implements RenditionEventPublisherPort {

  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringRenditionEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publishRenditionCompleted(RenditionCompletedEvent event) {
    applicationEventPublisher.publishEvent(event);
  }

  @Override
  public void publishRenditionFailed(RenditionFailedEvent event) {
    applicationEventPublisher.publishEvent(event);
  }

  @Override
  public void publishAllRenditionsCompleted(AllRenditionsCompletedEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
