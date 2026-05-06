package dev.pavle.mediamonolith.video.domain.port;

import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.Video;

public interface VideoStoragePort {
  Video save(Video video);

  Optional<Video> findById(UUID videoId);
}
