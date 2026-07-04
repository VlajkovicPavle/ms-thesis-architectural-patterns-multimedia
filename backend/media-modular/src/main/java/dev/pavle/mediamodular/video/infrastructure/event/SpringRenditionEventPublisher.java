package dev.pavle.mediamodular.video.infrastructure.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import dev.pavle.mediamodular.video.domain.port.RenditionEventPublisherPort;
import dev.pavle.mediamodular.video.published.AllRenditionsCompletedEvent;
import dev.pavle.mediamodular.video.published.RenditionCompletedEvent;
import dev.pavle.mediamodular.video.published.RenditionFailedEvent;

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
