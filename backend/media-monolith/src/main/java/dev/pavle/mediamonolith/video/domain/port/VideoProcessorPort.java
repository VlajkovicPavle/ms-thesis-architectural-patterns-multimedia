package dev.pavle.mediamonolith.video.domain.port;

import org.hibernate.engine.spi.Resolution;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;

public interface VideoProcessorPort {
  VideoMetadata extractMetadata(StoredFileRef ref);

  StoredFileRef createRendition(StoredFileRef source, Resolution resolution);
}
