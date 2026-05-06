package dev.pavle.mediamonolith.video.infrastructure.eventBus;

import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.port.RenditionJobProducerPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PostgresEventBusAdapter implements RenditionJobProducerPort {
  private static final String CHANNEL_NAME = "rendition_jobs";

  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper;

  public PostgresEventBusAdapter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void publishCreateRenditionJob(CreateRenditionEvent event) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (JacksonException e) {
      throw new EventBusException("Failed to serialize CreateRenditionEvent", e);
    }
    entityManager
        .createNativeQuery("SELECT pg_notify(:channel, :payload)")
        .setParameter("channel", CHANNEL_NAME)
        .setParameter("payload", payload)
        .getSingleResult();
  }
}
