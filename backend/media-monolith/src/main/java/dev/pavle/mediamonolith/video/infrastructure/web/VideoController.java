package dev.pavle.mediamonolith.video.infrastructure.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.video.application.RenditionService;
import dev.pavle.mediamonolith.video.application.VideoService;
import dev.pavle.mediamonolith.video.application.model.view.VideoView;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.CreateVideoResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("v1/video")
@Slf4j
public class VideoController {

  private final VideoService videoService;

  public VideoController(VideoService service, RenditionService renditionService) {
    this.videoService = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CreateVideoResponse create(@RequestParam("file") MultipartFile file) throws IOException {
    log.info("File upload started: name={} size={}", file.getOriginalFilename(), file.getSize());
    VideoView view = videoService.upload(file.getInputStream(), file.getOriginalFilename());
    return CreateVideoResponse.from(view);
  }
}
