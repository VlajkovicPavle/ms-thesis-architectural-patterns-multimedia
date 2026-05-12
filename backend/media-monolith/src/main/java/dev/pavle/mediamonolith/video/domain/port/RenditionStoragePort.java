package dev.pavle.mediamonolith.video.domain.port;

import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public interface RenditionStoragePort {
  Rendition save(Rendition rendition);

  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);
}
