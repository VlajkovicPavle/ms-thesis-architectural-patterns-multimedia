package dev.pavle.media.transcoder;

import static dev.pavle.media.contracts.MessagingTopology.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import dev.pavle.media.contracts.RenditionCommand;
import dev.pavle.media.contracts.RenditionFailed;
import dev.pavle.media.contracts.RenditionRunning;
import dev.pavle.media.contracts.RenditionSucceeded;
import dev.pavle.media.contracts.VideoResolution;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RenditionCommandConsumerTest {
  @Test
  void publishesRunningBeforeProcessingAndSuccessAfterward() {
    FFmpegTranscoder transcoder = mock(FFmpegTranscoder.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    RenditionCommandConsumer consumer =
        new RenditionCommandConsumer(transcoder, rabbitTemplate, new SimpleMeterRegistry());
    RenditionCommand command =
        new RenditionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            VideoResolution.SD_360,
            "/media/source.mp4",
            "output.mp4",
            Instant.now());
    when(transcoder.transcode(command)).thenReturn("/media/output.mp4");

    consumer.consume(command);

    InOrder order = inOrder(rabbitTemplate, transcoder);
    order
        .verify(rabbitTemplate)
        .convertAndSend(
            org.mockito.ArgumentMatchers.eq(EVENT_EXCHANGE),
            org.mockito.ArgumentMatchers.eq(RUNNING_ROUTING_KEY),
            any(RenditionRunning.class));
    order.verify(transcoder).transcode(command);
    order
        .verify(rabbitTemplate)
        .convertAndSend(
            org.mockito.ArgumentMatchers.eq(EVENT_EXCHANGE),
            org.mockito.ArgumentMatchers.eq(SUCCEEDED_ROUTING_KEY),
            any(RenditionSucceeded.class));
  }

  @Test
  void successfulTranscodeDoesNotBecomeFailedWhenSuccessPublicationFails() {
    FFmpegTranscoder transcoder = mock(FFmpegTranscoder.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    RenditionCommandConsumer consumer =
        new RenditionCommandConsumer(
            transcoder, rabbitTemplate, mock(Timer.class), mock(Timer.class));
    RenditionCommand command = command();
    when(transcoder.transcode(command)).thenReturn("/media/output.mp4");
    AmqpException publicationFailure = new AmqpException("broker unavailable");
    doThrow(publicationFailure)
        .when(rabbitTemplate)
        .convertAndSend(
            eq(EVENT_EXCHANGE), eq(SUCCEEDED_ROUTING_KEY), any(RenditionSucceeded.class));

    assertThatThrownBy(() -> consumer.consume(command)).isSameAs(publicationFailure);

    verify(rabbitTemplate, never())
        .convertAndSend(eq(EVENT_EXCHANGE), eq(FAILED_ROUTING_KEY), any(RenditionFailed.class));
  }

  @Test
  void successfulTranscodeDoesNotBecomeFailedWhenSuccessMetricFails() {
    FFmpegTranscoder transcoder = mock(FFmpegTranscoder.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    Timer successTimer = mock(Timer.class);
    RenditionCommandConsumer consumer =
        new RenditionCommandConsumer(transcoder, rabbitTemplate, successTimer, mock(Timer.class));
    RenditionCommand command = command();
    when(transcoder.transcode(command)).thenReturn("/media/output.mp4");
    RuntimeException metricFailure = new RuntimeException("metrics unavailable");
    doThrow(metricFailure).when(successTimer).record(any(Duration.class));

    assertThatThrownBy(() -> consumer.consume(command)).isSameAs(metricFailure);

    verify(rabbitTemplate, never())
        .convertAndSend(eq(EVENT_EXCHANGE), eq(FAILED_ROUTING_KEY), any(RenditionFailed.class));
  }

  @Test
  void failedOutcomePublicationEscapesForBrokerRedelivery() {
    FFmpegTranscoder transcoder = mock(FFmpegTranscoder.class);
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    RenditionCommandConsumer consumer =
        new RenditionCommandConsumer(
            transcoder, rabbitTemplate, mock(Timer.class), mock(Timer.class));
    RenditionCommand command = command();
    when(transcoder.transcode(command)).thenThrow(new TranscodingException("ffmpeg failed"));
    AmqpException publicationFailure = new AmqpException("broker unavailable");
    doThrow(publicationFailure)
        .when(rabbitTemplate)
        .convertAndSend(eq(EVENT_EXCHANGE), eq(FAILED_ROUTING_KEY), any(RenditionFailed.class));

    assertThatThrownBy(() -> consumer.consume(command)).isSameAs(publicationFailure);
  }

  private RenditionCommand command() {
    return new RenditionCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        VideoResolution.SD_360,
        "/media/source.mp4",
        "output.mp4",
        Instant.now());
  }
}
