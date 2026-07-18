package dev.pavle.media.contracts;

import java.time.Instant;
import java.util.UUID;

public record RenditionRunning(UUID renditionId, UUID videoId, Instant occurredAt)
    implements RenditionEvent {}
