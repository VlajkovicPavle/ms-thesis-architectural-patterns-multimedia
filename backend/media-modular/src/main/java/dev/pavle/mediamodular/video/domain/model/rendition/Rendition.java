package dev.pavle.mediamodular.video.domain.model.rendition;

import dev.pavle.mediamodular.shared.BaseEntity;
import dev.pavle.mediamodular.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamodular.video.domain.model.video.Video;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Rendition extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  @Setter @Embedded private StoredFileRef storedFileRef;

  @Enumerated(EnumType.STRING)
  private VideoResolution resolution;

  @EqualsAndHashCode.Include private String name;

  @Setter
  @Enumerated(EnumType.STRING)
  private RenditionStatus status = RenditionStatus.PENDING;

  @Setter
  @Column(columnDefinition = "TEXT")
  private String error;

  public Rendition(Video video, VideoResolution resolution) {
    this.video = video;
    this.resolution = resolution;
    this.name = generateName();
  }

  private String generateName() {
    return "%s-%s-rendition.%s"
        .formatted(
            video.getSysName(),
            this.resolution,
            video.getMetadata().videoContainerFormat().getExtension());
  }
}
