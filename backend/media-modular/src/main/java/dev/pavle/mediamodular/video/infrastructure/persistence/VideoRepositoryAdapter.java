package dev.pavle.mediamodular.video.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.pavle.mediamodular.video.domain.model.video.Video;
import dev.pavle.mediamodular.video.domain.port.VideoStoragePort;

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
  public List<Video> findAllNewestFirst() {
    return repository.findAllByOrderByCreatedAtDesc();
  }

  @Override
  public Optional<Video> findById(UUID videoId) {
    return repository.findById(videoId);
  }
}
