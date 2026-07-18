package dev.pavle.media.mediaservice.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.pavle.media.mediaservice.model.Video;

public record VideoDetailsResponse(
    UUID id,
    String name,
    VideoMetadataResponse metadata,
    List<RenditionResponse> renditions,
    Instant createdAt) {
  public static VideoDetailsResponse from(Video video, List<RenditionResponse> renditions) {
    return new VideoDetailsResponse(
        video.getId(),
        video.getOriginalName(),
        VideoMetadataResponse.from(video.getMetadata()),
        renditions,
        video.getCreatedAt());
  }
}
