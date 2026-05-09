package dev.pavle.mediamonolith.video.infrastructure.web.dto;

import java.util.Set;
import java.util.UUID;

import dev.pavle.mediamonolith.video.domain.model.video.VideoResolution;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateRenditionsRequest(
    @NotNull UUID videoId, @NotEmpty Set<VideoResolution> resolutions) {}
