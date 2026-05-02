package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;

public interface VideoProcessorPort {
  VideoMetadata extractMetadata(StoredFileRef ref);
}
