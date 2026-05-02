package dev.pavle.mediamonolith.video.domain.model.video;

import java.util.ArrayList;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {

  private String originalName;

  private String sysName;

  @Setter private String sysPath = null;

  @Embedded private VideoMetadata metadata;

  @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
  private final ArrayList<Rendition> renditions = new ArrayList<>();

  public Video(String originalName, VideoMetadata metadata) {
    this.originalName = originalName;
    this.metadata = metadata;
    this.sysName = generateSysName();
  }

  private String generateSysName() {
    return "sys-%s-%s".formatted(this.originalName, UUID.randomUUID());
  }
}
