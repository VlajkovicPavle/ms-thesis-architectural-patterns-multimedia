package dev.pavle.mediamonolith.video.domain.exception;

import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.rendition.RenditionStatus;

public class RenditionNotFinishedException extends RuntimeException {

  public RenditionNotFinishedException(UUID renditionId, RenditionStatus status) {
    super("Rendition %s is not finished (status: %s)".formatted(renditionId, status));
  }
}
