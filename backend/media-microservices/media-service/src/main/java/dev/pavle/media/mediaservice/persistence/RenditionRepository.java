package dev.pavle.media.mediaservice.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.VideoResolution;
import dev.pavle.media.mediaservice.model.Rendition;

public interface RenditionRepository extends JpaRepository<Rendition, UUID> {
  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);

  List<Rendition> findAllByVideoId(UUID videoId);

  long countByVideoIdAndStatusIn(UUID videoId, Collection<RenditionStatus> statuses);
}
