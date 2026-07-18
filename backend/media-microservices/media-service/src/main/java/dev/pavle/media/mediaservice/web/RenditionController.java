package dev.pavle.media.mediaservice.web;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.pavle.media.mediaservice.service.RenditionApplicationService;
import dev.pavle.media.mediaservice.service.RenditionApplicationService.RenditionDownload;
import dev.pavle.media.mediaservice.web.dto.CreateRenditionsRequest;
import dev.pavle.media.mediaservice.web.dto.RenditionResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("v1/rendition")
public class RenditionController {
  private final RenditionApplicationService renditionService;

  public RenditionController(RenditionApplicationService renditionService) {
    this.renditionService = renditionService;
  }

  @PostMapping
  public void create(@Valid @RequestBody CreateRenditionsRequest request) {
    renditionService.create(request.videoId(), request.resolutions());
  }

  @GetMapping("/video/{videoId}")
  public List<RenditionResponse> list(@PathVariable UUID videoId) {
    return renditionService.listForVideo(videoId).stream().map(RenditionResponse::from).toList();
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
