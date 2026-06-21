package dev.pavle.mediamonolith.video.infrastructure.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.video.application.RenditionService;
import dev.pavle.mediamonolith.video.application.VideoService;
import dev.pavle.mediamonolith.video.application.model.view.VideoView;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.CreateVideoResponse;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.VideoDetailsResponse;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.VideoResponse;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("v1/video")
@Slf4j
public class VideoController {

  private final VideoService videoService;
  private final RenditionService renditionService;

  public VideoController(VideoService service, RenditionService renditionService) {
    this.videoService = service;
    this.renditionService = renditionService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CreateVideoResponse create(@RequestParam("file") MultipartFile file) throws IOException {
    log.info("File upload started: name={} size={}", file.getOriginalFilename(), file.getSize());
    VideoView view = videoService.upload(file.getInputStream(), file.getOriginalFilename());
    return CreateVideoResponse.from(view);
  }

  @GetMapping
  public List<VideoResponse> list() {
    return videoService.list().stream().map(VideoResponse::from).toList();
  }

  @GetMapping("/{videoId}")
  public VideoDetailsResponse get(@PathVariable UUID videoId) {
    VideoView video = videoService.get(videoId);
    return VideoDetailsResponse.from(video, renditionService.getRenditionsForVideo(videoId));
  }
}
