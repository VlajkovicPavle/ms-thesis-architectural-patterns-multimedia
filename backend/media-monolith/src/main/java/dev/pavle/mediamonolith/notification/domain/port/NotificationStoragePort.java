package dev.pavle.mediamonolith.notification.domain.port;

import java.util.List;

import dev.pavle.mediamonolith.notification.domain.model.Notification;

public interface NotificationStoragePort {
  Notification save(Notification notification);

  List<Notification> findAllNewestFirst();
}
