package dev.pavle.mediamonolith.notification.domain.port;

import dev.pavle.mediamonolith.notification.domain.model.Notification;

public interface NotificationStreamPort {
  void push(Notification notification);
}
