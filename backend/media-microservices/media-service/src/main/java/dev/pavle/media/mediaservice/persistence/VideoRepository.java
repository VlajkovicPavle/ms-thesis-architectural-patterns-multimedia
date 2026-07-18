package dev.pavle.media.mediaservice.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.pavle.media.mediaservice.model.Video;
import jakarta.persistence.LockModeType;

public interface VideoRepository extends JpaRepository<Video, UUID> {
  List<Video> findAllByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select video from Video video where video.id = :videoId")
  Optional<Video> findByIdForUpdate(@Param("videoId") UUID videoId);
}
