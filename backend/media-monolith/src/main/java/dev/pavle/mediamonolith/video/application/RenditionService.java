package dev.pavle.mediamonolith.video.application;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.exception.VideoNotFoundException;
import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.video.Video;
import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import dev.pavle.mediamonolith.video.domain.port.VideoStoragePort;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RenditionService {

  private final VideoStoragePort videoStoragePort;

  public RenditionService(VideoStoragePort videoStoragePort) {
    this.videoStoragePort = videoStoragePort;
  }

  public void createRendition(UUID videoId, Set<VideoResolution> resolutions) {
    Video video =
        videoStoragePort.findById(videoId).orElseThrow(() -> new VideoNotFoundException(videoId));
    var renditions = resolutions.stream().map((resolution -> new Rendition(video, resolution)));
    renditions.forEach(video::addRendition);
    videoStoragePort.save(video);
  }
}
