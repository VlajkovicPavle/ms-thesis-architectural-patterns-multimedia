package dev.pavle.mediamonolith.video.domain.port;

import java.io.InputStream;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;

public interface FileStoragePort {
  StoredFileRef createTemporary(InputStream inputStream, String tmpFileName);

  StoredFileRef persist(StoredFileRef temp, String fileName);

  void delete(StoredFileRef ref);
}
