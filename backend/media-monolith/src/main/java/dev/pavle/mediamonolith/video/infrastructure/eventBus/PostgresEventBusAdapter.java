package dev.pavle.mediamonolith.video.infrastructure.eventBus;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.pavle.mediamonolith.video.domain.event.CreateRenditionEvent;
import dev.pavle.mediamonolith.video.domain.port.RenditionJobEventBusPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class PostgresEventBusAdapter implements RenditionJobEventBusPort {
  private static final String CHANNEL_NAME = "rendition_jobs";

  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper;
  private final DataSource dataSource;
  private PGConnection listenerConnection;

  public PostgresEventBusAdapter(ObjectMapper objectMapper, DataSource dataSource) {
    this.objectMapper = objectMapper;
    this.dataSource = dataSource;
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

  @Scheduled(fixedDelay = 1000)
  private void consumeCreateRenditionJob() {
    try {
      if (listenerConnection == null) {
        initListenerConnection();
      }
      PGNotification[] notifications = listenerConnection.getNotifications(0);
      if (notifications == null) {
        return;
      }
      for (PGNotification notification : notifications) {
        CreateRenditionEvent event =
            objectMapper.readValue(notification.getParameter(), CreateRenditionEvent.class);
        log.info("Received rendition job: {}", event);
      }
    } catch (SQLException e) {
      log.error("Listener connection lost, will reconnect on next poll", e);
      listenerConnection = null;
    } catch (JacksonException e) {
      log.error("Failed to deserialize rendition event", e);
    }
  }

  private void initListenerConnection() throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(true);
    listenerConnection = connection.unwrap(PGConnection.class);
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("LISTEN " + CHANNEL_NAME);
    }
    log.info("Listening on channel '{}'", CHANNEL_NAME);
  }
}
