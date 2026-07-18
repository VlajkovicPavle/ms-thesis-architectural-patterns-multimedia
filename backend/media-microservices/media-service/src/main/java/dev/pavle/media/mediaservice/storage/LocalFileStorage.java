package dev.pavle.media.mediaservice.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

import dev.pavle.media.mediaservice.config.StorageProperties;

@Component
public class LocalFileStorage {
  private static final String TMP_DIRECTORY = "tmp";
  private static final String STORAGE_DIRECTORY = "storage";
  private final Path root;

  public LocalFileStorage(StorageProperties properties) throws IOException {
    root = Path.of(properties.getPath());
    Files.createDirectories(root.resolve(TMP_DIRECTORY));
    Files.createDirectories(root.resolve(STORAGE_DIRECTORY));
  }

  public String createTemporary(InputStream content, String fileName) {
    Path target = root.resolve(TMP_DIRECTORY).resolve(fileName);
    try {
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
      return target.toString();
    } catch (IOException exception) {
      throw new FileStorageException("Failed to write temp file: " + fileName, exception);
    }
  }

  public String persist(String temporaryIdentifier, String fileName) {
    Path target = root.resolve(STORAGE_DIRECTORY).resolve(fileName);
    try {
      return Files.move(Path.of(temporaryIdentifier), target, StandardCopyOption.REPLACE_EXISTING)
          .toString();
    } catch (IOException exception) {
      throw new FileStorageException("Failed to persist file: " + fileName, exception);
    }
  }

  public InputStream open(String identifier) {
    try {
      return Files.newInputStream(Path.of(identifier));
    } catch (IOException exception) {
      throw new FileStorageException("Failed to open file: " + identifier, exception);
    }
  }
}
