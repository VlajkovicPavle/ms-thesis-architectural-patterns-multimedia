package dev.pavle.media.transcoder;

import static dev.pavle.media.contracts.MessagingTopology.*;

import java.time.Duration;
import java.time.Instant;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.pavle.media.contracts.RenditionCommand;
import dev.pavle.media.contracts.RenditionFailed;
import dev.pavle.media.contracts.RenditionRunning;
import dev.pavle.media.contracts.RenditionSucceeded;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class RenditionCommandConsumer {
  private final FFmpegTranscoder transcoder;
  private final RabbitTemplate rabbitTemplate;
  private final Timer successTimer;
  private final Timer errorTimer;

  @Autowired
  public RenditionCommandConsumer(
      FFmpegTranscoder transcoder, RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
    this(
        transcoder,
        rabbitTemplate,
        pipelineTimer(meterRegistry, "success"),
        pipelineTimer(meterRegistry, "error"));
  }

  RenditionCommandConsumer(
      FFmpegTranscoder transcoder,
      RabbitTemplate rabbitTemplate,
      Timer successTimer,
      Timer errorTimer) {
    this.transcoder = transcoder;
    this.rabbitTemplate = rabbitTemplate;
    this.successTimer = successTimer;
    this.errorTimer = errorTimer;
  }

  @RabbitListener(queues = COMMAND_QUEUE)
  public void consume(RenditionCommand command) {
    rabbitTemplate.convertAndSend(
        EVENT_EXCHANGE,
        RUNNING_ROUTING_KEY,
        new RenditionRunning(command.renditionId(), command.videoId(), Instant.now()));
    String outputIdentifier;
    try {
      outputIdentifier = transcoder.transcode(command);
    } catch (Exception exception) {
      rabbitTemplate.convertAndSend(
          EVENT_EXCHANGE,
          FAILED_ROUTING_KEY,
          new RenditionFailed(
              command.renditionId(), command.videoId(), exception.getMessage(), Instant.now()));
      errorTimer.record(Duration.between(command.createdAt(), Instant.now()));
      return;
    }
    rabbitTemplate.convertAndSend(
        EVENT_EXCHANGE,
        SUCCEEDED_ROUTING_KEY,
        new RenditionSucceeded(
            command.renditionId(), command.videoId(), outputIdentifier, Instant.now()));
    successTimer.record(Duration.between(command.createdAt(), Instant.now()));
  }

  private static Timer pipelineTimer(MeterRegistry meterRegistry, String status) {
    return Timer.builder("rendition.pipeline.duration")
        .tag("status", status)
        .publishPercentileHistogram()
        .register(meterRegistry);
  }
}
