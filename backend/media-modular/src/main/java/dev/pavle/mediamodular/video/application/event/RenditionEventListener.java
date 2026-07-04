package dev.pavle.mediamodular.video.application.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.pavle.mediamodular.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamodular.video.domain.port.RenditionJobEventBusPort;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RenditionEventListener {

  private final RenditionJobEventBusPort renditionJobEventBusPort;

  public RenditionEventListener(RenditionJobEventBusPort renditionJobEventBusPort) {
    this.renditionJobEventBusPort = renditionJobEventBusPort;
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  private void onCreate(CreateRenditionEvent event) {
    log.info("Create rendition job dispatched: {}", event);
    this.renditionJobEventBusPort.publishCreateRenditionJob(event);
  }
}
