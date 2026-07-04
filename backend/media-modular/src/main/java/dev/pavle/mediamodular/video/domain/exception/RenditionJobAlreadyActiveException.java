package dev.pavle.mediamodular.video.domain.exception;

import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public class RenditionJobAlreadyActiveException extends RuntimeException {

  public RenditionJobAlreadyActiveException(UUID videoId, VideoResolution resolution) {
    super(
        "Rendition job for resolution %s is already in progress for video %s"
            .formatted(resolution, videoId));
  }
}
