package dev.pavle.media.mediaservice.processing;

import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import dev.pavle.media.mediaservice.model.VideoCodec;
import dev.pavle.media.mediaservice.model.VideoContainerFormat;
import dev.pavle.media.mediaservice.model.VideoMetadata;
import tools.jackson.databind.JsonNode;

@Component
public class FFprobeParser {
  public VideoMetadata parse(JsonNode root) {
    JsonNode videoStream =
        StreamSupport.stream(root.path("streams").spliterator(), false)
            .filter(stream -> "video".equals(stream.path("codec_type").asText()))
            .findFirst()
            .orElseThrow(() -> new VideoProcessingException("No video stream found"));
    JsonNode format = root.path("format");
    String formatName = format.path("format_name").asText().split(",")[0];
    return new VideoMetadata(
        VideoCodec.fromFfprobeName(videoStream.path("codec_name").asText()),
        videoStream.path("width").asInt(),
        videoStream.path("height").asInt(),
        VideoContainerFormat.fromFfprobeName(formatName),
        format.path("duration").asDouble(),
        format.path("size").asLong(),
        format.path("bit_rate").asLong());
  }
}
