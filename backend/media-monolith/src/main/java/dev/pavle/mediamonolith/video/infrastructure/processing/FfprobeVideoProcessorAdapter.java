package dev.pavle.mediamonolith.video.infrastructure.processing;

import java.io.IOException;

import org.hibernate.engine.spi.Resolution;
import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamonolith.video.domain.model.video.VideoMetadata;
import dev.pavle.mediamonolith.video.domain.port.VideoProcessorPort;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class FfprobeVideoProcessorAdapter implements VideoProcessorPort {

  private final ObjectMapper objectMapper;
  private final FfprobeParser ffprobeParser;

  public FfprobeVideoProcessorAdapter(ObjectMapper objectMapper, FfprobeParser ffprobeParser) {
    this.objectMapper = objectMapper;
    this.ffprobeParser = ffprobeParser;
  }

  @Override
  public VideoMetadata extractMetadata(StoredFileRef ref) {
    try {
      var process = ffprobeCommand(ref.identifier()).start();
      String metadataJson = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      log.info("Ffprobe finished: exitCode={}", exitCode);
      if (exitCode != 0) {
        throw new VideoProcessingException("ffprobe failed for file: " + ref.identifier());
      }
      return ffprobeParser.parse(objectMapper.readTree(metadataJson));
    } catch (InterruptedException | IOException e) {
      throw new VideoProcessingException("ffprobe failed for file: " + ref.identifier(), e);
    }
  }

  @Override
  public StoredFileRef createRendition(StoredFileRef parentVideo, Resolution resolution) {
    return null;
  }

  private ProcessBuilder ffprobeCommand(String filePath) {
    return new ProcessBuilder(
        "ffprobe",
        "-v",
        "quiet",
        "-print_format",
        "json",
        "-show_streams",
        "-show_format",
        filePath);
  }
}
