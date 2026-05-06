package dev.pavle.mediamonolith.video.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamonolith.video.domain.model.video.Video;
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

  @Override
  public Optional<Video> findById(UUID videoId) {
    return repository.findById(videoId);
  }
}
