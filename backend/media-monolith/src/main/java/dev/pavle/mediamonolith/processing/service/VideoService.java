package dev.pavle.mediamonolith.processing.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.processing.model.entity.Video;
import dev.pavle.mediamonolith.processing.model.vo.VideoMetadata;
import dev.pavle.mediamonolith.processing.repository.FileRepository;
import dev.pavle.mediamonolith.processing.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VideoService {

  private final FileRepository fileRepository;
  private final VideoRepository videoRepository;
  private final ProcessingService processingService;

  public VideoService(
      FileRepository fileRepository,
      VideoRepository videoRepository,
      ProcessingService processingService) {
    this.fileRepository = fileRepository;
    this.videoRepository = videoRepository;
    this.processingService = processingService;
  }

  public void upload(MultipartFile file) throws IOException {
    Path tmpPath = createTmpVideo(file);
    VideoMetadata metadata = processingService.extractMetadata(tmpPath);
    Video createdVideo = createVideo(tmpPath,file.getOriginalFilename(),metadata);
    videoRepository.save(createdVideo);
    log.info("Created video {}", createdVideo);
  }

  private Path createTmpVideo(MultipartFile file) throws IOException {
    String tmpFileName =
            Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> !name.isBlank())
                    .orElse(UUID.randomUUID().toString());
    return fileRepository.createTemp(file.getInputStream(), tmpFileName);
  }

  private Video createVideo(Path tmpVideoPath, String originalName, VideoMetadata metadata){
    Video video = new Video(originalName, metadata);
    Path savedSysPath = fileRepository.persistTempFile(tmpVideoPath, video.getSysName());
    video.setSysPath(savedSysPath.toString());
    return video;
  }
}
