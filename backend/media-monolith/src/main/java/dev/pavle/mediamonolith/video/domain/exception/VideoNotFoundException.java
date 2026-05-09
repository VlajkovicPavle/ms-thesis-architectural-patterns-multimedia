package dev.pavle.mediamonolith.video.domain.exception;

import java.util.UUID;

public class VideoNotFoundException extends RuntimeException {

  public VideoNotFoundException(UUID videoId) {
    super("Video with id %s not found".formatted(videoId));
  }
}
