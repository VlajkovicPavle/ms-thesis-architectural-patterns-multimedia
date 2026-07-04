package dev.pavle.mediamodular.notification.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamodular.notification.domain.model.Notification;
import dev.pavle.mediamodular.notification.domain.port.NotificationStoragePort;

@Repository
public class NotificationRepositoryAdapter implements NotificationStoragePort {

  private final NotificationRepository repository;

  public NotificationRepositoryAdapter(NotificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Notification save(Notification notification) {
    return repository.save(notification);
  }

  @Override
  public List<Notification> findAllNewestFirst() {
    return repository.findAllByOrderByCreatedAtDesc();
  }
}
