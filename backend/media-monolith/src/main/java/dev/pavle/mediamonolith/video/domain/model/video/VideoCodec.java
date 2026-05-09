package dev.pavle.mediamonolith.video.domain.model.video;

import java.util.Arrays;

import dev.pavle.mediamonolith.video.infrastructure.processing.VideoProcessingException;

public enum VideoCodec {
  H264("h264");

  private final String ffProbeName;

  VideoCodec(String ffProbeName) {
    this.ffProbeName = ffProbeName;
  }

  public static VideoCodec fromFfmProbeName(String name) {
    return Arrays.stream(VideoCodec.values())
        .filter(codec -> codec.ffProbeName.equals(name))
        .findFirst()
        .orElseThrow(() -> new VideoProcessingException("Unsupported codec: " + name));
  }
}
