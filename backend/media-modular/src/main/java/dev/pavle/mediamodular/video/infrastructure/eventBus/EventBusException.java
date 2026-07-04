package dev.pavle.mediamodular.video.infrastructure.eventBus;

public class EventBusException extends RuntimeException {
  public EventBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
