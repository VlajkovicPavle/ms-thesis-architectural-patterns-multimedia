package dev.pavle.media.transcoder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.pavle.media.contracts.RenditionCommand;
import dev.pavle.media.contracts.VideoResolution;

class FFmpegTranscoderTest {
  @TempDir Path storage;

  @Test
  void preservesFfmpegArguments() throws Exception {
    FFmpegTranscoder transcoder = new FFmpegTranscoder(storage.toString());
    RenditionCommand command =
        new RenditionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            VideoResolution.HD_720,
            "/media/source.mp4",
            "output.mp4",
            Instant.now());

    assertThat(transcoder.buildCommand(command, Path.of("/media/output.mp4")))
        .containsExactly(
            "ffmpeg",
            "-i",
            "/media/source.mp4",
            "-vf",
            "scale=-2:720",
            "-c:v",
            "libx264",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            "-y",
            "/media/output.mp4");
  }
}
