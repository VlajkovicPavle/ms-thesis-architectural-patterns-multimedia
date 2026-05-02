package dev.pavle.mediamonolith.video.infrastructure.filestorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.config.StorageProperties;
import dev.pavle.mediamonolith.video.domain.model.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.port.FileStoragePort;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class LocalFileStorageAdapter implements FileStoragePort {
  private static final String TMP_PREFIX = "tmp";
  private static final String STORAGE_PREFIX = "storage";
  private final Path rootStoragePath;

  LocalFileStorageAdapter(StorageProperties storageProperties) throws IOException {
    this.rootStoragePath = Paths.get(storageProperties.path());
    createStorageDirs(rootStoragePath);
  }

  @Override
  public StoredFileRef createTemporary(InputStream inputStream, String tmpFileName) {
    Path tmpPath = rootStoragePath.resolve(TMP_PREFIX).resolve(tmpFileName);
    try {
      Files.copy(inputStream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
      log.info("Temp file created: path={}", tmpPath);
    } catch (IOException e) {
      throw new FileStorageException("Failed to write temp file: " + tmpFileName, e);
    }
    return new StoredFileRef(tmpPath.toString());
  }

  @Override
  public StoredFileRef persist(StoredFileRef temp, String fileName) {
    Path target = rootStoragePath.resolve(STORAGE_PREFIX).resolve(fileName);
    try {
      Path moved = Files.move(Paths.get(temp.identifier()), target);
      return new StoredFileRef(moved.toString());
    } catch (IOException e) {
      throw new FileStorageException("Failed to persist file: " + fileName, e);
    }
  }

  @Override
  public void delete(StoredFileRef ref) {
    try {
      Files.deleteIfExists(Paths.get(ref.identifier()));
    } catch (IOException e) {
      throw new FileStorageException("Failed to delete file: " + ref.identifier(), e);
    }
  }

  private void createStorageDirs(final Path rootStoragePath) throws IOException {
    Files.createDirectories(rootStoragePath.resolve(TMP_PREFIX));
    Files.createDirectories(rootStoragePath.resolve(STORAGE_PREFIX));
  }
}
