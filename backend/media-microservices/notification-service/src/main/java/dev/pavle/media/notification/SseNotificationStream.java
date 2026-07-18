package dev.pavle.media.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseNotificationStream {
  private static final String EVENT_NAME = "notification";
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(exception -> emitters.remove(emitter));
    emitters.add(emitter);
    return emitter;
  }

  public void push(Notification notification) {
    NotificationResponse response = NotificationResponse.from(notification);
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(EVENT_NAME).data(response));
      } catch (Exception exception) {
        emitters.remove(emitter);
      }
    }
  }
}
