package dev.pavle.mediamonolith.video.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.pavle.mediamonolith.video.application.RenditionService;
import dev.pavle.mediamonolith.video.application.model.view.RenditionDownload;
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

  @GetMapping("/{renditionId}/download")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID renditionId) {
    RenditionDownload download = renditionService.download(renditionId);
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(download.fileName()).build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new InputStreamResource(download.content()));
  }
}
