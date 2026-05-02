package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.model.video.Video;

public interface VideoStoragePort {
  Video save(Video video);
}
