package dev.pavle.mediamodular.notification.domain.port;

import dev.pavle.mediamodular.notification.domain.model.Notification;

public interface NotificationStreamPort {
  void push(Notification notification);
}
