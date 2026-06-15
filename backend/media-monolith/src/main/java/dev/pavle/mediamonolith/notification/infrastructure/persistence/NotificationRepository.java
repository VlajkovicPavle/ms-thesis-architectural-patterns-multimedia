package dev.pavle.mediamonolith.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamonolith.notification.domain.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  List<Notification> findAllByOrderByCreatedAtDesc();
}
