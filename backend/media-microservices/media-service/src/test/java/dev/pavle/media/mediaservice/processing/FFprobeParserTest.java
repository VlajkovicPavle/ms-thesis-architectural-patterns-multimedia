package dev.pavle.media.mediaservice.processing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.pavle.media.mediaservice.model.VideoCodec;
import dev.pavle.media.mediaservice.model.VideoContainerFormat;
import tools.jackson.databind.ObjectMapper;

class FFprobeParserTest {
  @Test
  void parsesTheFirstVideoStreamAndFormatMetadata() {
    String json =
        """
        {
          "streams": [
            {"codec_type":"audio","codec_name":"aac"},
            {"codec_type":"video","codec_name":"h264","width":1280,"height":720}
          ],
          "format": {
            "format_name":"mov,mp4,m4a,3gp,3g2,mj2",
            "duration":"12.5",
            "size":"12345",
            "bit_rate":"98765"
          }
        }
        """;

    var metadata = new FFprobeParser().parse(new ObjectMapper().readTree(json));

    assertThat(metadata.getVideoCodec()).isEqualTo(VideoCodec.H264);
    assertThat(metadata.getVideoContainerFormat()).isEqualTo(VideoContainerFormat.MP4);
    assertThat(metadata.getWidth()).isEqualTo(1280);
    assertThat(metadata.getHeight()).isEqualTo(720);
    assertThat(metadata.getDuration()).isEqualTo(12.5);
    assertThat(metadata.getSize()).isEqualTo(12345);
    assertThat(metadata.getBitRate()).isEqualTo(98765);
  }
}
