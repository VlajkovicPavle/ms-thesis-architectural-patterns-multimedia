package dev.pavle.mediamodular.video.application;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import dev.pavle.mediamodular.video.domain.model.rendition.RenditionStatus;
import dev.pavle.mediamodular.video.domain.port.RenditionStoragePort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class RenditionMetrics {

  private final MeterRegistry meterRegistry;
  private final RenditionStoragePort renditionStoragePort;
  private Timer pipelineSuccessTimer;
  private Timer pipelineErrorTimer;

  public RenditionMetrics(MeterRegistry meterRegistry, RenditionStoragePort renditionStoragePort) {
    this.meterRegistry = meterRegistry;
    this.renditionStoragePort = renditionStoragePort;
    buildGuageMetrics();
    buildTimerMetrics();
  }

  public void registerActiveJobsGauge(Supplier<Number> activeJobCount) {
    Gauge.builder("rendition.active.jobs", activeJobCount).register(meterRegistry);
  }

  public void recordPipelineSuccess(Instant startedAt) {
    this.pipelineSuccessTimer.record(calculateElapsedDuration(startedAt));
  }

  public void recordPipelineFailure(Instant startedAt) {
    this.pipelineErrorTimer.record(calculateElapsedDuration(startedAt));
  }

  private void buildGuageMetrics() {
    Gauge.builder(
            "rendition.queue.size",
            this.renditionStoragePort,
            port -> port.countByStatus(RenditionStatus.PENDING))
        .register(meterRegistry);
  }

  private void buildTimerMetrics() {
    this.pipelineSuccessTimer =
        Timer.builder("rendition.pipeline.duration")
            .tag("status", "success")
            .publishPercentileHistogram()
            .register(meterRegistry);
    this.pipelineErrorTimer =
        Timer.builder("rendition.pipeline.duration")
            .tag("status", "error")
            .publishPercentileHistogram()
            .register(meterRegistry);
  }

  private Duration calculateElapsedDuration(Instant start) {
    return Duration.between(start, Instant.now());
  }
}
