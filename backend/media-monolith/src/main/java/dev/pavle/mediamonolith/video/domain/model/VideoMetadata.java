package dev.pavle.mediamonolith.video.domain.model;

import dev.pavle.mediamonolith.video.infrastructure.processing.VideoProcessingException;
import jakarta.persistence.Embeddable;

@Embeddable
public record VideoMetadata(
    VideoCodec videoCodec,
    int width,
    int height,
    VideoContainerFormat videoContainerFormat,
    double duration,
    long size,
    long bitRate) {

  public VideoMetadata {
    if (width < VideoConstraints.MIN_VIDEO_WIDTH || width > VideoConstraints.MAX_VIDEO_WIDTH) {
      throw new VideoProcessingException(
          "Video width %d out of bounds [%d, %d]"
              .formatted(
                  width, VideoConstraints.MIN_VIDEO_WIDTH, VideoConstraints.MAX_VIDEO_WIDTH));
    }
    if (height < VideoConstraints.MIN_VIDEO_HEIGHT || height > VideoConstraints.MAX_VIDEO_HEIGHT) {
      throw new VideoProcessingException(
          "Video height %d out of bounds [%d, %d]"
              .formatted(
                  height, VideoConstraints.MIN_VIDEO_HEIGHT, VideoConstraints.MAX_VIDEO_HEIGHT));
    }
  }
}
