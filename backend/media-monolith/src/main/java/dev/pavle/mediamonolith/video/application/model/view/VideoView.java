package dev.pavle.mediamonolith.video.application.model.view;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.Video;

public record VideoView(
    UUID id, String originalName, VideoMetadataView metadata, Instant createdAt) {
  public static VideoView from(Video video) {
    return new VideoView(
        video.getId(),
        video.getOriginalName(),
        VideoMetadataView.from(video.getMetadata()),
        video.getCreatedAt());
  }
}
