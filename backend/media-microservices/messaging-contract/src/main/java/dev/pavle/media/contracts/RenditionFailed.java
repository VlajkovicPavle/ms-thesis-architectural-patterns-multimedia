package dev.pavle.media.contracts;

import java.time.Instant;
import java.util.UUID;

public record RenditionFailed(UUID renditionId, UUID videoId, String error, Instant occurredAt)
    implements RenditionEvent {}
