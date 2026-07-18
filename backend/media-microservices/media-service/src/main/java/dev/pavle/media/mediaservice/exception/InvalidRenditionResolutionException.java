package dev.pavle.media.mediaservice.exception;

import dev.pavle.media.contracts.VideoResolution;

public class InvalidRenditionResolutionException extends RuntimeException {
  public InvalidRenditionResolutionException(
      VideoResolution resolution, String videoSysName, int sourceHeight) {
    super(
        "Rendition resolution %s is not valid for source video %s (height=%d)"
            .formatted(resolution, videoSysName, sourceHeight));
  }
}
