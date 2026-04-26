package dev.pavle.mediamonolith.processing.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.processing.exceptions.VideoProcessingException;

@Service
public class ProcessingService {

  public void extractMetadata(Path tmpPath) {
    try {
      var process = ffprobeCommand(tmpPath).start();
      String metadataJson = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new VideoProcessingException("ffprobe failed for file: " + tmpPath);
      }
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
