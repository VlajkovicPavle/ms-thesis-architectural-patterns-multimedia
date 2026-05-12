package dev.pavle.mediamonolith.video.domain.exception;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import java.util.UUID;

public class RenditionJobAlreadyActiveException extends RuntimeException {

  public RenditionJobAlreadyActiveException(UUID videoId, VideoResolution resolution) {
    super(
        "Rendition job for resolution %s is already in progress for video %s"
            .formatted(resolution, videoId));
  }
}
