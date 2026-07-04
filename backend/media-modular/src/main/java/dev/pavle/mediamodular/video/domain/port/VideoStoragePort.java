package dev.pavle.mediamodular.video.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamodular.video.domain.model.video.Video;

public interface VideoStoragePort {
  Video save(Video video);

  List<Video> findAllNewestFirst();

  Optional<Video> findById(UUID videoId);
}
