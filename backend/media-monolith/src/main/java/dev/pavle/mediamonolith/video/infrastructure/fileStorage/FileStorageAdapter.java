package dev.pavle.mediamonolith.video.infrastructure.fileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.config.StorageProperties;
import dev.pavle.mediamonolith.video.domain.port.FileStoragePort;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class FileStorageAdapter implements FileStoragePort {
  private static final String TMP_PREFIX = "tmp";
  private static final String STORAGE_PREFIX = "storage";
  private final Path rootStoragePath;

  FileStorageAdapter(StorageProperties storageProperties) throws IOException {
    this.rootStoragePath = Paths.get(storageProperties.path());
    createFileRepositories(rootStoragePath);
  }

  public Path createTemporary(InputStream inputStream, String tmpFileName) {
    Path tmpPath = rootStoragePath.resolve(TMP_PREFIX).resolve(tmpFileName);
    try {
      Files.copy(inputStream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
      log.info("Temp file created: path={}", tmpPath);

    } catch (IOException e) {
      throw new FileStorageException("Failed to write temp file: " + tmpFileName, e);
    }
    return tmpPath;
  }

  public Path saveTemporary(Path tmpPath, String fileName) {
    Path target = rootStoragePath.resolve(STORAGE_PREFIX).resolve(fileName);
    try {
      return Files.move(tmpPath, target);
    } catch (IOException e) {
      throw new FileStorageException("Failed to persist file: " + fileName, e);
    }
  }

  public void delete(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new FileStorageException("Failed to delete file: " + path, e);
    }
  }

  private void createFileRepositories(final Path rootStoragePath) throws IOException {
    Files.createDirectories(rootStoragePath.resolve(TMP_PREFIX));
    Files.createDirectories(rootStoragePath.resolve(STORAGE_PREFIX));
  }
}
