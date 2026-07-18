package dev.pavle.media.mediaservice.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import dev.pavle.media.mediaservice.model.VideoMetadata;
import tools.jackson.databind.ObjectMapper;

@Component
public class FFprobe {
  private final ObjectMapper objectMapper;
  private final FFprobeParser parser;

  public FFprobe(ObjectMapper objectMapper, FFprobeParser parser) {
    this.objectMapper = objectMapper;
    this.parser = parser;
  }

  public VideoMetadata extractMetadata(String identifier) {
    try {
      Process process =
          new ProcessBuilder(
                  "ffprobe",
                  "-v",
                  "quiet",
                  "-print_format",
                  "json",
                  "-show_streams",
                  "-show_format",
                  identifier)
              .start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new VideoProcessingException("ffprobe failed for file: " + identifier);
      }
      return parser.parse(objectMapper.readTree(output));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new VideoProcessingException("ffprobe failed for file: " + identifier, exception);
    } catch (IOException exception) {
      throw new VideoProcessingException("ffprobe failed for file: " + identifier, exception);
    }
  }
}
