package dev.pavle.media.notification;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.media.contracts.NotificationType;
import dev.pavle.media.contracts.TerminalNotification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class Notification {
  @Id private UUID id;
  private UUID videoId;
  private UUID renditionId;

  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Column(columnDefinition = "TEXT")
  private String message;

  private Instant createdAt;

  protected Notification() {}

  public Notification(TerminalNotification event) {
    id = UUID.randomUUID();
    videoId = event.videoId();
    renditionId = event.renditionId();
    type = event.type();
    message = event.message();
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getVideoId() {
    return videoId;
  }

  public UUID getRenditionId() {
    return renditionId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
