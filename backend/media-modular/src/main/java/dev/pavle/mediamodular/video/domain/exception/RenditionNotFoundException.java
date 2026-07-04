package dev.pavle.mediamodular.video.domain.exception;

import java.util.UUID;

public class RenditionNotFoundException extends RuntimeException {

  public RenditionNotFoundException(UUID renditionId) {
    super("Rendition not found: " + renditionId);
  }
}
