package dev.pavle.media.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import dev.pavle.media.contracts.NotificationType;
import dev.pavle.media.contracts.TerminalNotification;

class NotificationApplicationServiceTest {
  @Test
  void persistsCanonicalPayloadBeforeSchedulingBroadcast() {
    NotificationRepository repository = mock(NotificationRepository.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    SseNotificationStream stream = mock(SseNotificationStream.class);
    NotificationApplicationService service =
        new NotificationApplicationService(repository, publisher, stream);
    when(repository.save(any(Notification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    UUID videoId = UUID.randomUUID();
    UUID renditionId = UUID.randomUUID();
    TerminalNotification event =
        new TerminalNotification(
            videoId,
            renditionId,
            NotificationType.TASK_COMPLETED,
            "HD_720 rendition is ready",
            Instant.now());

    service.record(event);

    InOrder order = inOrder(repository, publisher);
    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    order.verify(repository).save(notificationCaptor.capture());
    order.verify(publisher).publishEvent(any(NotificationCommitted.class));
    assertThat(notificationCaptor.getValue().getVideoId()).isEqualTo(videoId);
    assertThat(notificationCaptor.getValue().getRenditionId()).isEqualTo(renditionId);
    assertThat(notificationCaptor.getValue().getMessage()).isEqualTo("HD_720 rendition is ready");
  }

  @Test
  void committedNotificationIsBroadcastAsSameEntity() {
    NotificationRepository repository = mock(NotificationRepository.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    SseNotificationStream stream = mock(SseNotificationStream.class);
    NotificationApplicationService service =
        new NotificationApplicationService(repository, publisher, stream);
    Notification notification =
        new Notification(
            new TerminalNotification(
                UUID.randomUUID(),
                null,
                NotificationType.ALL_COMPLETED,
                "All renditions are ready",
                Instant.now()));

    service.broadcast(new NotificationCommitted(notification));

    org.mockito.Mockito.verify(stream).push(notification);
  }
}
