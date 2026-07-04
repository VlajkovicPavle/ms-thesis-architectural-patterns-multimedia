package dev.pavle.mediamodular.video.infrastructure.processing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import dev.pavle.mediamodular.config.StorageProperties;
import dev.pavle.mediamodular.video.domain.model.shared.StoredFileRef;
import dev.pavle.mediamodular.video.domain.model.video.VideoResolution;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FFmpegTranscoderAdapter {

  private final StorageProperties storageProperties;

  public FFmpegTranscoderAdapter(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  public StoredFileRef transcode(
      StoredFileRef source, VideoResolution resolution, String outputFileName) {
    Path outputPath = Paths.get(storageProperties.path(), "tmp", outputFileName);
    try {
      var process =
          ffmpegCommand(source.identifier(), resolution.getHeight(), outputPath.toString()).start();
      String errorOutput = new String(process.getErrorStream().readAllBytes());
      int exitCode = process.waitFor();
      log.info("FFmpeg finished: exitCode={}, resolution={}", exitCode, resolution);
      if (exitCode != 0) {
        throw new VideoProcessingException(
            "ffmpeg transcoding failed for file: "
                + source.identifier()
                + ", stderr: "
                + errorOutput);
      }
      return new StoredFileRef(outputPath.toString());
    } catch (InterruptedException | IOException e) {
      throw new VideoProcessingException(
          "ffmpeg transcoding failed for file: " + source.identifier(), e);
    }
  }

  private ProcessBuilder ffmpegCommand(String inputPath, int targetHeight, String outputPath) {
    return new ProcessBuilder(
        "ffmpeg",
        "-i",
        inputPath,
        "-vf",
        "scale=-2:" + targetHeight,
        "-c:v",
        "libx264",
        "-c:a",
        "aac",
        "-b:a",
        "128k",
        "-movflags",
        "+faststart",
        "-y",
        outputPath);
  }
}
