package dev.pavle.media.mediaservice.messaging;

import static dev.pavle.media.contracts.MessagingTopology.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AfterCommitPublisher {
  private final RabbitTemplate rabbitTemplate;

  public AfterCommitPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishCommands(RenditionCommandsCommitted event) {
    event
        .commands()
        .forEach(
            command ->
                rabbitTemplate.convertAndSend(COMMAND_EXCHANGE, COMMAND_ROUTING_KEY, command));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publishNotifications(TerminalNotificationsCommitted event) {
    event
        .notifications()
        .forEach(
            notification ->
                rabbitTemplate.convertAndSend(
                    NOTIFICATION_EXCHANGE, NOTIFICATION_ROUTING_KEY, notification));
  }
}
