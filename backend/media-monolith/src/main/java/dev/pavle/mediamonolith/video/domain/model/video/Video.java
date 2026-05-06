package dev.pavle.mediamonolith.video.domain.model.video;

import java.util.ArrayList;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.exception.DuplicateRenditionException;
import dev.pavle.mediamonolith.video.domain.exception.InvalidRenditionResolutionException;
import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.shared.BaseAggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseAggregateRoot<Video> {

  @Getter private String originalName;

  @Getter private String sysName;

  @Getter @Setter private String sysPath = null;

  @Getter @Embedded private VideoMetadata metadata;

  @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
  private final ArrayList<Rendition> renditions = new ArrayList<>();

  public Video(String originalName, VideoMetadata metadata) {
    this.originalName = originalName;
    this.metadata = metadata;
    this.sysName = generateSysName();
  }

  public void addRendition(Rendition rendition) {
    if (renditions.contains(rendition)) {
      throw new DuplicateRenditionException(rendition.getResolution(), this.sysName);
    }
    if (rendition.getResolution().isUpscaleOf(this.metadata.height())) {
      throw new InvalidRenditionResolutionException(
          rendition.getResolution(), this.sysName, this.metadata.height());
    }
    this.renditions.add(rendition);
    registerEvent(new CreateRenditionEvent(this.getId(), rendition.getResolution()));
  }

  private String generateSysName() {
    return "sys-%s-%s".formatted(this.originalName, UUID.randomUUID());
  }
}
