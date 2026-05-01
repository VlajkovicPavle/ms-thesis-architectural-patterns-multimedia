package dev.pavle.mediamonolith.video.application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.port.FileStoragePort;
import dev.pavle.mediamonolith.video.domain.port.ProcessingPort;
import dev.pavle.mediamonolith.video.infrastructure.processing.ProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.video.domain.model.Video;
import dev.pavle.mediamonolith.video.domain.model.VideoMetadata;
import dev.pavle.mediamonolith.video.infrastructure.fileStorage.FileRepository;
import dev.pavle.mediamonolith.video.infrastructure.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VideoService {

  private final VideoRepository videoRepository;
  private final ProcessingPort processingPort;
  private final FileStoragePort fileStoragePort;

  public VideoService(
          VideoRepository videoRepository,
          ProcessingService processingService,
          FileRepository fileStoragePort) {
    this.videoRepository = videoRepository;
    this.processingPort = processingService;
      this.fileStoragePort = fileStoragePort;
  }

  public void upload(MultipartFile file) throws IOException {
    Path tmpPath = createTmpVideo(file);
    VideoMetadata metadata = processingPort.extractMetadata(tmpPath);
    Video createdVideo = createVideo(tmpPath,file.getOriginalFilename(),metadata);
    videoRepository.save(createdVideo);
    log.info("Created video {}", createdVideo);
  }

  private Path createTmpVideo(MultipartFile file) throws IOException {
    String tmpFileName =
            Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> !name.isBlank())
                    .orElse(UUID.randomUUID().toString());
    return fileStoragePort.createTemporary(file.getInputStream(), tmpFileName);
  }

  private Video createVideo(Path tmpVideoPath, String originalName, VideoMetadata metadata){
    Video video = new Video(originalName, metadata);
    Path savedSysPath = fileStoragePort.saveTemporary(tmpVideoPath, video.getSysName());
    video.setSysPath(savedSysPath.toString());
    return video;
  }
}
