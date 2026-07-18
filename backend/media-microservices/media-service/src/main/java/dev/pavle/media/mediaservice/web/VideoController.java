package dev.pavle.media.mediaservice.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.media.mediaservice.model.Video;
import dev.pavle.media.mediaservice.service.RenditionApplicationService;
import dev.pavle.media.mediaservice.service.VideoApplicationService;
import dev.pavle.media.mediaservice.web.dto.RenditionResponse;
import dev.pavle.media.mediaservice.web.dto.VideoDetailsResponse;
import dev.pavle.media.mediaservice.web.dto.VideoResponse;

@RestController
@RequestMapping("v1/video")
public class VideoController {
  private final VideoApplicationService videoService;
  private final RenditionApplicationService renditionService;

  public VideoController(
      VideoApplicationService videoService, RenditionApplicationService renditionService) {
    this.videoService = videoService;
    this.renditionService = renditionService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public VideoResponse create(@RequestParam("file") MultipartFile file) throws IOException {
    return VideoResponse.from(
        videoService.upload(file.getInputStream(), file.getOriginalFilename()));
  }

  @GetMapping
  public List<VideoResponse> list() {
    return videoService.list().stream().map(VideoResponse::from).toList();
  }

  @GetMapping("/{videoId}")
  public VideoDetailsResponse get(@PathVariable UUID videoId) {
    Video video = videoService.get(videoId);
    List<RenditionResponse> renditions =
        renditionService.listForVideo(videoId).stream().map(RenditionResponse::from).toList();
    return VideoDetailsResponse.from(video, renditions);
  }
}
