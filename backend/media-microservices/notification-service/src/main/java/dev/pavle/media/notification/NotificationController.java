package dev.pavle.media.notification;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("v1/notification")
public class NotificationController {
  private final NotificationApplicationService service;
  private final SseNotificationStream stream;

  public NotificationController(
      NotificationApplicationService service, SseNotificationStream stream) {
    this.service = service;
    this.stream = stream;
  }

  @GetMapping
  public List<NotificationResponse> list() {
    return service.list().stream().map(NotificationResponse::from).toList();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() {
    return stream.subscribe();
  }
}
