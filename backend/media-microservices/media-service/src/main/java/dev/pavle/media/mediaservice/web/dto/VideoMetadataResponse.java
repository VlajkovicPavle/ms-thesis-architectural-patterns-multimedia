package dev.pavle.media.mediaservice.web.dto;

import dev.pavle.media.mediaservice.model.VideoCodec;
import dev.pavle.media.mediaservice.model.VideoContainerFormat;
import dev.pavle.media.mediaservice.model.VideoMetadata;

public record VideoMetadataResponse(
    VideoCodec videoCodec,
    VideoContainerFormat containerFormat,
    int width,
    int height,
    double durationSeconds,
    long sizeBytes,
    long bitRateBps) {
  public static VideoMetadataResponse from(VideoMetadata metadata) {
    return new VideoMetadataResponse(
        metadata.getVideoCodec(),
        metadata.getVideoContainerFormat(),
        metadata.getWidth(),
        metadata.getHeight(),
        metadata.getDuration(),
        metadata.getSize(),
        metadata.getBitRate());
  }
}
