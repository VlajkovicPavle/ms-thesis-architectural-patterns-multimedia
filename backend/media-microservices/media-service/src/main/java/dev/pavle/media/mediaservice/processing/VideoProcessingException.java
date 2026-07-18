package dev.pavle.media.mediaservice.processing;

public class VideoProcessingException extends RuntimeException {
  public VideoProcessingException(String message) {
    super(message);
  }

  public VideoProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
