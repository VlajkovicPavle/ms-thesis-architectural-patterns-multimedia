package dev.pavle.mediamodular.notification.domain.port;

import java.util.List;

import dev.pavle.mediamodular.notification.domain.model.Notification;

public interface NotificationStoragePort {
  Notification save(Notification notification);

  List<Notification> findAllNewestFirst();
}
