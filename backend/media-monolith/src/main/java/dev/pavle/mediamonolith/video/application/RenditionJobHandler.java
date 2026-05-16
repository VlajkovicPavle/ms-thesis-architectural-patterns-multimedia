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
import dev.pavle.mediamonolith.video.domain.model.rendition.Rendition;
import dev.pavle.mediamonolith.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.Video;
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

  private void executeRenditionJob(CreateRenditionEvent event) {
    Rendition rendition = null;
    try {
      Video video =
          videoStoragePort
              .findById(event.videoId())
              .orElseThrow(() -> new VideoNotFoundException(event.videoId()));
      rendition =
          renditionStoragePort
              .findByVideoIdAndResolution(event.videoId(), event.resolution())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Rendition not found for video %s and resolution %s"
                              .formatted(event.videoId(), event.resolution())));
      rendition.setStatus(RenditionStatus.RUNNING);
      rendition = renditionStoragePort.save(rendition);
      log.info(
          "Rendition job RUNNING: videoId={}, resolution={}", event.videoId(), event.resolution());
      StoredFileRef persistedFile = transcodeAndPersist(video, rendition, event);
      rendition.setStoredFileRef(persistedFile);
      rendition.setStatus(RenditionStatus.FINISHED);
      renditionStoragePort.save(rendition);
      log.info(
          "Rendition job FINISHED: videoId={}, resolution={}",
          event.videoId(),
          event.resolution());

    } catch (Exception e) {
      handleJobError(rendition, event, e);
    } finally {
      activeRenditions.remove(event);
    }
  }

  private StoredFileRef transcodeAndPersist(
      Video video, Rendition rendition, CreateRenditionEvent event) {
    StoredFileRef source = new StoredFileRef(video.getSysPath());
    StoredFileRef tmpFile =
        videoProcessorPort.createRendition(source, event.resolution(), rendition.getName());
    return fileStoragePort.persist(tmpFile, rendition.getName());
  }

  private void handleJobError(Rendition rendition, CreateRenditionEvent event, Exception e) {
    log.error(
        "Rendition job ERROR: videoId={}, resolution={}, error={}",
        event.videoId(),
        event.resolution(),
        e.getMessage(),
        e);
    if (rendition != null) {
      rendition.setStatus(RenditionStatus.ERROR);
      rendition.setError(e.getMessage());
      try {
        renditionStoragePort.save(rendition);
      } catch (Exception saveEx) {
        log.error(
            "Failed to persist ERROR status for rendition: videoId={}, resolution={}",
            event.videoId(),
            event.resolution(),
            saveEx);
      }
    }
  }
}
