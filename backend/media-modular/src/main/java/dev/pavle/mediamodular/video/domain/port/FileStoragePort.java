package dev.pavle.mediamodular.video.domain.port;

import java.io.InputStream;

import dev.pavle.mediamodular.video.domain.model.shared.StoredFileRef;

public interface FileStoragePort {
  StoredFileRef createTemporary(InputStream inputStream, String tmpFileName);

  StoredFileRef persist(StoredFileRef temp, String fileName);

  InputStream open(StoredFileRef ref);

  void delete(StoredFileRef ref);
}
