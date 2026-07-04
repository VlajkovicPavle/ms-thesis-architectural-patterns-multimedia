package dev.pavle.mediamodular.video.application;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.pavle.mediamodular.video.application.model.view.VideoView;
import dev.pavle.mediamodular.video.domain.exception.VideoNotFoundException;
import dev.pavle.mediamodular.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamodular.video.domain.model.video.Video;
import dev.pavle.mediamodular.video.domain.model.video.VideoMetadata;
import dev.pavle.mediamodular.video.domain.port.FileStoragePort;
import dev.pavle.mediamodular.video.domain.port.VideoProcessorPort;
import dev.pavle.mediamodular.video.domain.port.VideoStoragePort;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VideoService {

  private final VideoStoragePort videoStoragePort;
  private final VideoProcessorPort videoProcessorPort;
  private final FileStoragePort fileStoragePort;

  public VideoService(
      VideoStoragePort videoStoragePort,
      VideoProcessorPort videoProcessorPort,
      FileStoragePort fileStoragePort) {
    this.videoStoragePort = videoStoragePort;
    this.videoProcessorPort = videoProcessorPort;
    this.fileStoragePort = fileStoragePort;
  }

  public VideoView upload(InputStream content, String originalName) {
    StoredFileRef tmpRef = createTmpVideo(content, originalName);
    VideoMetadata metadata = videoProcessorPort.extractMetadata(tmpRef);
    Video createdVideo = createVideo(tmpRef, originalName, metadata);
    Video saved = videoStoragePort.save(createdVideo);
    log.info("Created video {}", saved);
    return VideoView.from(saved);
  }

  public List<VideoView> list() {
    return videoStoragePort.findAllNewestFirst().stream().map(VideoView::from).toList();
  }

  public VideoView get(UUID videoId) {
    return videoStoragePort
        .findById(videoId)
        .map(VideoView::from)
        .orElseThrow(() -> new VideoNotFoundException(videoId));
  }

  private StoredFileRef createTmpVideo(InputStream content, String originalName) {
    String tmpFileName =
        Optional.ofNullable(originalName)
            .filter(name -> !name.isBlank())
            .orElse(UUID.randomUUID().toString());
    return fileStoragePort.createTemporary(content, tmpFileName);
  }

  private Video createVideo(StoredFileRef tmpRef, String originalName, VideoMetadata metadata) {
    Video video = new Video(originalName, metadata);
    StoredFileRef saved = fileStoragePort.persist(tmpRef, video.getSysName());
    video.setSysPath(saved.identifier());
    return video;
  }
}
