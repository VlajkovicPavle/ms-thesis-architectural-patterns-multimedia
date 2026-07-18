package dev.pavle.media.notification;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.pavle.media.contracts.TerminalNotification;

@Service
public class NotificationApplicationService {
  private final NotificationRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final SseNotificationStream stream;

  public NotificationApplicationService(
      NotificationRepository repository,
      ApplicationEventPublisher eventPublisher,
      SseNotificationStream stream) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.stream = stream;
  }

  @Transactional
  public void record(TerminalNotification event) {
    Notification saved = repository.save(new Notification(event));
    eventPublisher.publishEvent(new NotificationCommitted(saved));
  }

  @Transactional(readOnly = true)
  public List<Notification> list() {
    return repository.findAllByOrderByCreatedAtDesc();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void broadcast(NotificationCommitted event) {
    stream.push(event.notification());
  }
}
