package dev.pavle.mediamonolith.video.infrastructure.processing;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.video.domain.model.VideoMetadata;
import dev.pavle.mediamonolith.video.domain.port.ProcessingPort;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class ProcessingAdapter implements ProcessingPort {

  private final ObjectMapper objectMapper;
  private final FfprobeParser ffprobeParser;

  public ProcessingAdapter(ObjectMapper objectMapper, FfprobeParser ffprobeParser) {
    this.objectMapper = objectMapper;
    this.ffprobeParser = ffprobeParser;
  }

  public VideoMetadata extractMetadata(Path filePath) {
    try {
      var process = ffprobeCommand(filePath).start();
      String metadataJson = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      log.info("Ffprobe finished: exitCode={}", exitCode);
      if (exitCode != 0) {
        throw new VideoProcessingException("ffprobe failed for file: " + filePath);
      }
      return ffprobeParser.parse(objectMapper.readTree(metadataJson));
    } catch (InterruptedException | IOException e) {
      throw new VideoProcessingException("ffprobe failed for file: " + filePath, e);
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
