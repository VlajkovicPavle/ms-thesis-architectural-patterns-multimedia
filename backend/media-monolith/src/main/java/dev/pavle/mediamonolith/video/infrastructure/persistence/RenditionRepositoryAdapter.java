package dev.pavle.mediamonolith.video.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import dev.pavle.mediamonolith.video.domain.port.RenditionStoragePort;

@Repository
public class RenditionRepositoryAdapter implements RenditionStoragePort {

  private final RenditionRepository repository;

  public RenditionRepositoryAdapter(RenditionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Rendition save(Rendition rendition) {
    return repository.save(rendition);
  }

  @Override
  public Optional<Rendition> findById(UUID renditionId) {
    return repository.findById(renditionId);
  }

  @Override
  public Optional<Rendition> findByVideoIdAndResolution(UUID videoId, VideoResolution resolution) {
    return repository.findByVideoIdAndResolution(videoId, resolution);
  }

  @Override
  public List<Rendition> findAllByVideoId(UUID videoId) {
    return repository.findAllByVideoId(videoId);
  }
}
