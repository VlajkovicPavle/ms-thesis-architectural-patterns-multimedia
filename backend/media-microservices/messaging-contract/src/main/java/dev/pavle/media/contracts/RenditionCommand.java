package dev.pavle.media.contracts;

import java.time.Instant;
import java.util.UUID;

public record RenditionCommand(
    UUID renditionId,
    UUID videoId,
    VideoResolution resolution,
    String sourceIdentifier,
    String outputFileName,
    Instant createdAt) {}
