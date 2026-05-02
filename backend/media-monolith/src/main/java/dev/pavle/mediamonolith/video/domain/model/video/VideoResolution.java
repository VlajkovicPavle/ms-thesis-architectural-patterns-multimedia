package dev.pavle.mediamonolith.video.domain.model.video;

import java.util.Arrays;
import java.util.Optional;

public enum VideoResolution {
  SD_360(360),
  SD_488(480),
  HD_720(720),
  FHD_1080(1080);

  private final int height;

  VideoResolution(int height) {
    this.height = height;
  }

  public boolean isUpscaleOf(int sourceHeight) {
    return this.height >= sourceHeight;
  }

  public Optional<VideoResolution> ofHeight(int height) {
    return Arrays.stream(VideoResolution.values()).filter(res -> res.height == height).findFirst();
  }
}
