package dev.pavle.mediamodular.video.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamodular.video.application.model.view.VideoMetadataView;
import dev.pavle.mediamodular.video.application.model.view.VideoView;

public record VideoResponse(UUID id, String name, VideoMetadataView metadata, Instant createdAt) {

  public static VideoResponse from(VideoView view) {
    return new VideoResponse(view.id(), view.originalName(), view.metadata(), view.createdAt());
  }
}
