package dev.pavle.mediamodular.notification.infrastructure.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.pavle.mediamodular.notification.application.NotificationService;
import dev.pavle.mediamodular.notification.infrastructure.sse.SseNotificationStreamAdapter;
import dev.pavle.mediamodular.notification.infrastructure.web.dto.NotificationResponse;

@RestController
@RequestMapping("v1/notification")
public class NotificationController {

  private final NotificationService notificationService;
  private final SseNotificationStreamAdapter streamAdapter;

  public NotificationController(
      NotificationService notificationService, SseNotificationStreamAdapter streamAdapter) {
    this.notificationService = notificationService;
    this.streamAdapter = streamAdapter;
  }

  @GetMapping
  public List<NotificationResponse> list() {
    return notificationService.list().stream().map(NotificationResponse::from).toList();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() {
    return streamAdapter.subscribe();
  }
}
