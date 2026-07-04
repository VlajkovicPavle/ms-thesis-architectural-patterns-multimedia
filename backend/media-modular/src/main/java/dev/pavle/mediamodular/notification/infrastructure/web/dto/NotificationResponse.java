package dev.pavle.mediamodular.notification.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamodular.notification.domain.model.Notification;
import dev.pavle.mediamodular.notification.domain.model.NotificationType;

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
