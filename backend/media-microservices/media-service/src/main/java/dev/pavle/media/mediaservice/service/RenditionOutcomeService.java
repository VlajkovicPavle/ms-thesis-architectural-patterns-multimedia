package dev.pavle.media.mediaservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.pavle.media.contracts.NotificationType;
import dev.pavle.media.contracts.RenditionFailed;
import dev.pavle.media.contracts.RenditionRunning;
import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.contracts.RenditionSucceeded;
import dev.pavle.media.contracts.TerminalNotification;
import dev.pavle.media.mediaservice.exception.RenditionNotFoundException;
import dev.pavle.media.mediaservice.messaging.TerminalNotificationsCommitted;
import dev.pavle.media.mediaservice.model.Rendition;
import dev.pavle.media.mediaservice.persistence.RenditionRepository;

@Service
public class RenditionOutcomeService {
  private static final List<RenditionStatus> OUTSTANDING =
      List.of(RenditionStatus.PENDING, RenditionStatus.RUNNING);

  private final RenditionRepository renditionRepository;
  private final ApplicationEventPublisher eventPublisher;

  public RenditionOutcomeService(
      RenditionRepository renditionRepository, ApplicationEventPublisher eventPublisher) {
    this.renditionRepository = renditionRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void markRunning(RenditionRunning event) {
    Rendition rendition = get(event.renditionId());
    if (rendition.getStatus() == RenditionStatus.PENDING) {
      rendition.markRunning();
    }
  }

  @Transactional
  public void complete(RenditionSucceeded event) {
    Rendition rendition = get(event.renditionId());
    if (isTerminal(rendition)) {
      return;
    }
    rendition.markFinished(event.storedFileIdentifier());
    publishTerminalNotifications(
        rendition,
        NotificationType.TASK_COMPLETED,
        "%s rendition is ready".formatted(rendition.getResolution()));
  }

  @Transactional
  public void fail(RenditionFailed event) {
    Rendition rendition = get(event.renditionId());
    if (isTerminal(rendition)) {
      return;
    }
    rendition.markFailed(event.error());
    publishTerminalNotifications(
        rendition,
        NotificationType.TASK_FAILED,
        "%s rendition failed".formatted(rendition.getResolution()));
  }

  private Rendition get(java.util.UUID renditionId) {
    return renditionRepository
        .findById(renditionId)
        .orElseThrow(() -> new RenditionNotFoundException(renditionId));
  }

  private boolean isTerminal(Rendition rendition) {
    return rendition.getStatus() == RenditionStatus.FINISHED
        || rendition.getStatus() == RenditionStatus.ERROR
        || rendition.getStatus() == RenditionStatus.CANCELED;
  }

  private void publishTerminalNotifications(
      Rendition rendition, NotificationType type, String message) {
    renditionRepository.saveAndFlush(rendition);
    List<TerminalNotification> notifications = new ArrayList<>();
    notifications.add(
        new TerminalNotification(
            rendition.getVideo().getId(), rendition.getId(), type, message, Instant.now()));
    if (renditionRepository.countByVideoIdAndStatusIn(rendition.getVideo().getId(), OUTSTANDING)
        == 0) {
      notifications.add(
          new TerminalNotification(
              rendition.getVideo().getId(),
              null,
              NotificationType.ALL_COMPLETED,
              "All renditions are ready",
              Instant.now()));
    }
    eventPublisher.publishEvent(new TerminalNotificationsCommitted(List.copyOf(notifications)));
  }
}
