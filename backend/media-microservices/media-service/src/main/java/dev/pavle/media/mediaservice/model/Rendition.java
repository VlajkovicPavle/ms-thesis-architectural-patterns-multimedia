package dev.pavle.media.mediaservice.model;

import java.util.UUID;

import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.VideoResolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "resolution"}))
public class Rendition extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  private String storedFileIdentifier;

  @Enumerated(EnumType.STRING)
  private VideoResolution resolution;

  private String name;

  @Enumerated(EnumType.STRING)
  private RenditionStatus status = RenditionStatus.PENDING;

  @Column(columnDefinition = "TEXT")
  private String error;

  protected Rendition() {}

  public Rendition(Video video, VideoResolution resolution) {
    super(UUID.randomUUID());
    this.video = video;
    this.resolution = resolution;
    this.name =
        "%s-%s-rendition.%s"
            .formatted(
                video.getSysName(),
                resolution,
                video.getMetadata().getVideoContainerFormat().getExtension());
  }

  public void retry() {
    status = RenditionStatus.PENDING;
    error = null;
    storedFileIdentifier = null;
  }

  public void markRunning() {
    status = RenditionStatus.RUNNING;
  }

  public void markFinished(String identifier) {
    storedFileIdentifier = identifier;
    status = RenditionStatus.FINISHED;
    error = null;
  }

  public void markFailed(String error) {
    status = RenditionStatus.ERROR;
    this.error = error;
  }

  public Video getVideo() {
    return video;
  }

  public String getStoredFileIdentifier() {
    return storedFileIdentifier;
  }

  public VideoResolution getResolution() {
    return resolution;
  }

  public String getName() {
    return name;
  }

  public RenditionStatus getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }
}
