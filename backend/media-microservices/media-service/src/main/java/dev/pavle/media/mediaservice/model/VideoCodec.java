package dev.pavle.media.mediaservice.model;

import java.util.Arrays;

import dev.pavle.media.mediaservice.processing.VideoProcessingException;

public enum VideoCodec {
  H264("h264");

  private final String ffprobeName;

  VideoCodec(String ffprobeName) {
    this.ffprobeName = ffprobeName;
  }

  public static VideoCodec fromFfprobeName(String name) {
    return Arrays.stream(values())
        .filter(codec -> codec.ffprobeName.equals(name))
        .findFirst()
        .orElseThrow(() -> new VideoProcessingException("Unsupported codec: " + name));
  }
}
