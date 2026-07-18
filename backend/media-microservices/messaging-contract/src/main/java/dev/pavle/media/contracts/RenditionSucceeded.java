package dev.pavle.media.contracts;

import java.time.Instant;
import java.util.UUID;

public record RenditionSucceeded(
    UUID renditionId, UUID videoId, String storedFileIdentifier, Instant occurredAt)
    implements RenditionEvent {}
