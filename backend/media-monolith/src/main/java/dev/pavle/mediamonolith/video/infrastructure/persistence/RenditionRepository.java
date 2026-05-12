package dev.pavle.mediamonolith.video.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;

public interface RenditionRepository extends JpaRepository<Rendition, UUID> {
  Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution);
}
