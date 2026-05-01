package dev.pavle.mediamonolith.video.infrastructure.processing;

public class VideoProcessingException extends RuntimeException {

  public VideoProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public VideoProcessingException(String message) {
    super(message);
  }
}
