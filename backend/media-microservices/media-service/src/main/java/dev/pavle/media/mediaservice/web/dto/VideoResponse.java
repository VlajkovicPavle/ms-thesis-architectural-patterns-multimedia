package dev.pavle.media.mediaservice.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.media.mediaservice.model.Video;

public record VideoResponse(
    UUID id, String name, VideoMetadataResponse metadata, Instant createdAt) {
  public static VideoResponse from(Video video) {
    return new VideoResponse(
        video.getId(),
        video.getOriginalName(),
        VideoMetadataResponse.from(video.getMetadata()),
        video.getCreatedAt());
  }
}
