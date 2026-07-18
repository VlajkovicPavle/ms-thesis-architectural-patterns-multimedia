package dev.pavle.media.contracts;

public enum VideoResolution {
  SD_360(360),
  SD_480(480),
  HD_720(720),
  FHD_1080(1080);

  private final int height;

  VideoResolution(int height) {
    this.height = height;
  }

  public int getHeight() {
    return height;
  }

  public boolean isUpscaleOf(int sourceHeight) {
    return height >= sourceHeight;
  }
}
