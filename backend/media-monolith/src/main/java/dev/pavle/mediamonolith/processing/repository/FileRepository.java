package dev.pavle.mediamonolith.processing.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

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

  public Path createTempFile(MultipartFile file) {
    String tmpFileName =
        Optional.ofNullable(file.getOriginalFilename())
            .filter(name -> !name.isBlank())
            .orElse(UUID.randomUUID().toString());
    Path tmpPath = rootStoragePath.resolve(TMP_PREFIX).resolve(tmpFileName);
    try {
      file.transferTo(tmpPath);
    } catch (IOException e) {
      throw new FileStorageException("Failed to write temp file: " + tmpFileName, e);
    }
    return tmpPath;
  }

  private void createFileRepositories(final Path rootStoragePath) throws IOException {
    Files.createDirectories(rootStoragePath.resolve(TMP_PREFIX));
    Files.createDirectories(rootStoragePath.resolve(STORAGE_PREFIX));
  }
}
