package dev.pavle.mediamodular.video.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamodular.video.domain.model.rendition.Rendition;
import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;

public interface RenditionRepository extends JpaRepository<Rendition, UUID> {
  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);

  List<Rendition> findAllByVideoId(UUID videoId);

  long countByStatus(RenditionStatus status);

  long countByVideoIdAndStatusIn(UUID videoId, Collection<RenditionStatus> statuses);
}
