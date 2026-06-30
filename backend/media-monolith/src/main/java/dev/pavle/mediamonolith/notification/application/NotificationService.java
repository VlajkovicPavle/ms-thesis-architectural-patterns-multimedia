package dev.pavle.mediamonolith.notification.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.notification.domain.model.Notification;
import dev.pavle.mediamonolith.notification.domain.model.NotificationType;
import dev.pavle.mediamonolith.notification.domain.port.NotificationStoragePort;
import dev.pavle.mediamonolith.notification.domain.port.NotificationStreamPort;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

  private final NotificationStoragePort notificationStoragePort;
  private final NotificationStreamPort notificationStreamPort;

  public NotificationService(
      NotificationStoragePort notificationStoragePort,
      NotificationStreamPort notificationStreamPort) {
    this.notificationStoragePort = notificationStoragePort;
    this.notificationStreamPort = notificationStreamPort;
  }

  public void notifyRenditionCompleted(UUID videoId, UUID renditionId, String resolution) {
    String message = "%s rendition is ready".formatted(resolution);
    record(new Notification(videoId, renditionId, NotificationType.TASK_COMPLETED, message));
  }

  public void notifyRenditionFailed(UUID videoId, UUID renditionId, String resolution) {
    String message = "%s rendition failed".formatted(resolution);
    record(new Notification(videoId, renditionId, NotificationType.TASK_FAILED, message));
  }

  public void notifyAllRenditionsCompleted(UUID videoId) {
    String message = "All renditions are ready";
    record(new Notification(videoId, null, NotificationType.ALL_COMPLETED, message));
  }

  public List<Notification> list() {
    return notificationStoragePort.findAllNewestFirst();
  }

  private void record(Notification notification) {
    Notification saved = notificationStoragePort.save(notification);
    log.info("Recorded notification: {}", saved);
    notificationStreamPort.push(saved);
  }
}
