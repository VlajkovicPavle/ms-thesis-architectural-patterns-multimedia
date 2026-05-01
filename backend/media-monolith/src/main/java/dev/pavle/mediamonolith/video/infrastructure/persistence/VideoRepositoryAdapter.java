package dev.pavle.mediamonolith.video.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.video.domain.model.Video;
import dev.pavle.mediamonolith.video.domain.port.VideoStoragePort;

@Repository
public class VideoRepositoryAdapter implements VideoStoragePort {

  private final VideoRepository repository;

  public VideoRepositoryAdapter(VideoRepository repository) {
    this.repository = repository;
  }

  @Override
  public Video save(Video video) {
    return repository.save(video);
  }
}
