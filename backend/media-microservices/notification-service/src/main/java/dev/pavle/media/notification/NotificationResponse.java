package dev.pavle.media.notification;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.media.contracts.NotificationType;

public record NotificationResponse(
    UUID id,
    UUID videoId,
    UUID renditionId,
    NotificationType type,
    String message,
    Instant createdAt) {
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getVideoId(),
        notification.getRenditionId(),
        notification.getType(),
        notification.getMessage(),
        notification.getCreatedAt());
  }
}
