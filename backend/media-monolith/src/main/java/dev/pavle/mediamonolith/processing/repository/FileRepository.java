package dev.pavle.mediamonolith.processing.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.config.StorageProperties;
import dev.pavle.mediamonolith.processing.exceptions.FileStorageException;

@Repository
public class FileRepository {
  private static final String TMP_PREFIX = "tmp";
  private static final String STORAGE_PREFIX = "storage";
  private final Path rootStoragePath;

  FileRepository(StorageProperties storageProperties) throws IOException {
    this.rootStoragePath = Paths.get(storageProperties.path());
    createFileRepositories(rootStoragePath);
  }

  public Path createTemp(InputStream inputStream, String fileName) {
    Path tmpPath = rootStoragePath.resolve(TMP_PREFIX).resolve(fileName);
    try {
      Files.copy(inputStream, tmpPath);
    } catch (IOException e) {
      throw new FileStorageException("Failed to write temp file: " + fileName, e);
    }
    return tmpPath;
  }

  public Path persist(Path tmpPath, String fileName) {
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
