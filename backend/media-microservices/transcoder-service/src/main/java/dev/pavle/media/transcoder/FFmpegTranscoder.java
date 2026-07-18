package dev.pavle.media.transcoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.pavle.media.contracts.RenditionCommand;

@Component
public class FFmpegTranscoder {
  private final Path storageRoot;

  public FFmpegTranscoder(@Value("${app.storage.local.path}") String storagePath)
      throws IOException {
    storageRoot = Path.of(storagePath);
    Files.createDirectories(storageRoot.resolve("tmp"));
    Files.createDirectories(storageRoot.resolve("storage"));
  }

  public String transcode(RenditionCommand command) {
    Path temporaryOutput = storageRoot.resolve("tmp").resolve(command.outputFileName());
    try {
      Process process = new ProcessBuilder(buildCommand(command, temporaryOutput)).start();
      String errorOutput =
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new TranscodingException(
            "ffmpeg transcoding failed for file: "
                + command.sourceIdentifier()
                + ", stderr: "
                + errorOutput);
      }
      Path persistedOutput = storageRoot.resolve("storage").resolve(command.outputFileName());
      return Files.move(temporaryOutput, persistedOutput, StandardCopyOption.REPLACE_EXISTING)
          .toString();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new TranscodingException(
          "ffmpeg transcoding failed for file: " + command.sourceIdentifier(), exception);
    } catch (IOException exception) {
      throw new TranscodingException(
          "ffmpeg transcoding failed for file: " + command.sourceIdentifier(), exception);
    }
  }

  List<String> buildCommand(RenditionCommand command, Path outputPath) {
    return List.of(
        "ffmpeg",
        "-i",
        command.sourceIdentifier(),
        "-vf",
        "scale=-2:" + command.resolution().getHeight(),
        "-c:v",
        "libx264",
        "-c:a",
        "aac",
        "-b:a",
        "128k",
        "-movflags",
        "+faststart",
        "-y",
        outputPath.toString());
  }
}
