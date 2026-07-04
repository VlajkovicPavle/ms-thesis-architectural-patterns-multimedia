package dev.pavle.mediamodular.notification.infrastructure.sse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.pavle.mediamodular.notification.domain.model.Notification;
import dev.pavle.mediamodular.notification.domain.port.NotificationStreamPort;
import dev.pavle.mediamodular.notification.infrastructure.web.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SseNotificationStreamAdapter implements NotificationStreamPort {

  private static final String EVENT_NAME = "notification";

  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));
    emitters.add(emitter);
    log.info("SSE subscriber connected, active subscribers: {}", emitters.size());
    return emitter;
  }

  @Override
  public void push(Notification notification) {
    NotificationResponse payload = NotificationResponse.from(notification);
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(EVENT_NAME).data(payload));
      } catch (Exception e) {
        log.warn("Removing unreachable SSE subscriber", e);
        emitters.remove(emitter);
      }
    }
  }
}
