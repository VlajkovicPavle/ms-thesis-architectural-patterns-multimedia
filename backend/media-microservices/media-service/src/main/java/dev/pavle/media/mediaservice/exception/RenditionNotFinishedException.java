package dev.pavle.media.mediaservice.exception;

import java.util.UUID;

import dev.pavle.media.contracts.RenditionStatus;

public class RenditionNotFinishedException extends RuntimeException {
  public RenditionNotFinishedException(UUID renditionId, RenditionStatus status) {
    super("Rendition %s is not finished (status: %s)".formatted(renditionId, status));
  }
}
