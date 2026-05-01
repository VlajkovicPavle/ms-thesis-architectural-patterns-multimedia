package dev.pavle.mediamonolith.video.domain.port;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStoragePort {
  Path createTemporary(InputStream inputStream, String tmpFileName);

  Path saveTemporary(Path tmpPath, String fileName);

  void delete(Path path);
}
