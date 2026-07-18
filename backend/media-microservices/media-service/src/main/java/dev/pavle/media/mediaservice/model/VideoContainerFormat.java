package dev.pavle.media.mediaservice.model;

import java.util.Arrays;

import dev.pavle.media.mediaservice.processing.VideoProcessingException;

public enum VideoContainerFormat {
  MP4("mov", "mp4");

  private final String ffprobeName;
  private final String extension;

  VideoContainerFormat(String ffprobeName, String extension) {
    this.ffprobeName = ffprobeName;
    this.extension = extension;
  }

  public static VideoContainerFormat fromFfprobeName(String name) {
    return Arrays.stream(values())
        .filter(format -> format.ffprobeName.equals(name))
        .findFirst()
        .orElseThrow(() -> new VideoProcessingException("Unsupported format: " + name));
  }

  public String getExtension() {
    return extension;
  }
}
