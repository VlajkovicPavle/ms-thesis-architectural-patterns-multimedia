package dev.pavle.mediamonolith.video.domain.model;

import dev.pavle.mediamonolith.shared.BaseEntity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {
  private String originalName;
  private String sysName;
  @Setter private String sysPath = null;
  @Embedded private VideoMetadata metadata;

  public Video(String originalName, VideoMetadata metadata) {
    this.originalName = originalName;
    this.metadata = metadata;
    this.sysName = generateSysName();
  }

  private String generateSysName() {
    return "sys-%s-%s".formatted(this.originalName, UUID.randomUUID());
  }
}
