package dev.pavle.mediamonolith.processing.service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.processing.repository.FileRepository;
import dev.pavle.mediamonolith.processing.repository.VideoRepository;

@Service
@Slf4j
public class VideoService {

  private final FileRepository fileRepository;
  private final ProcessingService processingService;

  public VideoService(
      VideoRepository videoRepository,
      FileRepository fileRepository,
      ProcessingService processingService) {
    this.fileRepository = fileRepository;
    this.processingService = processingService;
  }

  public void upload(MultipartFile file) throws IOException {
    String tmpFileName =
        Optional.ofNullable(file.getOriginalFilename())
            .filter(name -> !name.isBlank())
            .orElse(UUID.randomUUID().toString());
    var tmpPath = fileRepository.createTemp(file.getInputStream(), tmpFileName);
    var metadata = processingService.extractMetadata(tmpPath);
    log.info("Metadata: {}", metadata);
  }
}
