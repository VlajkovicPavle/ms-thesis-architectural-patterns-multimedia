package dev.pavle.mediamonolith.video.domain.model;

import java.util.Arrays;

import dev.pavle.mediamonolith.video.infrastructure.processing.VideoProcessingException;
import lombok.Getter;

@Getter
public enum VideoContainerFormat {
  MP4("mov", "mp4");

  private final String ffProbeName;
  private final String extension;

  VideoContainerFormat(String ffProbeName, String extension) {
    this.ffProbeName = ffProbeName;
    this.extension = extension;
  }

  public static VideoContainerFormat fromFfmProbeName(String name) {
    return Arrays.stream(VideoContainerFormat.values())
        .filter(format -> format.ffProbeName.equals(name))
        .findFirst()
        .orElseThrow(() -> new VideoProcessingException("Unsupported format: " + name));
  }
}
