package dev.pavle.media.mediaservice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class Video extends BaseEntity {
  private String originalName;
  private String sysName;
  private String sysPath;

  @Embedded private VideoMetadata metadata;

  @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Rendition> renditions = new ArrayList<>();

  protected Video() {}

  public Video(String originalName, VideoMetadata metadata) {
    super(UUID.randomUUID());
    this.originalName = originalName;
    this.metadata = metadata;
    this.sysName = "sys-%s-%s".formatted(originalName, UUID.randomUUID());
  }

  public void addRendition(Rendition rendition) {
    renditions.add(rendition);
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getSysName() {
    return sysName;
  }

  public String getSysPath() {
    return sysPath;
  }

  public void setSysPath(String sysPath) {
    this.sysPath = sysPath;
  }

  public VideoMetadata getMetadata() {
    return metadata;
  }

  public List<Rendition> getRenditions() {
    return renditions;
  }
}
