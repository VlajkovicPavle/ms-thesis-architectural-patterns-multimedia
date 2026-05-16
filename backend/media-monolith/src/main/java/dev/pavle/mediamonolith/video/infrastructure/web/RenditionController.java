package dev.pavle.mediamonolith.video.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import dev.pavle.mediamonolith.video.application.RenditionService;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.CreateRenditionsRequest;
import dev.pavle.mediamonolith.video.infrastructure.web.dto.RenditionResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("v1/rendition")
@Slf4j
public class RenditionController {

  private final RenditionService renditionService;

  public RenditionController(RenditionService renditionService) {
    this.renditionService = renditionService;
  }

  @PostMapping()
  public void createRendition(@Valid @RequestBody CreateRenditionsRequest createRenditionsRequest) {
    log.info("Rendition request received, {}", createRenditionsRequest);
    renditionService.createRendition(
        createRenditionsRequest.videoId(), createRenditionsRequest.resolutions());
  }

  @GetMapping("/video/{videoId}")
  public List<RenditionResponse> getRenditionsForVideo(@PathVariable UUID videoId) {
    return renditionService.getRenditionsForVideo(videoId);
  }
}
