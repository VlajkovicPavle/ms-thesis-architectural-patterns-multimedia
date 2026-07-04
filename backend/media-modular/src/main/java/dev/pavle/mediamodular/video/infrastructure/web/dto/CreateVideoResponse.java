package dev.pavle.mediamodular.video.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.pavle.mediamodular.video.application.model.view.VideoMetadataView;
import dev.pavle.mediamodular.video.application.model.view.VideoView;

public record CreateVideoResponse(
    UUID id, String name, VideoMetadataView metadata, Instant createdAt) {
  public static CreateVideoResponse from(VideoView view) {
    return new CreateVideoResponse(
        view.id(), view.originalName(), view.metadata(), view.createdAt());
  }
}
