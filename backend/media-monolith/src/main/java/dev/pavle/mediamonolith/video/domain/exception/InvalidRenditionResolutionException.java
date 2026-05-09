package dev.pavle.mediamonolith.video.domain.exception;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public class InvalidRenditionResolutionException extends RuntimeException {

  public InvalidRenditionResolutionException(
      VideoResolution resolution, String videoSysName, int sourceHeight) {
    super(
        "Rendition resolution %s is not valid for source video %s (height=%d)"
            .formatted(resolution, videoSysName, sourceHeight));
  }
}
