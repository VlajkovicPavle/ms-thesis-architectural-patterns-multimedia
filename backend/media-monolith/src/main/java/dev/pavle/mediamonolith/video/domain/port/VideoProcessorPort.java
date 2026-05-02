package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.model.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.VideoMetadata;

public interface VideoProcessorPort {
  VideoMetadata extractMetadata(StoredFileRef ref);
}
