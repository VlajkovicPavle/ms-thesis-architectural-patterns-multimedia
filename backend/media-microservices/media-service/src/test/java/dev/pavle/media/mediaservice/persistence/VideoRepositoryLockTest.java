package dev.pavle.media.mediaservice.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

class VideoRepositoryLockTest {
  @Test
  void renditionCreationLookupUsesAPessimisticWriteLock() throws NoSuchMethodException {
    var method = VideoRepository.class.getMethod("findByIdForUpdate", UUID.class);

    assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    assertThat(method.getAnnotation(Query.class).value())
        .isEqualTo("select video from Video video where video.id = :videoId");
  }
}
