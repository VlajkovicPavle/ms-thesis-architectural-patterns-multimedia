package dev.pavle.mediamonolith.video.infrastructure.eventBus;

public class EventBusException extends RuntimeException {
  public EventBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
