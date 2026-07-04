package dev.pavle.mediamodular.video.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.rendition.Rendition;
import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public interface RenditionStoragePort {
  Rendition save(Rendition rendition);

  Optional<Rendition> findById(UUID renditionId);

  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);

  List<Rendition> findAllByVideoId(UUID videoId);

  long countByStatus(RenditionStatus status);

  long countByVideoIdAndStatusIn(UUID videoId, Collection<RenditionStatus> statuses);
}
