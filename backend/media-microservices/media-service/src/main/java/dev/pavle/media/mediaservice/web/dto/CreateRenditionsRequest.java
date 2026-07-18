package dev.pavle.media.mediaservice.web.dto;

import java.util.Set;
import java.util.UUID;

import dev.pavle.media.contracts.VideoResolution;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateRenditionsRequest(
    @NotNull UUID videoId, @NotEmpty Set<VideoResolution> resolutions) {}
