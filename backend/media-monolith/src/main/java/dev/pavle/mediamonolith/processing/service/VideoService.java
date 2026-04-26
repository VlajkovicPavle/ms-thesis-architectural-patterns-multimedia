package dev.pavle.mediamonolith.processing.service;

import org.springframework.stereotype.Service;

import dev.pavle.mediamonolith.processing.repository.VideoRepository;

@Service
public class VideoService {

  private final VideoRepository repository;

  public VideoService(VideoRepository repository) {
    this.repository = repository;
  }
}
