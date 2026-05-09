package dev.pavle.mediamonolith.video.application.event;

import dev.pavle.mediamonolith.video.domain.port.RenditionJobEventBusPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
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
