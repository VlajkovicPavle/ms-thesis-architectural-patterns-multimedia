package dev.pavle.mediamonolith.video.application;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.exception.RenditionJobAlreadyActiveException;
import dev.pavle.mediamonolith.video.domain.exception.VideoNotFoundException;
import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.port.FileStoragePort;
import dev.pavle.mediamonolith.video.domain.port.RenditionStoragePort;
import dev.pavle.mediamonolith.video.domain.port.VideoProcessorPort;
import dev.pavle.mediamonolith.video.domain.port.VideoStoragePort;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RenditionJobHandler {

  private final ExecutorService executorService;
  private final Set<CreateRenditionEvent> activeRenditions = ConcurrentHashMap.newKeySet();

  private final VideoStoragePort videoStoragePort;
  private final RenditionStoragePort renditionStoragePort;
  private final VideoProcessorPort videoProcessorPort;
  private final FileStoragePort fileStoragePort;

  public RenditionJobHandler(
      @Value("${rendition.pool-size}") int poolSize,
      VideoStoragePort videoStoragePort,
      RenditionStoragePort renditionStoragePort,
      VideoProcessorPort videoProcessorPort,
      FileStoragePort fileStoragePort) {
    this.executorService = Executors.newFixedThreadPool(poolSize);
    this.videoStoragePort = videoStoragePort;
    this.renditionStoragePort = renditionStoragePort;
    this.videoProcessorPort = videoProcessorPort;
    this.fileStoragePort = fileStoragePort;
  }

  public void dispatchCreateRenditionJob(CreateRenditionEvent event) {
    if (activeRenditions.contains(event)) {
      throw new RenditionJobAlreadyActiveException(event.videoId(), event.resolution());
    }
    activeRenditions.add(event);
    executorService.submit(() -> executeRenditionJob(event));
  }

  // TODO: Implement the rendition transcoding job body
  private void executeRenditionJob(CreateRenditionEvent event) {
  }
}
