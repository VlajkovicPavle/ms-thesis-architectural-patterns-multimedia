package dev.pavle.mediamonolith.processing.model;

import java.util.stream.StreamSupport;

import org.jspecify.annotations.NonNull;

import tools.jackson.databind.JsonNode;

import dev.pavle.mediamonolith.processing.exceptions.VideoProcessingException;

public record VideoProbeResult(
    VideoCodec videoCodec,
    int width,
    int height,
    VideoContainerFormat videoContainerFormat,
    double duration,
    long size,
    long bitRate) {

  public VideoProbeResult {
    if (width < VideoConstraints.MIN_VIDEO_WIDTH || width > VideoConstraints.MAX_VIDEO_WIDTH) {
      throw new VideoProcessingException(
          "Video width %d out of bounds [%d, %d]"
              .formatted(
                  width, VideoConstraints.MIN_VIDEO_WIDTH, VideoConstraints.MAX_VIDEO_WIDTH));
    }
    if (height < VideoConstraints.MIN_VIDEO_HEIGHT || height > VideoConstraints.MAX_VIDEO_HEIGHT) {
      throw new VideoProcessingException(
          "Video height %d out of bounds [%d, %d]"
              .formatted(
                  height, VideoConstraints.MIN_VIDEO_HEIGHT, VideoConstraints.MAX_VIDEO_HEIGHT));
    }
  }

  public static VideoProbeResult fromFfprobeJson(JsonNode root) {
    return getVideoProbeResult(root);
  }

  @NonNull
  public static VideoProbeResult getVideoProbeResult(JsonNode root) {
    JsonNode videoStream =
        StreamSupport.stream(root.path("streams").spliterator(), false)
            .filter(s -> "video".equals(s.path("codec_type").asText()))
            .findFirst()
            .orElseThrow(() -> new VideoProcessingException("No video stream found"));
    JsonNode format = root.path("format");
    String formatName = format.path("format_name").asText().split(",")[0];
    return new VideoProbeResult(
        VideoCodec.fromFfmProbeName(videoStream.path("codec_name").asText()),
        videoStream.path("width").asInt(),
        videoStream.path("height").asInt(),
        VideoContainerFormat.fromFfmProbeName(formatName),
        format.path("duration").asDouble(),
        format.path("size").asLong(),
        format.path("bit_rate").asLong());
  }
}
