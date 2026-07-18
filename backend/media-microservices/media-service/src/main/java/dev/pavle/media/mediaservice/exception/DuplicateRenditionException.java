package dev.pavle.media.mediaservice.exception;

import dev.pavle.media.contracts.VideoResolution;

public class DuplicateRenditionException extends RuntimeException {
  public DuplicateRenditionException(VideoResolution resolution, String videoSysName) {
    super(
        "Rendition with resolution %s already exists for video %s"
            .formatted(resolution, videoSysName));
  }
}
