package dev.pavle.mediamonolith.video.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamonolith.video.domain.model.video.Video;

public interface VideoRepository extends JpaRepository<Video, UUID> {}
