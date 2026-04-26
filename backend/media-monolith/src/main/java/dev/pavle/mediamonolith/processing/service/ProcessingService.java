package dev.pavle.mediamonolith.processing.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import dev.pavle.mediamonolith.processing.exceptions.VideoProcessingException;
import dev.pavle.mediamonolith.processing.model.VideoProbeResult;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessingService {

  private final ObjectMapper objectMapper;

  public ProcessingService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public VideoProbeResult extractMetadata(Path tmpPath) {
    try {
      var process = ffprobeCommand(tmpPath).start();
      String metadataJson = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      log.info("Ffprobe finished: exitCode={}", exitCode);
      if (exitCode != 0) {
        throw new VideoProcessingException("ffprobe failed for file: " + tmpPath);
      }
      return VideoProbeResult.fromFfprobeJson(objectMapper.readTree(metadataJson));
    } catch (InterruptedException | IOException e) {
      throw new VideoProcessingException("ffprobe failed for file: " + tmpPath, e);
    }
  }

  private ProcessBuilder ffprobeCommand(Path file) {
    return new ProcessBuilder(
        "ffprobe",
        "-v",
        "quiet",
        "-print_format",
        "json",
        "-show_streams",
        "-show_format",
        file.toString());
  }
}
