package dev.pavle.mediamodular.video.domain.port;

import dev.pavle.mediamodular.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamodular.video.domain.model.video.VideoMetadata;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public interface VideoProcessorPort {
  VideoMetadata extractMetadata(StoredFileRef ref);

  StoredFileRef createRendition(
      StoredFileRef source, VideoResolution resolution, String outputFileName);
}
