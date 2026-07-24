package dev.pavle.media.mediaservice.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.pavle.media.contracts.RenditionStatus;
import dev.pavle.media.mediaservice.persistence.RenditionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class RenditionMetrics {
  private static final List<RenditionStatus> ACTIVE_STATUSES =
      List.of(RenditionStatus.PENDING, RenditionStatus.RUNNING);

  public RenditionMetrics(MeterRegistry meterRegistry, RenditionRepository renditionRepository) {
    Gauge.builder(
            "rendition.queue.size",
            renditionRepository,
            repository -> repository.countByStatus(RenditionStatus.PENDING))
        .register(meterRegistry);
    Gauge.builder(
            "rendition.active.jobs",
            renditionRepository,
            repository -> repository.countByStatusIn(ACTIVE_STATUSES))
        .register(meterRegistry);
  }
}
