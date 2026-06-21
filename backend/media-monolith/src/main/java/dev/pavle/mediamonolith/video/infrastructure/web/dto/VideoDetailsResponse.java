package dev.pavle.mediamonolith.video.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.pavle.mediamonolith.video.application.model.view.VideoMetadataView;
import dev.pavle.mediamonolith.video.application.model.view.VideoView;

public record VideoDetailsResponse(
    UUID id,
    String name,
    VideoMetadataView metadata,
    List<RenditionResponse> renditions,
    Instant createdAt) {

  public static VideoDetailsResponse from(VideoView view, List<RenditionResponse> renditions) {
    return new VideoDetailsResponse(
        view.id(), view.originalName(), view.metadata(), renditions, view.createdAt());
  }
}
