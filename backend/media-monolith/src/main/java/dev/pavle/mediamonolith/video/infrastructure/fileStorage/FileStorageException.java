package dev.pavle.mediamonolith.video.infrastructure.fileStorage;

public class FileStorageException extends RuntimeException {

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
