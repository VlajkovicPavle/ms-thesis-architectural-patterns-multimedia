package dev.pavle.mediamodular.video.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamodular.video.domain.model.video.Video;

public interface VideoRepository extends JpaRepository<Video, UUID> {
  List<Video> findAllByOrderByCreatedAtDesc();
}
