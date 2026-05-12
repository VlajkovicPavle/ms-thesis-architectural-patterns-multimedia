package dev.pavle.mediamonolith.video.application;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.exception.RenditionJobAlreadyActiveException;

public class RenditionJobHandler {

  @Value("${rendition.pool-size}")
  private int poolSize;

  private ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
  private Set<CreateRenditionEvent> activeRenditions = ConcurrentHashMap.newKeySet();

  public void dispatchCreateRenditionJob(CreateRenditionEvent createRenditionEvent) {
    if (activeRenditions.contains(createRenditionEvent)) {
      throw new RenditionJobAlreadyActiveException(
          createRenditionEvent.videoId(), createRenditionEvent.resolution());
    }
    activeRenditions.add(createRenditionEvent);
    // TODO:
    // Write actual job dispatch
  }
}
