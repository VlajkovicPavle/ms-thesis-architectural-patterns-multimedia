package dev.pavle.mediamodular.video.domain.model.video;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.pavle.mediamodular.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamodular.video.domain.exception.DuplicateRenditionException;
import dev.pavle.mediamodular.video.domain.exception.InvalidRenditionResolutionException;
import dev.pavle.mediamodular.video.domain.model.rendition.Rendition;
import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.model.shared.BaseAggregateRoot;
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
  private final Set<Rendition> renditions = new LinkedHashSet<>();

  public Video(String originalName, VideoMetadata metadata) {
    this.originalName = originalName;
    this.metadata = metadata;
    this.sysName = generateSysName();
  }

  public void addRendition(Rendition rendition) {
    if (!this.getId().equals(rendition.getVideo().getId())) {
      throw new IllegalArgumentException(
          "Rendition does not belong to video %s".formatted(this.getId()));
    }
    if (rendition.getResolution().isUpscaleOf(this.metadata.height())) {
      throw new InvalidRenditionResolutionException(
          rendition.getResolution(), this.sysName, this.metadata.height());
    }
    Optional<Rendition> existing =
        this.renditions.stream()
            .filter(stored -> stored.getResolution().equals(rendition.getResolution()))
            .findFirst();
    if (existing.isPresent()) {
      Rendition stored = existing.get();
      if (stored.getStatus() != RenditionStatus.CANCELED
          && stored.getStatus() != RenditionStatus.ERROR) {
        throw new DuplicateRenditionException(rendition.getResolution(), this.sysName);
      }
      stored.setStatus(RenditionStatus.PENDING);
      stored.setError(null);
      registerEvent(new CreateRenditionEvent(this.getId(), rendition.getResolution()));
      return;
    }

    this.renditions.add(rendition);
    registerEvent(new CreateRenditionEvent(this.getId(), rendition.getResolution()));
  }

  private String generateSysName() {
    return "sys-%s-%s".formatted(this.originalName, UUID.randomUUID());
  }
}
