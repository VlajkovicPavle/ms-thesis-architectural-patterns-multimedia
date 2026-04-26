package dev.pavle.mediamonolith.processing.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.pavle.mediamonolith.processing.service.VideoService;

@RestController
@RequestMapping("/video")
public class VideoController {

  private final VideoService service;

  public VideoController(VideoService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public void create(@RequestParam("file") MultipartFile file)
      throws IOException, InterruptedException {
    service.upload(file);
  }
}
