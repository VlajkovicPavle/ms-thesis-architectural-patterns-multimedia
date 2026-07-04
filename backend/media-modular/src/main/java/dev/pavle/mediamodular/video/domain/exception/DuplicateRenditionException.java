package dev.pavle.mediamodular.video.domain.exception;

import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public class DuplicateRenditionException extends RuntimeException {

  public DuplicateRenditionException(VideoResolution resolution, String videoSysName) {
    super(
        "Rendition with resolution %s already exists for video %s"
            .formatted(resolution, videoSysName));
  }
}
