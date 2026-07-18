package dev.pavle.media.mediaservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import dev.pavle.media.contracts.NotificationType;
import dev.pavle.media.contracts.RenditionCommand;
import dev.pavle.media.contracts.RenditionFailed;
import dev.pavle.media.contracts.RenditionRunning;
import dev.pavle.media.contracts.RenditionSucceeded;
import dev.pavle.media.contracts.TerminalNotification;
import dev.pavle.media.contracts.VideoResolution;

class MessagingContractTest {
  @Test
  void everyMessageRoundTripsThroughTheRabbitJsonConverter() {
    UUID renditionId = UUID.randomUUID();
    UUID videoId = UUID.randomUUID();
    Instant timestamp = Instant.parse("2026-07-17T12:00:00Z");
    List<Object> messages =
        List.of(
            new RenditionCommand(
                renditionId,
                videoId,
                VideoResolution.HD_720,
                "/media/source.mp4",
                "output.mp4",
                timestamp),
            new RenditionRunning(renditionId, videoId, timestamp),
            new RenditionSucceeded(renditionId, videoId, "/media/output.mp4", timestamp),
            new RenditionFailed(renditionId, videoId, "processing failed", timestamp),
            new TerminalNotification(
                videoId,
                renditionId,
                NotificationType.TASK_COMPLETED,
                "HD_720 rendition is ready",
                timestamp));
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

    for (Object message : messages) {
      Object roundTrip =
          converter.fromMessage(converter.toMessage(message, new MessageProperties()));
      assertThat(roundTrip).isEqualTo(message);
    }
  }
}
