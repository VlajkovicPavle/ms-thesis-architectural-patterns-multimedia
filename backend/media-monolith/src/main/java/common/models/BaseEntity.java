package common.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private Instant createdAt;
  private Instant updatedAt;

  @PrePersist
  private void onCreate() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  private void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
