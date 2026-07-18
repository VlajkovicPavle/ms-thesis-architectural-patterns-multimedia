package dev.pavle.media.mediaservice.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class BaseEntity {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;

  protected BaseEntity() {}

  protected BaseEntity(UUID id) {
    this.id = id;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
