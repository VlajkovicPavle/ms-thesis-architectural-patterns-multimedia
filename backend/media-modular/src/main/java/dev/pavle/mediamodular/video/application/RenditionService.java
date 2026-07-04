package dev.pavle.mediamodular.video.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.pavle.mediamodular.video.application.model.view.RenditionDownload;
import dev.pavle.mediamodular.video.domain.exception.RenditionNotFinishedException;
import dev.pavle.mediamodular.video.domain.exception.RenditionNotFoundException;
import dev.pavle.mediamodular.video.domain.exception.VideoNotFoundException;
import dev.pavle.mediamodular.video.domain.model.rendition.Rendition;
import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.model.video.Video;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;
import dev.pavle.mediamodular.video.domain.port.FileStoragePort;
import dev.pavle.mediamodular.video.domain.port.RenditionStoragePort;
import dev.pavle.mediamodular.video.domain.port.VideoStoragePort;
import dev.pavle.mediamodular.video.infrastructure.web.dto.RenditionResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RenditionService {

  private final VideoStoragePort videoStoragePort;
  private final RenditionStoragePort renditionStoragePort;
  private final FileStoragePort fileStoragePort;

  public RenditionService(
      VideoStoragePort videoStoragePort,
      RenditionStoragePort renditionStoragePort,
      FileStoragePort fileStoragePort) {
    this.videoStoragePort = videoStoragePort;
    this.renditionStoragePort = renditionStoragePort;
    this.fileStoragePort = fileStoragePort;
  }

  public void createRendition(UUID videoId, Set<VideoResolution> resolutions) {
    Video video =
        videoStoragePort.findById(videoId).orElseThrow(() -> new VideoNotFoundException(videoId));
    var renditions = resolutions.stream().map((resolution -> new Rendition(video, resolution)));
    renditions.forEach(video::addRendition);
    videoStoragePort.save(video);
  }

  public List<RenditionResponse> getRenditionsForVideo(UUID videoId) {
    return renditionStoragePort.findAllByVideoId(videoId).stream()
        .map(RenditionResponse::from)
        .toList();
  }

  public RenditionDownload download(UUID renditionId) {
    Rendition rendition =
        renditionStoragePort
            .findById(renditionId)
            .orElseThrow(() -> new RenditionNotFoundException(renditionId));
    if (rendition.getStatus() != RenditionStatus.FINISHED || rendition.getStoredFileRef() == null) {
      throw new RenditionNotFinishedException(renditionId, rendition.getStatus());
    }
    return new RenditionDownload(
        rendition.getName(), fileStoragePort.open(rendition.getStoredFileRef()));
  }
}
