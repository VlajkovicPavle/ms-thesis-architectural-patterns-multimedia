package dev.pavle.media.mediaservice.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.pavle.media.contracts.RenditionCommand;
import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.VideoResolution;
import dev.pavle.media.mediaservice.exception.DuplicateRenditionException;
import dev.pavle.media.mediaservice.exception.InvalidRenditionResolutionException;
import dev.pavle.media.mediaservice.exception.RenditionNotFinishedException;
import dev.pavle.media.mediaservice.exception.RenditionNotFoundException;
import dev.pavle.media.mediaservice.exception.VideoNotFoundException;
import dev.pavle.media.mediaservice.messaging.RenditionCommandsCommitted;
import dev.pavle.media.mediaservice.model.Rendition;
import dev.pavle.media.mediaservice.model.Video;
import dev.pavle.media.mediaservice.persistence.RenditionRepository;
import dev.pavle.media.mediaservice.persistence.VideoRepository;
import dev.pavle.media.mediaservice.storage.LocalFileStorage;

@Service
public class RenditionApplicationService {
  private final VideoRepository videoRepository;
  private final RenditionRepository renditionRepository;
  private final LocalFileStorage fileStorage;
  private final ApplicationEventPublisher eventPublisher;

  public RenditionApplicationService(
      VideoRepository videoRepository,
      RenditionRepository renditionRepository,
      LocalFileStorage fileStorage,
      ApplicationEventPublisher eventPublisher) {
    this.videoRepository = videoRepository;
    this.renditionRepository = renditionRepository;
    this.fileStorage = fileStorage;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void create(UUID videoId, Set<VideoResolution> resolutions) {
    Video video =
        videoRepository
            .findByIdForUpdate(videoId)
            .orElseThrow(() -> new VideoNotFoundException(videoId));
    validateRequest(video, resolutions);

    List<RenditionCommand> commands = new ArrayList<>();
    for (VideoResolution resolution : resolutions) {
      Rendition rendition =
          renditionRepository
              .findByVideoIdAndResolution(videoId, resolution)
              .map(
                  existing -> {
                    existing.retry();
                    return existing;
                  })
              .orElseGet(
                  () -> {
                    Rendition created = new Rendition(video, resolution);
                    video.addRendition(created);
                    return created;
                  });
      commands.add(
          new RenditionCommand(
              rendition.getId(),
              videoId,
              resolution,
              video.getSysPath(),
              rendition.getName(),
              Instant.now()));
    }
    videoRepository.save(video);
    eventPublisher.publishEvent(new RenditionCommandsCommitted(List.copyOf(commands)));
  }

  @Transactional(readOnly = true)
  public List<Rendition> listForVideo(UUID videoId) {
    return renditionRepository.findAllByVideoId(videoId);
  }

  @Transactional(readOnly = true)
  public RenditionDownload download(UUID renditionId) {
    Rendition rendition =
        renditionRepository
            .findById(renditionId)
            .orElseThrow(() -> new RenditionNotFoundException(renditionId));
    if (rendition.getStatus() != RenditionStatus.FINISHED
        || rendition.getStoredFileIdentifier() == null) {
      throw new RenditionNotFinishedException(renditionId, rendition.getStatus());
    }
    return new RenditionDownload(
        rendition.getName(), fileStorage.open(rendition.getStoredFileIdentifier()));
  }

  private void validateRequest(Video video, Set<VideoResolution> resolutions) {
    for (VideoResolution resolution : resolutions) {
      if (resolution.isUpscaleOf(video.getMetadata().getHeight())) {
        throw new InvalidRenditionResolutionException(
            resolution, video.getSysName(), video.getMetadata().getHeight());
      }
      renditionRepository
          .findByVideoIdAndResolution(video.getId(), resolution)
          .filter(
              existing ->
                  existing.getStatus() != RenditionStatus.CANCELED
                      && existing.getStatus() != RenditionStatus.ERROR)
          .ifPresent(
              existing -> {
                throw new DuplicateRenditionException(resolution, video.getSysName());
              });
    }
  }

  public record RenditionDownload(String fileName, InputStream content) {}
}
