package dev.pavle.media.mediaservice.service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.pavle.media.mediaservice.exception.VideoNotFoundException;
import dev.pavle.media.mediaservice.model.Video;
import dev.pavle.media.mediaservice.model.VideoMetadata;
import dev.pavle.media.mediaservice.persistence.VideoRepository;
import dev.pavle.media.mediaservice.processing.FFprobe;
import dev.pavle.media.mediaservice.storage.LocalFileStorage;

@Service
public class VideoApplicationService {
  private final VideoRepository videoRepository;
  private final FFprobe ffprobe;
  private final LocalFileStorage fileStorage;

  public VideoApplicationService(
      VideoRepository videoRepository, FFprobe ffprobe, LocalFileStorage fileStorage) {
    this.videoRepository = videoRepository;
    this.ffprobe = ffprobe;
    this.fileStorage = fileStorage;
  }

  @Transactional
  public Video upload(InputStream content, String originalName) {
    String temporaryName =
        Optional.ofNullable(originalName)
            .filter(name -> !name.isBlank())
            .orElse(UUID.randomUUID().toString());
    String temporaryIdentifier = fileStorage.createTemporary(content, temporaryName);
    VideoMetadata metadata = ffprobe.extractMetadata(temporaryIdentifier);
    Video video = new Video(originalName, metadata);
    video.setSysPath(fileStorage.persist(temporaryIdentifier, video.getSysName()));
    return videoRepository.save(video);
  }

  @Transactional(readOnly = true)
  public List<Video> list() {
    return videoRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public Video get(UUID videoId) {
    return videoRepository.findById(videoId).orElseThrow(() -> new VideoNotFoundException(videoId));
  }
}
