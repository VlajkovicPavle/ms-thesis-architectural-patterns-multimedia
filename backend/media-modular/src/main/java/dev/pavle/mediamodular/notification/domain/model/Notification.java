package dev.pavle.mediamodular.notification.domain.model;

import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.shared.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseAggregateRoot<Notification> {

  private UUID videoId;

  private UUID renditionId;

  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Column(columnDefinition = "TEXT")
  private String message;

  public Notification(UUID videoId, UUID renditionId, NotificationType type, String message) {
    this.videoId = videoId;
    this.renditionId = renditionId;
    this.type = type;
    this.message = message;
  }
}
