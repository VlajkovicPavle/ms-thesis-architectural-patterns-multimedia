package dev.pavle.mediamonolith.video.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public interface RenditionStoragePort {
  Rendition save(Rendition rendition);

  Optional<Rendition> findById(UUID renditionId);

  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);

  List<Rendition> findAllByVideoId(UUID videoId);
}
