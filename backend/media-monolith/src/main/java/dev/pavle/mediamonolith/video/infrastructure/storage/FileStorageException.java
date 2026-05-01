package dev.pavle.mediamonolith.video.infrastructure.storage;

public class FileStorageException extends RuntimeException {

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
