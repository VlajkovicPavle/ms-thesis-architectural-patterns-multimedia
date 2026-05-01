package dev.pavle.mediamonolith.video.domain.port;

import dev.pavle.mediamonolith.video.domain.model.VideoMetadata;

import java.nio.file.Path;

public interface ProcessingPort {
    VideoMetadata extractMetadata(Path filePath);
}
