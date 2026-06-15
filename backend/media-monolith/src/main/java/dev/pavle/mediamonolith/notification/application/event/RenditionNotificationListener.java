package dev.pavle.mediamonolith.notification.application.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.notification.application.NotificationService;
import dev.pavle.mediamonolith.video.domain.event.RenditionCompletedEvent;
import dev.pavle.mediamonolith.video.domain.event.RenditionFailedEvent;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RenditionNotificationListener {

  private final NotificationService notificationService;

  public RenditionNotificationListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener
  public void onRenditionCompleted(RenditionCompletedEvent event) {
    try {
      notificationService.notifyRenditionCompleted(
          event.videoId(), event.renditionId(), event.resolution().name());
    } catch (Exception e) {
      log.error("Failed to record completion notification for {}", event, e);
    }
  }

  @EventListener
  public void onRenditionFailed(RenditionFailedEvent event) {
    try {
      notificationService.notifyRenditionFailed(
          event.videoId(), event.renditionId(), event.resolution().name(), event.error());
    } catch (Exception e) {
      log.error("Failed to record failure notification for {}", event, e);
    }
  }
}
