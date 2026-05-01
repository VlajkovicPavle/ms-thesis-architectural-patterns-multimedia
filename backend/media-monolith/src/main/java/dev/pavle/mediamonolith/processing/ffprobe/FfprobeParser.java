package dev.pavle.mediamonolith.processing.ffprobe;

import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.processing.exception.VideoProcessingException;
import dev.pavle.mediamonolith.processing.model.vo.VideoCodec;
import dev.pavle.mediamonolith.processing.model.vo.VideoContainerFormat;
import dev.pavle.mediamonolith.processing.model.vo.VideoMetadata;
import tools.jackson.databind.JsonNode;

@Component
public class FfprobeParser {

  public VideoMetadata parse(JsonNode root) {
    JsonNode videoStream =
        StreamSupport.stream(root.path("streams").spliterator(), false)
            .filter(s -> "video".equals(s.path("codec_type").asText()))
            .findFirst()
            .orElseThrow(() -> new VideoProcessingException("No video stream found"));
    JsonNode format = root.path("format");
    String formatName = format.path("format_name").asText().split(",")[0];
    return new VideoMetadata(
        VideoCodec.fromFfmProbeName(videoStream.path("codec_name").asText()),
        videoStream.path("width").asInt(),
        videoStream.path("height").asInt(),
        VideoContainerFormat.fromFfmProbeName(formatName),
        format.path("duration").asDouble(),
        format.path("size").asLong(),
        format.path("bit_rate").asLong());
  }
}
