package dev.pavle.media.notification;

import static dev.pavle.media.contracts.MessagingTopology.NOTIFICATION_QUEUE;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import dev.pavle.media.contracts.TerminalNotification;

@Component
public class TerminalNotificationConsumer {
  private final NotificationApplicationService service;

  public TerminalNotificationConsumer(NotificationApplicationService service) {
    this.service = service;
  }

  @RabbitListener(queues = NOTIFICATION_QUEUE)
  public void consume(TerminalNotification event) {
    service.record(event);
  }
}
