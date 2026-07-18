package dev.pavle.media.mediaservice.messaging;

import static dev.pavle.media.contracts.MessagingTopology.EVENT_QUEUE;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import dev.pavle.media.contracts.RenditionFailed;
import dev.pavle.media.contracts.RenditionRunning;
import dev.pavle.media.contracts.RenditionSucceeded;
import dev.pavle.media.mediaservice.service.RenditionOutcomeService;

@Component
@RabbitListener(queues = EVENT_QUEUE)
public class RenditionEventConsumer {
  private final RenditionOutcomeService outcomeService;

  public RenditionEventConsumer(RenditionOutcomeService outcomeService) {
    this.outcomeService = outcomeService;
  }

  @RabbitHandler
  public void onRunning(RenditionRunning event) {
    outcomeService.markRunning(event);
  }

  @RabbitHandler
  public void onSucceeded(RenditionSucceeded event) {
    outcomeService.complete(event);
  }

  @RabbitHandler
  public void onFailed(RenditionFailed event) {
    outcomeService.fail(event);
  }
}
