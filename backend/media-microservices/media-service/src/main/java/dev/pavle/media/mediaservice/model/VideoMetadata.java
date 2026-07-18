package dev.pavle.media.mediaservice.model;

import dev.pavle.media.mediaservice.processing.VideoProcessingException;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class VideoMetadata {
  private static final int MIN_WIDTH = 144;
  private static final int MAX_WIDTH = 1920;
  private static final int MIN_HEIGHT = 144;
  private static final int MAX_HEIGHT = 1080;

  @Enumerated(EnumType.STRING)
  private VideoCodec videoCodec;

  private int width;
  private int height;

  @Enumerated(EnumType.STRING)
  private VideoContainerFormat videoContainerFormat;

  private double duration;
  private long size;
  private long bitRate;

  protected VideoMetadata() {}

  public VideoMetadata(
      VideoCodec videoCodec,
      int width,
      int height,
      VideoContainerFormat videoContainerFormat,
      double duration,
      long size,
      long bitRate) {
    if (width < MIN_WIDTH || width > MAX_WIDTH) {
      throw new VideoProcessingException(
          "Video width %d out of bounds [%d, %d]".formatted(width, MIN_WIDTH, MAX_WIDTH));
    }
    if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
      throw new VideoProcessingException(
          "Video height %d out of bounds [%d, %d]".formatted(height, MIN_HEIGHT, MAX_HEIGHT));
    }
    this.videoCodec = videoCodec;
    this.width = width;
    this.height = height;
    this.videoContainerFormat = videoContainerFormat;
    this.duration = duration;
    this.size = size;
    this.bitRate = bitRate;
  }

  public VideoCodec getVideoCodec() {
    return videoCodec;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public VideoContainerFormat getVideoContainerFormat() {
    return videoContainerFormat;
  }

  public double getDuration() {
    return duration;
  }

  public long getSize() {
    return size;
  }

  public long getBitRate() {
    return bitRate;
  }
}
