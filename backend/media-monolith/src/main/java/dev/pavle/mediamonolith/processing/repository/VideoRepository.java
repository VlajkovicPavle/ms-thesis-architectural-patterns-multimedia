package dev.pavle.mediamonolith.processing.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.pavle.mediamonolith.processing.model.Video;

public interface VideoRepository extends JpaRepository<Video, UUID> {}
