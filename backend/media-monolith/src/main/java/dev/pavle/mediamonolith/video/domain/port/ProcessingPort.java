package dev.pavle.mediamonolith.video.domain.port;

import java.nio.file.Path;

import dev.pavle.mediamonolith.video.domain.model.VideoMetadata;

public interface ProcessingPort {
  VideoMetadata extractMetadata(Path filePath);
}
