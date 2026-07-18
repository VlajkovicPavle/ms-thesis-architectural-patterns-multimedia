package dev.pavle.media.mediaservice.exception;

import java.util.UUID;

public class RenditionNotFoundException extends RuntimeException {
  public RenditionNotFoundException(UUID renditionId) {
    super("Rendition not found: " + renditionId);
  }
}
