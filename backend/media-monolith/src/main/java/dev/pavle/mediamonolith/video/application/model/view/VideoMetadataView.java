package dev.pavle.mediamonolith.video.application.model.view;

import dev.pavle.mediamonolith.video.domain.model.video.VideoCodec;
import dev.pavle.mediamonolith.video.domain.model.video.VideoContainerFormat;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;

public record VideoMetadataView(
    VideoCodec videoCodec,
    VideoContainerFormat containerFormat,
    int width,
    int height,
    double durationSeconds,
    long sizeBytes,
    long bitRateBps) {
  public static VideoMetadataView from(VideoMetadata m) {
    return new VideoMetadataView(
        m.videoCodec(),
        m.videoContainerFormat(),
        m.width(),
        m.height(),
        m.duration(),
        m.size(),
        m.bitRate());
  }
}
